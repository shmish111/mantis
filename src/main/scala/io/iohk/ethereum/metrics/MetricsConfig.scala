package io.iohk.ethereum.metrics

import com.typesafe.config.{Config ⇒ TypesafeConfig}
import io.micrometer.core.instrument.{Tag, Tags}
import scala.collection.JavaConverters._

final case class MetricsConfig(
  enabled: Boolean,
  host: String,
  port: Int,
  queueSize: Int,
  logErrors: Boolean,
  defaultTags: List[Tag]
)

object MetricsConfig {
  object Keys {
    final val Metrics = "metrics"

    final val Enabled = "enabled"
    final val Host = "host"
    final val Port = "port"
    final val QueueSize = "queue-size"
    final val LogErrors = "log-errors"

    final val Environment = "environment"
    final val Deployment = "deployment"
  }

  def apply(mantisConfig: TypesafeConfig): MetricsConfig = {
    val config = mantisConfig.getConfig(Keys.Metrics)

    val enabled = config.getBoolean(Keys.Enabled)
    val host = config.getString(Keys.Host)
    val port = config.getInt(Keys.Port)
    val queueSize = config.getInt(Keys.QueueSize)
    val logErrors = config.getBoolean(Keys.LogErrors)

    // For now, we support these two tags.
    // For historical reasons, in the backend they will be communicated as `env` and `depl` respectively.
    // These are default tags that must always accompany every metric.
    val environment = config.getString(Keys.Environment)
    val deployment = config.getString(Keys.Deployment)

    val tags =
      if(enabled) {
        if(environment.trim.isEmpty) {
          throw new IllegalArgumentException("Empty value for " + Keys.Environment + " in metrics configuration")
        }

        if(deployment.trim.isEmpty) {
          throw new IllegalArgumentException("Empty value for " + Keys.Deployment + " in metrics configuration")
        }

        Tags.of(
          Tag.of("env", environment),
          Tag.of("depl", deployment)
        ).asScala.toList
      } else Nil

    MetricsConfig(
      enabled = enabled,
      host = host,
      port = port,
      queueSize = queueSize,
      logErrors = logErrors,
      defaultTags = tags
    )
  }
}
