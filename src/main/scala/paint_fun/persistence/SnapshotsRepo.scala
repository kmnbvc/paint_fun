package paint_fun.persistence

import cats.effect.{Concurrent, ContextShift}
import paint_fun.model.BoardStroke

trait SnapshotsRepo[F[_]] {
  def snapshots(user: String): F[List[String]]
  def saveSnapshot(user: String, data: List[BoardStroke]): F[Unit]
}


object SnapshotsRepo {
  implicit def apply[F[_] : SnapshotsRepo]: SnapshotsRepo[F] = implicitly

  def instance[F[_] : Concurrent : ContextShift]: SnapshotsRepo[F] = new SnapshotsRepoImpl[F]
}


class SnapshotsRepoImpl[F[_]](implicit
                              concurrent: Concurrent[F],
                              contextShift: ContextShift[F]
                             ) extends SnapshotsRepo[F] {

  override def snapshots(user: String): F[List[String]] = ???
  override def saveSnapshot(user: String, data: List[BoardStroke]): F[Unit] = ???

}
