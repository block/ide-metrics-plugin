@file:Suppress("UnstableApiUsage")

package xyz.block.idea.telemetry.util.gradle

import com.intellij.openapi.diagnostic.Logger
import org.gradle.tooling.LongRunningOperation
import org.gradle.tooling.events.OperationType
import org.gradle.tooling.events.ProgressListener
import xyz.block.idea.telemetry.common.Constants
import xyz.block.idea.telemetry.listeners.sync.GradleBuildPhaseListener
import java.util.*

/**
 * Trailing closure compatible listener extension.
 */
internal fun LongRunningOperation.addProgressListener(
  operationType: OperationType,
  progressListener: ProgressListener
) = addProgressListener(progressListener, operationType)

internal fun LongRunningOperation.withBuildTraceId(id: UUID): LongRunningOperation {
  Logger.getInstance(GradleBuildPhaseListener::class.java).info("Injecting -D${Constants.BUILD_TRACE_ID_PROPERTY}=$id")
  return withSystemProperties(mapOf(
    Constants.BUILD_TRACE_ID_PROPERTY to id.toString(),
  ))
}