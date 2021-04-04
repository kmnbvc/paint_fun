package paint_fun.server

import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import fs2.Stream
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import paint_fun.persistence.{UserStorage, WhiteboardStorage}
import paint_fun.routes.{Authenticator, routes}

import scala.concurrent.ExecutionContext.global

object PaintFunServer {
  def stream[F[_] : ConcurrentEffect : Timer : ContextShift]: Stream[F, Nothing] = {
    val boards = WhiteboardStorage.instance[F]
    val users = UserStorage.instance[F]
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
