package paint_fun

import cats.effect.{Concurrent, ContextShift}
import cats.implicits.toSemigroupKOps
import org.http4s.HttpRoutes
import paint_fun.persistence._
import paint_fun.routes.SnapshotRoutes.snapshotRoutes
import paint_fun.routes.StaticRoutes.assets
import paint_fun.routes.UserRoutes._
import paint_fun.routes.WhiteboardAccessRoutes.whiteboardAccessRoutes
import paint_fun.routes.WhiteboardRoutes.whiteboardRoutes

package object routes {
  def routes[F[_] : Concurrent : ContextShift](
                                                boards: WhiteboardStorage[F],
                                                users: UserStorage[F],
                                                snapshots: SnapshotStorage[F],
                                                authenticator: Authenticator[F],
                                                access: WhiteboardAccessStorage[F]
                                              ): HttpRoutes[F] = {
    assets <+>
      whiteboardRoutes(boards, snapshots) <+>
      userRoutes(users, authenticator) <+>
      authed(snapshots, authenticator, access)
  }

  def authed[F[_] : Concurrent : ContextShift](
                                                snapshots: SnapshotStorage[F],
                                                authenticator: Authenticator[F],
                                                access: WhiteboardAccessStorage[F]
                                              ): HttpRoutes[F] = {
    val svc = snapshotRoutes(snapshots) <+> userAuthedRoutes() <+> whiteboardAccessRoutes(access)
    authenticator.lift(svc)
  }
}
