package paint_fun.persistence

import cats.effect.{Async, Blocker, ContextShift, Resource}
import com.zaxxer.hikari.HikariConfig
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import org.slf4j.{Logger, LoggerFactory}
import paint_fun.config

import scala.concurrent.ExecutionContext

final class DbConnection[F[_] : Async : ContextShift] {

  val logger: Logger = LoggerFactory.getLogger(getClass)
  private val cfg = config.dbConfig

  lazy val transactor: Resource[F, HikariTransactor[F]] = {
    for {
      ec <- ExecutionContexts.fixedThreadPool[F](cfg.threadPoolSize)
      be <- Blocker[F]
      xa <- createTransactor(ec, be)
    } yield xa
  }

  private def createTransactor(ec: ExecutionContext, be: Blocker): Resource[F, HikariTransactor[F]] = {
    val config = new HikariConfig()
    config.setDriverClassName(cfg.driver)
    config.setJdbcUrl(cfg.connectionUrl)
    config.setUsername(cfg.username)
    config.setPassword(cfg.password)
    HikariTransactor.fromHikariConfig(config, ec, be)
  }
}

object DbConnection {
  def apply[F[_] : Async : ContextShift] = new DbConnection[F]
}
