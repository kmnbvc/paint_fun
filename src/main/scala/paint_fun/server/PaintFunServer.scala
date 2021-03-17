package paint_fun.server

import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import fs2.Stream
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import paint_fun.persistence.WhiteboardRepo
import paint_fun.route.PaintFunRoutes
import cats.implicits._

import scala.concurrent.ExecutionContext.global

object PaintFunServer {
  def stream[F[_] : ConcurrentEffect : Timer : ContextShift]: Stream[F, Nothing] = {
    val repo = WhiteboardRepo.instance[F]
    val routes = (PaintFunRoutes.staticRoutes[F] <+> PaintFunRoutes.whiteboardRoutes[F](repo)).orNotFound
    val app = Logger.httpApp(logHeaders = true, logBody = false)(routes)

    BlazeServerBuilder[F](global)
      .bindHttp(9000, "0.0.0.0")
      .withHttpApp(app)
      .serve
      .drain
  }
}
