package paint_fun.persistence

import cats.Applicative
import paint_fun.model.BoardStroke
import fs2._

trait WhiteboardRepo[F[_]] {
  def strokes(boardId: String): Stream[F, BoardStroke]
  def save(stroke: BoardStroke): F[Int]
}

object WhiteboardRepo {
  implicit def apply[F[_] : WhiteboardRepo]: WhiteboardRepo[F] = implicitly

  def instance[F[_] : Applicative]: WhiteboardRepo[F] = new WhiteboardRepoImpl[F]
}

class WhiteboardRepoImpl[F[_]] extends WhiteboardRepo[F] {
  override def strokes(boardId: String): Stream[F, BoardStroke] = ???
  override def save(stroke: BoardStroke): F[Int] = ???
}
