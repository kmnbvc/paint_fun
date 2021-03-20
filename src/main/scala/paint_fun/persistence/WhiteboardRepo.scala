package paint_fun.persistence

import cats.effect.{Concurrent, ContextShift}
import dev.profunktor.redis4cats.connection.RedisClient
import dev.profunktor.redis4cats.data.RedisCodec
import dev.profunktor.redis4cats.log4cats._
import dev.profunktor.redis4cats.streams.RedisStream
import dev.profunktor.redis4cats.streams.data.XAddMessage
import fs2._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import paint_fun.config.redisConfig
import paint_fun.model.{BoardStroke, BoardStrokeData}

trait WhiteboardRepo[F[_]] {
  def strokes(boardId: String): Stream[F, BoardStroke]
  def save(in: Stream[F, BoardStroke]): Stream[F, String]
}

object WhiteboardRepo {
  implicit def apply[F[_] : WhiteboardRepo]: WhiteboardRepo[F] = implicitly

  def instance[F[_] : Concurrent : ContextShift]: WhiteboardRepo[F] = new WhiteboardRepoImpl[F]
}

class WhiteboardRepoImpl[F[_]](implicit
                               concurrent: Concurrent[F],
                               contextShift: ContextShift[F]
                              ) extends WhiteboardRepo[F] {

  implicit val logger: Logger[F] = Slf4jLogger.getLogger[F]

  private val codec = RedisCodec.Utf8

  private lazy val streaming = RedisClient[F].from(redisConfig.url).flatMap(client =>
    RedisStream.mkStreamingConnectionResource(client, codec))

  override def strokes(boardId: String): Stream[F, BoardStroke] = {
    Stream.resource(streaming).flatMap(s =>
      s.read(Set(redisConfig.streamKey))
        .filter(msg => msg.body.keySet == Set(boardId))
        .flatMap(msg => Stream.iterable(msg.body.map {
          case (id, data) => BoardStroke(id, BoardStrokeData.fromJson(data))
        })))
  }

  override def save(in: Stream[F, BoardStroke]): Stream[F, String] = {
    Stream.resource(streaming).flatMap { s =>
      val pipe = s.append
      in.map(stroke => XAddMessage(redisConfig.streamKey, Map(stroke.whiteboardId -> stroke.data.toString)))
        .through(pipe)
        .map(_.value)
    }
  }
}
