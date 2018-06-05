package io.iohk.ethereum.metrics

import java.time.Duration

import io.micrometer.statsd.{StatsdConfig, StatsdFlavor}

// Note https://micrometer.io/docs/registry/datadog
class DatadogRegistryConfig(metricsConfig: MetricsConfig) extends StatsdConfig {
  /**
   * Default tags we send to StatsD (actually Datadog).
   *
   * See https://github.com/input-output-hk/iohk-ops/blob/618748e09035f7bc3e3b055818c0cde4cf1958ce/modules/production.nix#L15
   */
  object Tag {
    final val Env = "env"
    final val Depl = "depl"
  }

  def get(key: String): String = null

  override def prefix(): String = Metrics.Prefix

  override def flavor(): StatsdFlavor = StatsdFlavor.DATADOG

  override def host(): String = metricsConfig.host

  override def port(): Int = metricsConfig.port

  override def enabled(): Boolean = metricsConfig.enabled

  override def pollingFrequency(): Duration = Duration.ofSeconds(5) // FIXME configurable
}
