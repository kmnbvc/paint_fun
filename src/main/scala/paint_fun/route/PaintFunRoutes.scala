package paint_fun.route

import cats.effect.{Blocker, ContextShift, Sync}
import fs2.{Pipe, Stream}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Location
import org.http4s.implicits._
import org.http4s.server.staticcontent.{ResourceService, resourceService}
import org.http4s.server.websocket.WebSocketBuilder
import org.http4s.twirl._
import org.http4s.websocket.WebSocketFrame
import paint_fun.model.BoardStroke
import paint_fun.persistence.WhiteboardRepo

import java.util.UUID.randomUUID
import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object PaintFunRoutes {

  def whiteboardRoutes[F[_] : Sync](repo: WhiteboardRepo[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root => SeeOther(`Location`(uri"/" / randomUUID().toString))

      case GET -> Root / UUIDVar(id) => Ok(html.whiteboard(id.toString))

      case GET -> Root / "ws" / UUIDVar(id) =>
        val send: Stream[F, WebSocketFrame] = repo.strokes(id.toString).map(s => WebSocketFrame.Text(s.data.toString))
        val receive: Pipe[F, WebSocketFrame, Unit] = stream => {
          val in = stream.map {
            case WebSocketFrame.Text(str, _) => BoardStroke.fromJson(str)
          }
          repo.save(in).showLinesStdOut
        }

        WebSocketBuilder[F].build(send, receive)
    }
  }

  def staticRoutes[F[_] : Sync : ContextShift]: HttpRoutes[F] = {
    val threadPool = Executors.newCachedThreadPool()
    val ec = ExecutionContext.fromExecutorService(threadPool)
    val blocker = Blocker.liftExecutionContext(ec)
    resourceService[F](ResourceService.Config("/assets", blocker))
  }
}
