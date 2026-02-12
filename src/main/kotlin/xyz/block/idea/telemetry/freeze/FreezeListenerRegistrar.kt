package xyz.block.idea.telemetry.freeze

import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Registers a freeze listener when the first project opens.
 *
 * Subscription to the internal `IdePerformanceListener.TOPIC` is handled entirely by
 * [FreezeReflectionCompat], keeping this class free of reflection details.
 */
internal class FreezeListenerRegistrar : ProjectActivity {

  override suspend fun execute(project: Project) {
    if (!registered.compareAndSet(false, true)) return

    try {
      FreezeReflectionCompat.subscribeToFreezeEvents(FreezeListener())
      thisLogger().info("Freeze listener registered successfully.")
    } catch (e: Exception) {
      registered.set(false)
      thisLogger().warn("Failed to register freeze listener — freeze detection disabled.", e)

      NotificationGroupManager.getInstance()
        .getNotificationGroup(NOTIFICATION_GROUP)
        .createNotification(
          "Freeze detection unavailable",
          "The IDE performance listener could not be registered.",
          NotificationType.ERROR,
        )
        .addAction(NotificationAction.createSimpleExpiring("Report issue") {
          BrowserUtil.browse(ISSUES_URL)
        })
        .notify(project)
    }
  }

  companion object {
    private val registered = AtomicBoolean(false)
    private const val NOTIFICATION_GROUP = "Block IDE Metrics"
    private const val ISSUES_URL = "https://github.com/block/ide-metrics-plugin/issues/new"
  }
}
