package paint_fun.persistence

import cats.data.Validated._
import cats.effect.{Concurrent, ContextShift}
import cats.implicits._
import doobie.implicits.toSqlInterpolator
import paint_fun.model.SnapshotValidation.validate
import paint_fun.model.ValidationError.AllErrorsOr
import paint_fun.model.{Snapshot, SnapshotValidation}

trait SnapshotStorage[F[_]] {
  def get(boardId: String, name: String): F[Snapshot]
  def find(boardId: String): F[List[Snapshot]]
  def save(snapshot: Snapshot): F[AllErrorsOr[Snapshot]]
  def linkToRestoreFrom(boardId: String, snapshot: Snapshot): F[Int]
  def findSnapshotToRestore(boardId: String): F[Option[Snapshot]]
}

object SnapshotStorage {
  implicit def apply[F[_] : SnapshotStorage]: SnapshotStorage[F] = implicitly

  def instance[F[_] : Concurrent : ContextShift]: SnapshotStorage[F] = new SnapshotStorageImpl[F]
}

class SnapshotStorageImpl[F[_]](implicit
                                concurrent: Concurrent[F],
                                contextShift: ContextShift[F]
                               ) extends DbConnection[F] with SnapshotStorage[F] {

  def get(boardId: String, name: String): F[Snapshot] = transact {
    sql"select * from paint_fun.snapshots where name = $name and source_board_id = $boardId".query[Snapshot].unique
  }

  def find(boardId: String): F[List[Snapshot]] = transact {
    sql"select * from paint_fun.snapshots where source_board_id = $boardId".query[Snapshot].to[List]
  }

  def save(snapshot: Snapshot): F[AllErrorsOr[Snapshot]] = validate(snapshot) match {
    case Valid(_) => insert(snapshot)
    case Invalid(e) => e.invalid[Snapshot].pure[F]
  }

  private def insert(snapshot: Snapshot): F[AllErrorsOr[Snapshot]] = {
    val (boardId, name, data) = (snapshot.sourceBoardId, snapshot.name, snapshot.data)
    val query = sql"insert into paint_fun.snapshots (source_board_id, name, data) " ++
      sql"values ($boardId, $name, $data) on conflict do nothing"
    val res = query.update.run.map {
      case 0 => SnapshotValidation.nameAlreadyExists
      case _ => snapshot.validNel
    }
    transact(res)
  }

  def linkToRestoreFrom(boardId: String, snapshot: Snapshot): F[Int] = transact {
    val query = sql"insert into paint_fun.snapshots_restore_links " ++
      sql"(whiteboard_id, snapshot_name, snapshot_from) " ++
      sql"values ($boardId, ${snapshot.name}, ${snapshot.sourceBoardId})"
    query.update.run
  }

  def findSnapshotToRestore(boardId: String): F[Option[Snapshot]] = transact {
    val query = sql"select s.* from paint_fun.snapshots s " ++
      sql"inner join paint_fun.snapshots_restore_links l " ++
      sql"on (s.name = l.snapshot_name and s.source_board_id = l.snapshot_from) " ++
      sql"where l.whiteboard_id = $boardId"
    query.query[Snapshot].option
  }
}
