package paint_fun.persistence

import cats.effect._
import com.zaxxer.hikari.HikariConfig
import doobie.Transactor
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import paint_fun.config

object DbConnection {

  private val cfg = config.dbConfig

  def transactor[F[_] : Async : ContextShift]: Resource[F, Transactor[F]] = for {
    ec <- ExecutionContexts.fixedThreadPool[F](cfg.threadPoolSize)
    be <- Blocker[F]
    xa <- HikariTransactor.fromHikariConfig(hikariConfig(), ec, be)
  } yield xa

  private def hikariConfig(): HikariConfig = {
    val config = new HikariConfig()
    config.setDriverClassName(cfg.driver)
    config.setJdbcUrl(cfg.connectionUrl)
    config.setUsername(cfg.username)
    config.setPassword(cfg.password)
    config
  }
}
