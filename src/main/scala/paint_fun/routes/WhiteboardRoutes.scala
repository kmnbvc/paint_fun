package paint_fun.routes

import cats.data.OptionT
import cats.effect.Sync
import cats.implicits._
import fs2.{Pipe, Stream}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.websocket.WebSocketBuilder
import org.http4s.twirl._
import org.http4s.websocket.WebSocketFrame
import org.slf4j.LoggerFactory
import paint_fun.model.BoardStroke
import paint_fun.persistence._
import paint_fun.routes.Authenticator.UserAwareSvc
import tsec.authentication.{UserAwareService, asAware}

object WhiteboardRoutes {

  private val logger = LoggerFactory.getLogger(getClass)

  def whiteboardRoutes[F[_] : Sync](
                                     repo: WhiteboardStorage[F],
                                     snapshots: SnapshotStorage[F],
                                     access: WhiteboardAccessStorage[F]
                                   ): UserAwareSvc[F] = {
    val dsl = Http4sDsl[F]
    import dsl._

    UserAwareService {
      case GET -> Root / "b" / UUIDVar(id) asAware user => for {
        allow <- access.permitted(id, user.map(_._1))
        resp <- OptionT.whenF(allow) {
          snapshots.findRestoredFrom(id).flatMap(s => Ok(html.whiteboard(id, s)))
        }.getOrElseF(Forbidden())
      } yield resp

      case GET -> Root / "ws" / UUIDVar(id) asAware user =>
        val send: Stream[F, WebSocketFrame] = repo.strokes(id)
          .map(_.toJson)
          .map(WebSocketFrame.Text(_))
        val receive: Pipe[F, WebSocketFrame, Unit] = in => {
          val strokes = in.collect {
            case WebSocketFrame.Text(json, _) => Stream.fromEither[F](BoardStroke.fromJson(json))
          }.flatten

          repo.save(strokes).drain
        }

        access.permitted(id, user.map(_._1)).flatMap {
          OptionT.whenF(_)(WebSocketBuilder[F].build(send, receive)).getOrElseF(Forbidden())
        }
    }
  }
}
