package paint_fun.server

import cats.effect._
import fs2.Stream
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import paint_fun.persistence._
import paint_fun.routes.{Authenticator, routes}

import scala.concurrent.ExecutionContext.global

object PaintFunServer {
  def run[F[_] : ConcurrentEffect : Timer : ContextShift]: Resource[F, Stream[F, ExitCode]] = {
    DbConnection.transactor[F].map { xa =>
      val boards = WhiteboardStorage[F]
      val users = UserStorage[F](xa)
      val snapshots = SnapshotStorage[F](xa)
      val access = WhiteboardAccessStorage[F](xa)
      val authenticator = new Authenticator[F](users)
      val app = routes(boards, users, snapshots, authenticator, access).orNotFound
      val appFinal = Logger.httpApp(logHeaders = true, logBody = true)(app)

      BlazeServerBuilder[F](global)
        .bindHttp(9000, "0.0.0.0")
        .withHttpApp(appFinal)
        .serve
    }
  }
}
