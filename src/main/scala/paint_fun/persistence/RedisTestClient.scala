package paint_fun.persistence

import cats.effect.{ExitCode, IO, IOApp, Sync}
import cats.syntax.parallel._
import dev.profunktor.redis4cats.connection.RedisClient
import dev.profunktor.redis4cats.data._
import dev.profunktor.redis4cats.log4cats._
import dev.profunktor.redis4cats.streams.RedisStream
import dev.profunktor.redis4cats.streams.data._
import fs2.{INothing, Stream}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Random

class RedisTestClient {
  implicit val timer = IO.timer(ExecutionContext.global)
  implicit val cs = IO.contextShift(ExecutionContext.global)
  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val stringCodec = RedisCodec.Utf8

  def putStrLn[A](a: A): IO[Unit] = IO(println(a))

  val streamKey1 = "demo"
  val streamKey2 = "users"

  def randomMessage: Stream[IO, XAddMessage[String, String]] = Stream.eval {
    val rndKey = IO(Random.nextInt(1000).toString)
    val rndValue = IO(Random.nextString(10))
    (rndKey, rndValue).parMapN {
      case (k, v) => XAddMessage(streamKey1, Map(k -> v))
    }
  }

  def testRun(): Stream[IO, INothing] = {
    val res = for {
      client <- Stream.resource(RedisClient[IO].from("redis://localhost"))
      streaming <- RedisStream.mkStreamingConnection[IO, String, String](client, stringCodec)
      source = streaming.read(Set(streamKey1, streamKey2))
      appender = streaming.append
      rs <- Stream(
        source.evalMap(putStrLn(_)),
        Stream.awakeEvery[IO](3.seconds) >> randomMessage.through(appender)
      ).parJoin(2).drain
    } yield rs

    res
  }
}

//object RedisApp extends IOApp {
//  override def run(args: List[String]): IO[ExitCode] = {
//    val client = new RedisTestClient
//    val rs = client.testRun()
//    rs.compile.drain.unsafeRunSync()
//
//    IO(ExitCode.Success)
//  }
//}
