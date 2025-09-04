package xyz.block.idea.telemetry.events

@Suppress("unused")
internal enum class SyncPhase {
  INITIALIZE,
  CONFIGURE_INCLUDED_BUILDS,
  CONFIGURE_ROOT_PROJECT,
  BUILD_MODELS,
  IMPORT_MODELS,
  ;
}