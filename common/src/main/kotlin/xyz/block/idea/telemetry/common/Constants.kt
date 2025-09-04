package xyz.block.idea.telemetry.common

public object Constants {
  /**
   * This system property is injected into the gradle build so it can be attached to a build scan. We can correlate
   * build scans with telemetry submitted by the IDE plugin using this property.
   */
  public const val BUILD_TRACE_ID_PROPERTY: String = "xyz.block.idea.telemetry.buildTraceId"
}
