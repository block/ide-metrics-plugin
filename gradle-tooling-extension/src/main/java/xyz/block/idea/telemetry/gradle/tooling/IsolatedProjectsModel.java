package xyz.block.idea.telemetry.gradle.tooling;

import java.io.Serializable;

/**
 * Build-level tooling model reporting whether Gradle's isolated projects feature was active for
 * the build that was synced. Built in the Gradle daemon by {@link IsolatedProjectsModelBuilder}.
 */
public interface IsolatedProjectsModel extends Serializable {
  boolean isEnabled();
}
