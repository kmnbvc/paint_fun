package paint_fun.server

import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import fs2.Stream
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import paint_fun.persistence.WhiteboardRepo
import paint_fun.route.routes

import scala.concurrent.ExecutionContext.global

object PaintFunServer {
  def stream[F[_] : ConcurrentEffect : Timer : ContextShift]: Stream[F, Nothing] = {
    val repo = WhiteboardRepo.instance[F]
    val app = routes(repo).orNotFound
    val appFinal = Logger.httpApp(logHeaders = true, logBody = false)(app)

    BlazeServerBuilder[F](global)
      .bindHttp(9000, "0.0.0.0")
      .withHttpApp(appFinal)
      .serve
      .drain
  }
}
