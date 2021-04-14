package paint_fun

import cats.effect.{Concurrent, ContextShift}
import cats.implicits.toSemigroupKOps
import org.http4s.HttpRoutes
import paint_fun.persistence._
import paint_fun.routes.SnapshotRoutes.snapshotRoutes
import paint_fun.routes.StaticRoutes.assets
import paint_fun.routes.UserRoutes._
import paint_fun.routes.WhiteboardRoutes.whiteboardRoutes

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
      authed(snapshots, authenticator)
  }

  def authed[F[_] : Concurrent : ContextShift](
                                                snapshots: SnapshotStorage[F],
                                                authenticator: Authenticator[F]
                                              ): HttpRoutes[F] = {
    val authedServices = snapshotRoutes(snapshots) <+> userAuthedRoutes()
    authenticator.lift(authedServices)
  }
}
