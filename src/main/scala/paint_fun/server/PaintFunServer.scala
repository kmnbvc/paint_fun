package paint_fun.server

import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import fs2.Stream
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import paint_fun.persistence.{UserRepo, WhiteboardRepo}
import paint_fun.route.{Authenticator, routes}

import scala.concurrent.ExecutionContext.global

object PaintFunServer {
  def stream[F[_] : ConcurrentEffect : Timer : ContextShift]: Stream[F, Nothing] = {
    val boards = WhiteboardRepo.instance[F]
    val users = UserRepo.instance[F]
    val authenticator = new Authenticator[F]
    val app = routes(boards, users, authenticator).orNotFound
    val appFinal = Logger.httpApp(logHeaders = true, logBody = true)(app)

    BlazeServerBuilder[F](global)
      .bindHttp(9000, "0.0.0.0")
      .withHttpApp(appFinal)
      .serve
      .drain
  }
}
