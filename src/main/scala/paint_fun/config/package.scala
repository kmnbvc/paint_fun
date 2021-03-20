package paint_fun

import com.typesafe.config.{Config, ConfigFactory}

package object config {
  val config: Config = ConfigFactory.load()
  val redisConfig: RedisConfig = RedisConfig(config.getConfig("redis"))
}

final case class RedisConfig(url: String, streamKey: String)

object RedisConfig {
  def apply(cfg: Config): RedisConfig = {
    val url = cfg.getString("url")
    val key = cfg.getString("stream.key")
    RedisConfig(url, key)
  }
}
