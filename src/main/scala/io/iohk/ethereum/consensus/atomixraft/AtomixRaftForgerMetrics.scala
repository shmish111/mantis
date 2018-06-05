package io.iohk.ethereum.consensus.atomixraft

import io.iohk.ethereum.metrics.{Metrics, MetricsContainer}

class AtomixRaftForgerMetrics(
  metrics: Metrics,
  lastForgedBlockNumber: () ⇒ Double
) extends MetricsContainer {
  /**
   * Signifies a leadership change. This is emitted from the new leader.
   */
  final val LeaderEvent = metrics.deltaSpike("raft.leader.event")

  final val LastForgedBlockNumber = metrics.newGauge("raft.last.forged.block.number", lastForgedBlockNumber)

  /**
   * Counts how many times this node became a leader.
   */
  final val BecomeLeaderCounter = metrics.newCounter("raft.become.leader.counter")

  /**
   * Counts how many times this node changed role.
   */
  final val ChangeRoleCounter = metrics.newCounter("raft.change.role.counter")

  /**
   * Counts how many blocks forged by the leader.
   */
  final val LeaderForgedBlocksCounter = metrics.newCounter("raft.leader.forged.blocks.counter")
}
