package xyz.block.idea.telemetry.freeze

/**
 * A synthetic exception representing an IDE freeze. The stack trace is set to the
 * EDT stack trace captured during the freeze, so Sentry can group and display it
 * as a meaningful stack trace rather than a generic event.
 */
internal class FreezeException(
  message: String,
  val durationMs: Long,
  edtStackTrace: Array<StackTraceElement>,
) : Exception(message) {
  init {
    stackTrace = edtStackTrace
  }
}
