package paint_fun.routes

import cats.effect.Sync
import cats.implicits._
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
import paint_fun.persistence.{SnapshotStorage, WhiteboardStorage}

import java.util.UUID.randomUUID

object WhiteboardRoutes {

  private val logger = LoggerFactory.getLogger(getClass)

  def whiteboardRoutes[F[_] : Sync](repo: WhiteboardStorage[F], snapshots: SnapshotStorage[F]): HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root => SeeOther(`Location`(uri"/b" / randomUUID().toString))

      case GET -> Root / "b" / UUIDVar(id) => for {
        snapshot <- snapshots.findSnapshotToRestore(id)
        resp <- Ok(html.whiteboard(id, snapshot))
      } yield resp

      case GET -> Root / "ws" / UUIDVar(id) =>
        val send: Stream[F, WebSocketFrame] = repo.strokes(id)
          .map(_.toJson)
          .map(WebSocketFrame.Text(_))
        val receive: Pipe[F, WebSocketFrame, Unit] = in => {
          val strokes = in.collect {
            case WebSocketFrame.Text(json, _) => Stream.fromEither[F](BoardStroke.fromJson(json))
          }.flatten

          repo.save(strokes).drain
        }

        WebSocketBuilder[F].build(send, receive)
    }
  }
}
