package paint_fun

import cats.effect.{Concurrent, ContextShift}
import cats.implicits.toSemigroupKOps
import org.http4s.HttpRoutes
import paint_fun.persistence.{AuthTokensRepo, UserRepo, WhiteboardRepo}
import paint_fun.route.StaticRoutes.assets
import paint_fun.route.UserRoutes.{authedRoutes, userRoutes}
import paint_fun.route.WhiteboardRoutes.whiteboardRoutes

package object route {
  def routes[F[_] : Concurrent : ContextShift](
                                                boardRepo: WhiteboardRepo[F],
                                                userRepo: UserRepo[F],
                                                authenticator: MyAuthenticator[F]
                                              ): HttpRoutes[F] = {
    assets <+> whiteboardRoutes(boardRepo) <+> userRoutes(userRepo) <+> authedRoutes(authenticator)
  }
}
