package paint_fun.persistence

import cats.effect.{Concurrent, ContextShift}
import paint_fun.model.BoardStroke

trait SnapshotStorage[F[_]] {
  def snapshots(user: String): F[List[String]]
  def save(user: String, data: List[BoardStroke]): F[Unit]
}

object SnapshotStorage {
  implicit def apply[F[_] : SnapshotStorage]: SnapshotStorage[F] = implicitly

  def instance[F[_] : Concurrent : ContextShift]: SnapshotStorage[F] = new SnapshotStorageImpl[F]
}


class SnapshotStorageImpl[F[_]](implicit
                                concurrent: Concurrent[F],
                                contextShift: ContextShift[F]
                               ) extends SnapshotStorage[F] {

  override def snapshots(user: String): F[List[String]] = ???
  override def save(user: String, data: List[BoardStroke]): F[Unit] = ???

}
