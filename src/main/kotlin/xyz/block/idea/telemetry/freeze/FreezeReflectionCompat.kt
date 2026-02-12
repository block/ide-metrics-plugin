package xyz.block.idea.telemetry.freeze

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.messages.Topic
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * Typed snapshot of data extracted from the internal `ThreadDump` class.
 *
 * @property edtStackTrace stack trace of the EDT at the time of the dump, or null if unavailable
 * @property rawDump the full thread dump text
 */
internal class ThreadDumpSnapshot(
  val edtStackTrace: Array<StackTraceElement>?,
  val rawDump: String,
)

/**
 * Reflection utilities for accessing internal JetBrains performance monitoring APIs.
 *
 * `IdePerformanceListener` and `ThreadDump` are annotated `@ApiStatus.Internal`, so the
 * JetBrains Marketplace plugin verifier rejects plugins that reference them in bytecode.
 * This object keeps all access string-based ([Class.forName], [Proxy]) so that no internal
 * class or method references appear in the compiled output.
 */
internal object FreezeReflectionCompat {

  private val log = Logger.getInstance(FreezeReflectionCompat::class.java)

  private const val LISTENER_CLASS_NAME = "com.intellij.diagnostic.IdePerformanceListener"

  /**
   * Subscribes [listener] to IDE freeze events via the internal
   * `IdePerformanceListener.TOPIC` message bus topic.
   *
   * @throws ReflectiveOperationException if the internal API surface has changed
   */
  fun subscribeToFreezeEvents(listener: FreezeListener) {
    val listenerClass = Class.forName(LISTENER_CLASS_NAME)

    @Suppress("UNCHECKED_CAST")
    val topic = listenerClass.getField("TOPIC").get(null) as Topic<Any>

    val proxy = Proxy.newProxyInstance(
      listenerClass.classLoader,
      arrayOf(listenerClass),
      FreezeInvocationHandler(listener),
    )

    val connection = ApplicationManager.getApplication().messageBus.connect()
    connection.subscribe(topic, proxy)
  }

  /**
   * Extracts a typed [ThreadDumpSnapshot] from an opaque `ThreadDump` instance.
   * Handles both Kotlin property accessors (`getEdtStackTrace`) and Java record
   * accessors (`edtStackTrace`).
   */
  fun extractSnapshot(threadDump: Any): ThreadDumpSnapshot {
    return ThreadDumpSnapshot(
      edtStackTrace = extractEdtStackTrace(threadDump),
      rawDump = extractRawDump(threadDump),
    )
  }

  @Suppress("UNCHECKED_CAST")
  private fun extractEdtStackTrace(dump: Any): Array<StackTraceElement>? {
    return try {
      val method = dump.javaClass.methods.firstOrNull {
        it.name in listOf("getEdtStackTrace", "edtStackTrace") && it.parameterCount == 0
      } ?: return null
      method.invoke(dump) as? Array<StackTraceElement>
    } catch (e: Exception) {
      log.warn("Failed to extract EDT stack trace from thread dump", e)
      null
    }
  }

  private fun extractRawDump(dump: Any): String {
    return try {
      val method = dump.javaClass.methods.firstOrNull {
        it.name in listOf("getRawDump", "rawDump") && it.parameterCount == 0
      } ?: return ""
      method.invoke(dump) as? String ?: ""
    } catch (e: Exception) {
      log.warn("Failed to extract raw dump from thread dump", e)
      ""
    }
  }
}

/**
 * Dynamic proxy handler that bridges reflective `IdePerformanceListener` calls
 * to the typed [FreezeListener], extracting [ThreadDumpSnapshot]s along the way.
 */
private class FreezeInvocationHandler(
  private val listener: FreezeListener,
) : InvocationHandler {

  override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
    return when (method.name) {
      "dumpedThreads" -> {
        val dump = args?.getOrNull(1) ?: return null
        listener.onDumpedThreads(FreezeReflectionCompat.extractSnapshot(dump))
        null
      }
      "uiFreezeFinished" -> {
        val durationMs = args?.getOrNull(0) as? Long ?: return null
        listener.onFreezeFinished(durationMs)
        null
      }
      "hashCode" -> System.identityHashCode(proxy)
      "equals" -> proxy === args?.getOrNull(0)
      "toString" -> "FreezeListener(proxy)"
      else -> null
    }
  }
}
