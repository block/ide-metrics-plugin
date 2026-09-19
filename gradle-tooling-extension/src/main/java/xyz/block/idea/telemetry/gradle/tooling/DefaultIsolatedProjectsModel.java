package xyz.block.idea.telemetry.gradle.tooling;

public final class DefaultIsolatedProjectsModel implements IsolatedProjectsModel {
  private static final long serialVersionUID = 1L;

  private final boolean enabled;

  public DefaultIsolatedProjectsModel(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public String toString() {
    return "IsolatedProjectsModel(enabled=" + enabled + ")";
  }
}
