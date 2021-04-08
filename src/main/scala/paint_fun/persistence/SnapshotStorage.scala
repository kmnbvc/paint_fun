package paint_fun.persistence

import cats.effect.{Concurrent, ContextShift}
import doobie.implicits.toSqlInterpolator
import paint_fun.model.{Snapshot, User}

trait SnapshotStorage[F[_]] {
  def get(name: String): F[Snapshot]
  def find(user: User): F[List[Snapshot]]
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

  def get(name: String): F[Snapshot] = transact {
    sql"select * from paint_fun.snapshots where name = $name".query[Snapshot].unique
  }

  def find(user: User): F[List[Snapshot]] = transact {
    sql"select * from paint_fun.snapshots where user = ${user.login}".query[Snapshot].to[List]
  }

  def save(snapshot: Snapshot): F[Int] = transact {
    val (name, user, data) = (snapshot.name, snapshot.user, snapshot.data)
    sql"insert into paint_fun.snapshots (name, user, data) values ($name, $user, $data)".update.run
  }
}
