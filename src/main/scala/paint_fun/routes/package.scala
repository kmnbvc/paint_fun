package paint_fun

import cats.effect.{Concurrent, ContextShift}
import cats.implicits.toSemigroupKOps
import org.http4s.HttpRoutes
import paint_fun.persistence._
import paint_fun.routes.StaticRoutes.assets
import paint_fun.routes.UserRoutes.userRoutes
import paint_fun.routes.WhiteboardRoutes.whiteboardRoutes
import paint_fun.routes.SnapshotRoutes.snapshotRoutes

package object routes {
  def routes[F[_] : Concurrent : ContextShift](
                                                boards: WhiteboardStorage[F],
                                                users: UserStorage[F],
                                                snapshots: SnapshotStorage[F],
                                                authenticator: Authenticator[F]
                                              ): HttpRoutes[F] = {
    assets <+>
      whiteboardRoutes(boards) <+>
      userRoutes(users, authenticator) <+>
      snapshotRoutes(snapshots, authenticator)
  }
}
