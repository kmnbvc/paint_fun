package paint_fun.persistence

import cats.data.Validated._
import cats.effect.{Concurrent, ContextShift}
import cats.implicits._
import doobie.Transactor
import doobie.implicits._
import doobie.postgres.implicits
import doobie.util.meta.Meta
import paint_fun.model.SnapshotValidation.validate
import paint_fun.model.ValidationError.AllErrorsOr
import paint_fun.model._

import java.util.UUID

trait SnapshotStorage[F[_]] {
  def get(boardId: UUID, name: String): F[Snapshot]
  def find(boardId: UUID): F[List[Snapshot]]
  def save(snapshot: Snapshot): F[AllErrorsOr[Snapshot]]
  def restore(snapshot: Snapshot): F[UUID]
  def findRestoredFrom(boardId: UUID): F[Option[Snapshot]]
}

object SnapshotStorage {
  implicit val uuidMeta: Meta[UUID] = implicits.UuidType

  def apply[F[_] : Concurrent : ContextShift](xa: Transactor[F]): SnapshotStorage[F] = new SnapshotStorage[F] {
    def get(boardId: UUID, name: String): F[Snapshot] = {
      sql"select * from paint_fun.snapshots where name = $name and source_board_id = $boardId"
        .query[Snapshot].unique.transact(xa)
    }

    def find(boardId: UUID): F[List[Snapshot]] = {
      sql"select * from paint_fun.snapshots where source_board_id = $boardId"
        .query[Snapshot].to[List].transact(xa)
    }

    def save(snapshot: Snapshot): F[AllErrorsOr[Snapshot]] = validate(snapshot) match {
      case Valid(_) => insert(snapshot)
      case Invalid(e) => e.invalid[Snapshot].pure[F]
    }

    private def insert(snapshot: Snapshot): F[AllErrorsOr[Snapshot]] = {
      val (boardId, name, data) = (snapshot.sourceBoardId, snapshot.name, snapshot.data)
      val sql = sql"insert into paint_fun.snapshots (source_board_id, name, data) " ++
        sql"values ($boardId, $name, $data) on conflict do nothing"
      val res = sql.update.run.map {
        case 0 => SnapshotValidation.nameAlreadyExists
        case _ => snapshot.validNel
      }
      res.transact(xa)
    }

    def restore(snapshot: Snapshot): F[UUID] = {
      val boardId = UUID.randomUUID()
      val sql = sql"insert into paint_fun.whiteboards " ++
        sql"(whiteboard_id, snapshot_name, snapshot_source_id) " ++
        sql"values ($boardId, ${snapshot.name}, ${snapshot.sourceBoardId})"
      sql.update.run.transact(xa).as(boardId)
    }

    def findRestoredFrom(boardId: UUID): F[Option[Snapshot]] = {
      val sql = sql"select s.* from paint_fun.snapshots s " ++
        sql"inner join paint_fun.whiteboards l " ++
        sql"on (s.name = l.snapshot_name and s.source_board_id = l.snapshot_source_id) " ++
        sql"where l.whiteboard_id = $boardId"
      sql.query[Snapshot].option.transact(xa)
    }
  }
}
