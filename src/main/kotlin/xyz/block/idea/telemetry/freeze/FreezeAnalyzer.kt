package xyz.block.idea.telemetry.freeze

import com.intellij.ide.plugins.PluginManager
import com.intellij.ide.plugins.PluginManagerCore
import java.util.concurrent.ConcurrentHashMap

/**
 * Analyzes EDT stack traces from freeze dumps.
 *
 * Uses [PluginManager.getLoadedPlugins] and plugin classloaders to dynamically identify which
 * plugin owns each stack frame. Classes not loaded by a plugin classloader (JDK, platform,
 * coroutines) are identified by package prefix.
 */
internal object FreezeAnalyzer {

  // Prefix that identifies IntelliJ platform classes (loaded by the app classloader, not a plugin classloader).
  // Only com.intellij.* is reliably platform-only; org.jetbrains.* is too broad (covers the Kotlin plugin, etc.).
  private const val PLATFORM_PREFIX = "com.intellij."

  /**
   * Identifies the plugin or subsystem that initiated the EDT work.
   * Walks the stack from bottom (caller) toward top, returning the first
   * third-party or bundled plugin name found. Falls back to "IntelliJ Platform"
   * if the entire stack is platform code.
   */
  fun identifyOrigin(edtStack: Array<StackTraceElement>): String {
    for (frame in edtStack.reversed()) {
      val name = resolvePluginName(frame.className) ?: continue
      if (name != PLATFORM_LABEL) return name
    }
    return PLATFORM_LABEL
  }

  /**
   * Builds a stable fingerprint for Sentry issue grouping.
   *
   * Freezes with the same origin plugin and same top non-infrastructure frames are grouped
   * together regardless of duration. This means a 5s freeze and a 92s freeze from the same
   * code path become the same Sentry issue with multiple events.
   */
  fun buildFingerprint(origin: String, edtStack: Array<StackTraceElement>): List<String> {
    val meaningfulFrames = edtStack
      .filter(::isMeaningfulFrame)
      .take(FINGERPRINT_FRAME_COUNT)
      .map { "${it.className}.${it.methodName}" }

    return listOf("freeze", origin) + meaningfulFrames
  }

  /**
   * Finds the topmost meaningful frame — the first frame in the responsible plugin
   * or subsystem, skipping JDK/coroutine infrastructure.
   */
  fun findTopMeaningfulFrame(edtStack: Array<StackTraceElement>): StackTraceElement? {
    return edtStack.firstOrNull(::isMeaningfulFrame)
  }

  /** Filters out JDK, coroutine infrastructure, and synthetic lambda frames. */
  private fun isMeaningfulFrame(frame: StackTraceElement): Boolean {
    val className = frame.className
    return !className.startsWith("java.") &&
      !className.startsWith("javax.") &&
      !className.startsWith("jdk.") &&
      !className.startsWith("sun.") &&
      !className.startsWith("kotlinx.coroutines.") &&
      !className.startsWith("kotlin.coroutines.") &&
      !className.startsWith("kotlin.jvm.internal.") &&
      !className.contains("\$\$Lambda")
  }

  /**
   * Builds a descriptive message for the freeze suitable as a Sentry issue title.
   * Duration is intentionally excluded so that different-length freezes of the same
   * code path group together as one issue.
   */
  fun buildFreezeMessage(origin: String, edtStack: Array<StackTraceElement>): String {
    val topFrame = findTopMeaningfulFrame(edtStack)
      ?.let { shortenClass("${it.className}.${it.methodName}") }
      ?: "unknown"
    return "IDE Freeze [$origin] at $topFrame"
  }

  private const val PLATFORM_LABEL = "IntelliJ Platform"
  private const val FINGERPRINT_FRAME_COUNT = 5

  // Cache: class name -> resolved label. Uses a sentinel for "null" to avoid re-lookups.
  private val pluginNameCache = ConcurrentHashMap<String, String>()
  private const val CACHE_NULL = "\u0000"

  /**
   * Resolves the owning plugin or subsystem name for a class.
   *
   * - Plugin classes (loaded by a plugin classloader):
   *   returns the plugin's display name via [PluginManager.getPluginByClass].
   * - Platform classes (`com.intellij.*`, `org.jetbrains.*`): returns [PLATFORM_LABEL].
   * - JDK / coroutine infrastructure: returns `null`.
   */
  private fun resolvePluginName(className: String): String? {
    val cached = pluginNameCache[className]
    if (cached != null) return cached.takeIf { it != CACHE_NULL }

    val result = resolvePluginNameUncached(className)
    pluginNameCache[className] = result ?: CACHE_NULL
    return result
  }

  private fun resolvePluginNameUncached(className: String): String? {
    // Try each plugin's classloader to find who owns this class. We can't use Class.forName()
    // because it only sees our own plugin's classloader.
    // When a plugin's classloader successfully loads the class, that plugin owns it.
    for (plugin in PluginManager.getLoadedPlugins()) {
      val classLoader = plugin.pluginClassLoader ?: continue
      try {
        classLoader.loadClass(className)
        // The core platform plugin owns com.intellij.* / org.jetbrains.* classes — label it clearly
        if (plugin.pluginId == PluginManagerCore.CORE_ID) return PLATFORM_LABEL
        return plugin.pluginId.idString
      } catch (_: ClassNotFoundException) {
        continue
      } catch (_: NoClassDefFoundError) {
        continue
      }
    }

    // Not a plugin class — check if it's a platform class by package prefix
    if (className.startsWith(PLATFORM_PREFIX)) return PLATFORM_LABEL

    // JDK or otherwise unresolvable
    return null
  }

  internal fun shortenClass(fqcn: String): String {
    val parts = fqcn.split(".")
    for ((i, p) in parts.withIndex()) {
      if (p.isNotEmpty() && p[0].isUpperCase()) {
        return parts.subList(i, parts.size).joinToString(".")
      }
    }
    return fqcn
  }
}
