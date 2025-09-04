package xyz.block.idea.telemetry.util.intellij

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId

fun getPluginVersion(id: String): String =
  PluginManagerCore.getPlugin(PluginId.getId(id))?.version.orEmpty()

const val ANDROID_PLUGIN_ID = "org.jetbrains.android"
const val INTELLIJ_CORE_ID = "com.intellij"
const val KOTLIN_PLUGIN_ID = "org.jetbrains.kotlin"
const val BLOCK_TELEMETRY_PLUGIN_ID = "xyz.block.idea.telemetry"