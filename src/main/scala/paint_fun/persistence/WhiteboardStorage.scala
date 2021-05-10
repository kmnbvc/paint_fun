package paint_fun.persistence

import cats.effect.{Concurrent, ContextShift}
import dev.profunktor.redis4cats.connection.RedisClient
import dev.profunktor.redis4cats.data.RedisCodec
import dev.profunktor.redis4cats.log4cats._
import dev.profunktor.redis4cats.streams.RedisStream
import dev.profunktor.redis4cats.streams.data.{XAddMessage, XReadMessage}
import fs2._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import paint_fun.config
import paint_fun.model.{BoardStroke, BoardStrokeData}

import java.util.UUID

trait WhiteboardStorage[F[_]] {
  def strokes(boardId: UUID): Stream[F, BoardStroke]
  def save(in: Stream[F, BoardStroke]): Stream[F, String]
}

object WhiteboardStorage {
  implicit def apply[F[_] : WhiteboardStorage]: WhiteboardStorage[F] = implicitly

  def instance[F[_] : Concurrent : ContextShift]: WhiteboardStorage[F] = new WhiteboardStorageImpl[F]
}

class WhiteboardStorageImpl[F[_]](implicit
                                  concurrent: Concurrent[F],
                                  cs: ContextShift[F]
                                 ) extends WhiteboardStorage[F] {

  implicit val logger: Logger[F] = Slf4jLogger.getLogger[F]

  private val codec = RedisCodec.Utf8
  private val cfg = config.redisConfig

  private lazy val client = RedisClient[F].from(cfg.url)
  private lazy val streaming = client.flatMap(RedisStream.mkStreamingConnectionResource(_, codec))

  override def strokes(boardId: UUID): Stream[F, BoardStroke] = {
    for {
      streaming <- Stream.resource(streaming)
      msg <- streaming.read(Set(cfg.streamKey)) if msg.body.contains(boardId.toString)
      stroke <- fromReadMessage(msg)
    } yield stroke
  }

  override def save(in: Stream[F, BoardStroke]): Stream[F, String] = {
    for {
      streaming <- Stream.resource(streaming)
      pipe = streaming.append
      msgs = in.map(addMessage)
      res <- msgs.through(pipe)
    } yield res.value
  }

  private def addMessage(stroke: BoardStroke): XAddMessage[String, String] =
    XAddMessage(cfg.streamKey, Map(stroke.whiteboardId.toString -> stroke.data.toJson))

  private def fromReadMessage(msg: XReadMessage[String, String]): Stream[F, BoardStroke] =
    for {
      (id, json) <- Stream.iterable(msg.body)
      data <- BoardStrokeData.fromJson(json) match {
        case Left(ex) => logger.error(ex)(ex.getMessage); Stream.empty
        case Right(value) => Stream.emit(value)
      }
    } yield BoardStroke(UUID.fromString(id), data)
}
