package paint_fun.persistence

import cats.effect.{Blocker, ContextShift, ExitCase, IO, Resource}
import doobie.ConnectionIO
import doobie.hikari.HikariTransactor
import doobie.implicits._
import doobie.util.ExecutionContexts
import org.slf4j.Logger
import paint_fun.config

import scala.concurrent.ExecutionContext

trait DbConnection {

  val logger: Logger
  private val cfg = config.dbConfig

  lazy val transactor: Resource[IO, HikariTransactor[IO]] = {
    for {
      ec <- ExecutionContexts.fixedThreadPool[IO](cfg.threadPoolSize)
      be <- Blocker[IO]
      xa <- createTransactor(ec, be, IO.contextShift(ec))
    } yield xa
  }

  private def createTransactor(ec: ExecutionContext, be: Blocker, cs: ContextShift[IO]): Resource[IO, HikariTransactor[IO]] = {
    implicit val ctxShift: ContextShift[IO] = cs
    HikariTransactor.newHikariTransactor[IO](cfg.driver, cfg.connectionUrl, cfg.username, cfg.password, ec, be)
  }

  def transact[T](statement: => ConnectionIO[T]): IO[T] =
    transactor.use(xa => statement.transact(xa))
      .guaranteeCase {
        case ExitCase.Error(e) => IO(logger.error(e.getMessage, e))
        case _ => IO.unit
      }
}
