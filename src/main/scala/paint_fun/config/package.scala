package paint_fun

import com.typesafe.config.{Config, ConfigFactory}

package object config {
  val config: Config = ConfigFactory.load()
  val redisConfig: RedisConfig = RedisConfig(config.getConfig("redis"))
  val dbConfig: DatabaseConfig = DatabaseConfig(config.getConfig("database"))


  final case class RedisConfig(url: String, streamKey: String)

  object RedisConfig {
    def apply(cfg: Config): RedisConfig = RedisConfig(cfg.getString("url"),
      cfg.getString("stream.key"))
  }

  final case class DatabaseConfig(driver: String,
                                  connectionUrl: String,
                                  username: String,
                                  password: String,
                                  threadPoolSize: Int)

  object DatabaseConfig {
    def apply(cfg: Config): DatabaseConfig =
      DatabaseConfig(cfg.getString("driver"),
        cfg.getString("connection-url"),
        cfg.getString("username"),
        cfg.getString("password"),
        cfg.getInt("thread-pool-size"))
  }

}
