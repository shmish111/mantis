package io.iohk.ethereum.metrics

import java.util.concurrent.atomic.AtomicReference

import com.typesafe.config.Config
import io.iohk.ethereum.utils.Logger
import io.micrometer.core.instrument._
import io.micrometer.core.instrument.binder.jvm.{JvmGcMetrics, JvmMemoryMetrics, JvmThreadMetrics}
import io.micrometer.core.instrument.composite.CompositeMeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.micrometer.jmx.JmxMeterRegistry
import io.micrometer.statsd.StatsdMeterRegistry

import scala.collection.JavaConverters._

case class Metrics(prefix: String, defaultTags: List[Tag], registry: MeterRegistry) {
  private[this] final val defaultTagsAsJava = defaultTags.asJava

  private[this] final val PrefixDot = prefix + "."

  private[this] def mkName(name: String): String = if(name.startsWith(PrefixDot)) name else PrefixDot + name

  def close(): Unit = registry.close()

  def deltaSpike(name: String): DeltaSpikeGauge =
    new DeltaSpikeGauge(name, this)

  /**
   * Creates a new [[io.micrometer.core.instrument.Gauge Gauge]]
   * which has:
   *
   *   - a name prefixed with [[io.iohk.ethereum.metrics.Metrics#prefix prefix]]
   *   - and default [[io.iohk.ethereum.metrics.Metrics#defaultTags tags]].
   *
   * @param computeValue A function that computes the current gauge value.
   */
  def newGauge(name: String, computeValue: () ⇒ Double): Gauge =
    Gauge
      .builder[Null](mkName(name), null, _ ⇒ computeValue())
      .tags(defaultTagsAsJava)
      .register(registry)

  def newCounter(name: String): Counter =
    Counter
      .builder(mkName(name))
      .tags(defaultTagsAsJava)
      .register(registry)

  def newTimer(name: String): Timer =
    Timer
      .builder(mkName(name))
      .tags(defaultTagsAsJava)
      .register(registry)
}

object Metrics extends Logger {
  private[this] final val StdMetricsClock = Clock.SYSTEM

  //+ Metrics singleton support
  private[this] final val metricsSentinel = Metrics(Prefix, Nil, new SimpleMeterRegistry())

  private[this] final val metricsRef = new AtomicReference[Metrics](metricsSentinel)

  private[this] def setOnce(metrics: Metrics): Boolean = metricsRef.compareAndSet(metricsSentinel, metrics)

  def get(): Metrics = metricsRef.get()
  //- Metrics singleton support

  /**
   * A prefix for all metrics.
   */
  final val Prefix = "mantis" // TODO there are several other strings of this value. Can we consolidate?

  /**
   * Instantiates and configures the metrics "service". This should happen once in the lifetime of the application.
   * After this call completes successfully, you can obtain the metrics service by using `Metrics.get()`.
   */
  def configure(config: MetricsConfig): Unit = {
    if(config.enabled) {
      val jmx = new JmxMeterRegistry(new JmxRegistryConfig, StdMetricsClock)

      //+ Backend-specific
      val datadog = new StatsdMeterRegistry(new DatadogRegistryConfig(config), StdMetricsClock)
      datadog.start()
      log.info(s"Started Datadog registry: ${datadog}")
      //- Backend-specific

      val registry = new CompositeMeterRegistry(StdMetricsClock, java.util.Arrays.asList(jmx, datadog))

      new JvmMemoryMetrics().bindTo(registry)
      new JvmGcMetrics().bindTo(registry)
      new JvmThreadMetrics().bindTo(registry)

      val metrics = new Metrics(Prefix, config.defaultTags, registry)

      if(setOnce(metrics)) {
        log.info(s"Configured metrics: $metrics")
      } else {
        val err = s"Could not configure metrics: $metrics"
        log.error(err)
        metrics.close()
        throw new Exception(err)
      }
    }
  }

  /**
   * Instantiates and configures the metrics client. This should happen once in the lifetime of the application.
   * After this call completes successfully, you can obtain the metrics client by using `MetricsClient.get()`.
   */
  def configure(mantisConfig: Config): Unit = {
    val config = MetricsConfig(mantisConfig)
    configure(config)
  }
}
