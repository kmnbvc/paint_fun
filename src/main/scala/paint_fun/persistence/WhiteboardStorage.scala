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
  def apply[F[_] : Concurrent : ContextShift]: WhiteboardStorage[F] = new WhiteboardStorage[F] {
    implicit val logger: Logger[F] = Slf4jLogger.getLogger[F]

    private val codec = RedisCodec.Utf8
    private val cfg = config.redisConfig

    private val client = RedisClient[F].from(cfg.url)
    private val streaming = client.flatMap(RedisStream.mkStreamingConnectionResource(_, codec))

    override def strokes(boardId: UUID): Stream[F, BoardStroke] = for {
      streaming <- Stream.resource(streaming)
      msg <- streaming.read(Set(streamKey(boardId)))
      stroke <- readMessage(msg)
    } yield stroke

    override def save(in: Stream[F, BoardStroke]): Stream[F, String] = for {
      streaming <- Stream.resource(streaming)
      pipe = streaming.append
      msgs = in.map(addMessage)
      res <- msgs.through(pipe)
    } yield res.value

    private def addMessage(stroke: BoardStroke): XAddMessage[String, String] =
      XAddMessage(streamKey(stroke.whiteboardId), Map(stroke.whiteboardId.toString -> stroke.data.toJson))

    private def readMessage(msg: XReadMessage[String, String]): Stream[F, BoardStroke] = for {
      (id, json) <- Stream.iterable(msg.body)
      data <- Stream.fromEither(BoardStrokeData.fromJson(json))
      res = BoardStroke(UUID.fromString(id), data)
    } yield res

    private def streamKey(boardId: UUID): String = s"${cfg.streamKey}-${boardId.toString}"
  }
}
