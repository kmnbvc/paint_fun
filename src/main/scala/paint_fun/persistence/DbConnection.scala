package paint_fun.persistence

import cats.effect.{Async, Blocker, ContextShift, Resource}
import doobie.ConnectionIO
import doobie.hikari.HikariTransactor
import doobie.implicits._
import doobie.util.ExecutionContexts
import org.slf4j.Logger
import paint_fun.config

import scala.concurrent.ExecutionContext

abstract class DbConnection[F[_]](implicit async: Async[F], cs: ContextShift[F]) {

  val logger: Logger
  private val cfg = config.dbConfig

  lazy val transactor: Resource[F, HikariTransactor[F]] = {
    for {
      ec <- ExecutionContexts.fixedThreadPool[F](cfg.threadPoolSize)
      be <- Blocker[F]
      xa <- createTransactor(ec, be)
    } yield xa
  }

  private def createTransactor(ec: ExecutionContext, be: Blocker): Resource[F, HikariTransactor[F]] = {
    HikariTransactor.newHikariTransactor[F](cfg.driver, cfg.connectionUrl, cfg.username, cfg.password, ec, be)
  }

  def transact[T](statement: => ConnectionIO[T]): F[T] =
    transactor.use(xa => statement.transact(xa))
}
