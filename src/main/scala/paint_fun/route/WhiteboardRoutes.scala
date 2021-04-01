package paint_fun.route

import cats.effect.Sync
import fs2.{Pipe, Stream}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Location
import org.http4s.implicits._
import org.http4s.server.websocket.WebSocketBuilder
import org.http4s.twirl._
import org.http4s.websocket.WebSocketFrame
import org.slf4j.LoggerFactory
import paint_fun.model.BoardStroke
import paint_fun.persistence.WhiteboardRepo

import java.util.UUID.randomUUID

object WhiteboardRoutes {

  private val logger = LoggerFactory.getLogger(getClass)

  def whiteboardRoutes[F[_] : Sync](repo: WhiteboardRepo[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root => SeeOther(`Location`(uri"/b" / randomUUID().toString))

      case GET -> Root / "b" / id => Ok(html.whiteboard(id))

      case GET -> Root / "ws" / id =>
        val send: Stream[F, WebSocketFrame] = repo.strokes(id)
          .map(_.toJson)
          .map(WebSocketFrame.Text(_))
        val receive: Pipe[F, WebSocketFrame, Unit] = stream => {
          val in = for {
            frame <- stream if frame.isInstanceOf[WebSocketFrame.Text]
            json = frame.asInstanceOf[WebSocketFrame.Text].str
            stroke <- BoardStroke.fromJson(json) match {
              case Left(ex) => logger.error(ex.getMessage, ex); Stream.empty
              case Right(value) => Stream(value)
            }
          } yield stroke

          repo.save(in).drain
        }

        WebSocketBuilder[F].build(send, receive)
    }
  }
}
