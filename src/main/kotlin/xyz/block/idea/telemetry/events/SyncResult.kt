package xyz.block.idea.telemetry.events

import org.gradle.util.GradleVersion
import java.util.*

internal sealed class SyncResult(
  open val buildTraceId: UUID?,
  open val gradleVersion: GradleVersion?,
  open val startTimestamp: Long,
  open val finishTimestamp: Long,
) {
  val totalDuration: Long get() = finishTimestamp - startTimestamp

  val resultName: String get() = when (this) {
    is SyncSucceeded -> "succeeded"
    is SyncCancelled -> "cancelled"
    is SyncFailed -> "failed"
  }

  data class SyncSucceeded(
    override val buildTraceId: UUID?,
    override val gradleVersion: GradleVersion?,
    override val startTimestamp: Long,
    override val finishTimestamp: Long,
    val projectCount: Int,
    val hasIncludedBuilds: Boolean,
    val configureIncludedBuildsFinishTimestamp: Long,
    val configureRootProjectFinishTimestamp: Long,
    val gradleFinishTimestamp: Long,
  ) : SyncResult(buildTraceId, gradleVersion, startTimestamp, finishTimestamp) {
    val configureIncludedBuildsDuration: Long
      get() = when (hasIncludedBuilds) {
        true -> configureIncludedBuildsFinishTimestamp - startTimestamp
        else -> 0
      }

    val configureRootProjectDuration: Long
      get() = when (hasIncludedBuilds) {
        true -> configureRootProjectFinishTimestamp - configureIncludedBuildsFinishTimestamp
        else -> configureRootProjectFinishTimestamp - startTimestamp
      }

    val gradleExecutionDuration: Long
      get() = gradleFinishTimestamp - configureRootProjectFinishTimestamp

    val gradleDuration: Long
      get() = gradleFinishTimestamp - startTimestamp

    val ideDuration: Long
      get() = finishTimestamp - gradleFinishTimestamp
  }

  data class SyncFailed(
    override val buildTraceId: UUID?,
    override val gradleVersion: GradleVersion?,
    override val startTimestamp: Long,
    override val finishTimestamp: Long,
    val phase: SyncPhase,
    val exception: Throwable,
  ) : SyncResult(buildTraceId, gradleVersion, startTimestamp, finishTimestamp)

  data class SyncCancelled(
    override val buildTraceId: UUID?,
    override val gradleVersion: GradleVersion?,
    override val startTimestamp: Long,
    override val finishTimestamp: Long,
    val phase: SyncPhase,
  ) : SyncResult(buildTraceId, gradleVersion, startTimestamp, finishTimestamp)
}