package io.iohk.ethereum.jsonrpc

import io.iohk.ethereum.metrics.{Metrics, MetricsContainer}
import io.micrometer.core.instrument.{Counter, Timer}

class JsonRpcControllerMetrics(metrics: Metrics) extends MetricsContainer {

  /**
   * Counts attempts to call disabled methods.
   */
  final val DisabledMethodsCounter = metrics.newCounter("json.rpc.disabled.calls.counter")

  final val MethodsCounter = metrics.newCounter("json.rpc.methods.counter")
  final val MethodsTimer = metrics.newTimer("json.rpc.methods.timer")
  final val MethodsSuccessCounter = metrics.newCounter("json.rpc.methods.success.counter")
  final val MethodsExceptionCounter = metrics.newCounter("json.rpc.methods.exception.counter")
  final val MethodsErrorCounter = metrics.newCounter("json.rpc.methods.error.counter")

  def methodCounter(method: String): Counter =
    metrics.newCounter("json.rpc.method." + method + ".counter")

  def methodSuccessCounter(method: String): Counter =
    metrics.newCounter("json.rpc.method." + method + ".success.counter")

  def methodExceptionCounter(method: String): Counter =
    metrics.newCounter("json.rpc.method." + method + ".exception.counter")

  def methodErrorCounter(method: String): Counter =
    metrics.newCounter("json.rpc.method." + method + ".error.counter")

  def methodTimer(method: String): Timer =
    metrics.newTimer("json.rpc.method." + method + ".counter")
}
