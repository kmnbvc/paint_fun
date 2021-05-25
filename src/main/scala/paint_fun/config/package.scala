package paint_fun

import pureconfig._
import pureconfig.generic.auto._

package object config {
  final case class ServiceConfig(database: DatabaseConfig, redis: RedisConfig)
  final case class RedisConfig(url: String, streamKey: String)
  final case class DatabaseConfig(driver: String, connectionUrl: String, username: String, password: String, threadPoolSize: Int)

  private val cfg = ConfigSource.default.loadOrThrow[ServiceConfig]
  val redis: RedisConfig = cfg.redis
  val database: DatabaseConfig = cfg.database
}
