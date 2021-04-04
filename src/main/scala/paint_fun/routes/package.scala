package paint_fun

import cats.effect.{Concurrent, ContextShift}
import cats.implicits.toSemigroupKOps
import org.http4s.HttpRoutes
import paint_fun.persistence.{UserStorage, WhiteboardStorage}
import paint_fun.routes.StaticRoutes.assets
import paint_fun.routes.UserRoutes.{authedRoutes, userRoutes}
import paint_fun.routes.WhiteboardRoutes.whiteboardRoutes

package object routes {
  def routes[F[_] : Concurrent : ContextShift](
                                                boards: WhiteboardStorage[F],
                                                users: UserStorage[F],
                                                authenticator: Authenticator[F]
                                              ): HttpRoutes[F] = {
    assets <+> whiteboardRoutes(boards) <+> userRoutes(users) <+> authedRoutes(authenticator)
  }
}
