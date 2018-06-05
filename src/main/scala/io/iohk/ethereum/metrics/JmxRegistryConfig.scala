package io.iohk.ethereum.metrics

import io.micrometer.jmx.JmxConfig

class JmxRegistryConfig extends JmxConfig {
  def get(key: String): String = null

  override def prefix(): String = Metrics.Prefix

  override def domain(): String = prefix()
}
