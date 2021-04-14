package paint_fun.persistence

import cats.effect.{Concurrent, ContextShift}
import doobie.implicits.toSqlInterpolator
import paint_fun.model.Snapshot

trait SnapshotStorage[F[_]] {
  def get(boardId: String, name: String): F[Snapshot]
  def find(boardId: String): F[List[Snapshot]]
  def save(snapshot: Snapshot): F[Int]
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
}
