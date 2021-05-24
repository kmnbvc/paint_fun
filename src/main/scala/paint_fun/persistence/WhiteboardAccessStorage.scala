package paint_fun.persistence

import cats.data.OptionT
import cats.effect._
import cats.implicits._
import doobie.Transactor
import doobie.implicits._
import doobie.postgres.implicits
import doobie.util.meta.Meta
import paint_fun.model.AccessType._
import paint_fun.model.{AccessType, User}

import java.util.UUID

trait WhiteboardAccessStorage[F[_]] {
  def create(user: User): F[UUID]
  def permitted(boardId: UUID, user: Option[User]): F[Boolean]
  def state(boardId: UUID): F[Map[AccessType, Boolean]]
  def set(boardId: UUID, accessType: AccessType, user: User): F[Unit]
}

object WhiteboardAccessStorage {
  implicit val uuidMeta: Meta[UUID] = implicits.UuidType

  def apply[F[_] : BracketThrow](xa: Transactor[F]): WhiteboardAccessStorage[F] = new WhiteboardAccessStorage[F] {
    def create(user: User): F[UUID] = {
      val uuid = UUID.randomUUID()
      set(uuid, OwnerOnly, user).as(uuid)
    }

    def permitted(boardId: UUID, user: Option[User]): F[Boolean] = {
      val sql = sql"select whiteboard_owner, access_type from paint_fun.whiteboards_access where whiteboard_id = $boardId"
      val data = sql.query[(String, AccessType)].option.transact(xa)

      OptionT(data).fold(true) {
        case (_, Anyone) => true
        case (owner, OwnerOnly) => user.exists(_.login == owner)
        case _ => false
      }
    }

    def state(boardId: UUID): F[Map[AccessType, Boolean]] = {
      val sql = sql"select access_type from paint_fun.whiteboards_access where whiteboard_id = $boardId"
      val stored = sql.query[AccessType].option.transact(xa)
      List(Anyone, OwnerOnly).traverse { t =>
        stored.map(t -> _.contains(t))
      }.map(_.toMap)
    }

    def set(boardId: UUID, accessType: AccessType, user: User): F[Unit] = {
      val insert = sql"insert into paint_fun.whiteboards_access " ++
        sql"(whiteboard_id, whiteboard_owner, access_type) " ++
        sql"values ($boardId, ${user.login}, $accessType) "

      val update = sql"update paint_fun.whiteboards_access set access_type = $accessType where whiteboard_id = $boardId"

      val select = sql"select 1 from paint_fun.whiteboards_access " ++
        sql"where whiteboard_id = $boardId and whiteboard_owner = ${user.login}"

      select.query[Int].option.flatMap(_.fold(insert)(_ => update).update.run).transact(xa).void
    }
  }
}
