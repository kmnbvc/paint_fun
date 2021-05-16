package paint_fun.persistence

import cats.data.OptionT
import cats.effect._
import cats.implicits._
import doobie.hikari.HikariTransactor
import doobie.implicits._
import doobie.postgres.implicits
import doobie.util.meta.Meta
import paint_fun.model.User

import java.util.UUID

trait WhiteboardAccessStorage[F[_]] {
  def create(user: User): F[UUID]
  def permitted(boardId: UUID, user: Option[User]): F[Boolean]
}

object WhiteboardAccessStorage {
  def apply[F[_] : WhiteboardAccessStorage]: WhiteboardAccessStorage[F] = implicitly

  def instance[F[_] : HikariTransactor : BracketThrow]: WhiteboardAccessStorage[F] = new WhiteboardAccessStorageImpl[F]
}

private class WhiteboardAccessStorageImpl[F[_] : BracketThrow](implicit
                                                               xa: HikariTransactor[F]
                                                              ) extends WhiteboardAccessStorage[F] {
  implicit val uuidMeta: Meta[UUID] = implicits.UuidType

  def create(user: User): F[UUID] = {
    val uuid = UUID.randomUUID()
    val sql = sql"insert into paint_fun.whiteboards_access " ++
      sql"(whiteboard_id, whiteboard_owner, access_type) " ++
      sql"values ($uuid, ${user.login}, 'owner_only')"
    sql.update.run.transact(xa).as(uuid)
  }

  def permitted(boardId: UUID, user: Option[User]): F[Boolean] = {
    val sql = sql"select whiteboard_owner, access_type from paint_fun.whiteboards_access where whiteboard_id = $boardId"
    val data = sql.query[(String, String)].option.transact(xa)

    OptionT(data).fold(true) {
      case (owner, accessType) =>
        accessType == "anyone" ||
          (accessType == "owner_only" && user.exists(_.login == owner))
    }
  }
}
