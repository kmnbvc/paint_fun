package paint_fun.persistence

import cats.effect.{Concurrent, ContextShift}
import doobie.implicits.toSqlInterpolator
import paint_fun.model.Snapshot

trait SnapshotStorage[F[_]] {
  def get(boardId: String, name: String): F[Snapshot]
  def find(boardId: String): F[List[Snapshot]]
  def save(snapshot: Snapshot): F[Int]
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
    sql"select * from paint_fun.snapshots where name = $name and whiteboard_id = $boardId".query[Snapshot].unique
  }

  def find(boardId: String): F[List[Snapshot]] = transact {
    sql"select * from paint_fun.snapshots where whiteboard_id = $boardId".query[Snapshot].to[List]
  }

  def save(snapshot: Snapshot): F[Int] = transact {
    val (boardId, name, data) = (snapshot.whiteboardId, snapshot.name, snapshot.data)
    sql"insert into paint_fun.snapshots (whiteboard_id, name, data) values ($boardId, $name, $data)".update.run
  }

  def linkToRestoreFrom(boardId: String, snapshot: Snapshot): F[Int] = transact {
    val query = sql"insert into paint_fun.snapshots_restore_links " ++
      sql"(whiteboard_id, snapshot_name, snapshot_from) " ++
      sql"values ($boardId, ${snapshot.name}, ${snapshot.whiteboardId})"
    query.update.run
  }

  def findSnapshotToRestore(boardId: String): F[Option[Snapshot]] = transact {
    val query = sql"select s.* from paint_fun.snapshots s " ++
      sql"inner join paint_fun.snapshots_restore_links l " ++
      sql"on (s.name = l.snapshot_name and s.whiteboard_id = l.snapshot_from) " ++
      sql"where l.whiteboard_id = $boardId"
    query.query[Snapshot].option
  }
}
