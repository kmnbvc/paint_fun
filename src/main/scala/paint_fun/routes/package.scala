package paint_fun

import cats.Applicative
import cats.effect.{Concurrent, ContextShift}
import cats.implicits.toSemigroupKOps
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Location
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.server.Router
import paint_fun.persistence._
import paint_fun.routes.SnapshotRoutes.snapshotRoutes
import paint_fun.routes.StaticRoutes.assets
import paint_fun.routes.UserRoutes._
import paint_fun.routes.WhiteboardAccessRoutes.whiteboardAccessRoutes
import paint_fun.routes.WhiteboardRoutes.whiteboardRoutes

import java.util.UUID.randomUUID

package object routes {
  def routes[F[_] : Concurrent : ContextShift](
                                                boards: WhiteboardStorage[F],
                                                users: UserStorage[F],
                                                snapshots: SnapshotStorage[F],
                                                authenticator: Authenticator[F],
                                                access: WhiteboardAccessStorage[F]
                                              ): HttpRoutes[F] = {
    Router.define(
      "/board" -> authenticator.liftUserAware(whiteboardRoutes(boards, snapshots, access)),
      "/public" -> (assets <+> userRoutes(users, authenticator)),
      "/private" -> authed(snapshots, authenticator, access)
    )(default)
  }

  def authed[F[_] : Concurrent : ContextShift](
                                                snapshots: SnapshotStorage[F],
                                                authenticator: Authenticator[F],
                                                access: WhiteboardAccessStorage[F]
                                              ): HttpRoutes[F] = {
    val svc = snapshotRoutes(snapshots) <+> userAuthedRoutes() <+> whiteboardAccessRoutes(access)
    authenticator.lift(svc)
  }

  def default[F[_] : Applicative]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._

    HttpRoutes.strict {
      case GET -> Root => SeeOther(`Location`(uri"/board/b" / randomUUID().toString))
    }
  }
}
