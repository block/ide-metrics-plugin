package xyz.block.idea.telemetry.gradle.tooling;

import javax.inject.Inject;
import org.gradle.api.Project;
import org.gradle.api.configuration.BuildFeatures;
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService;

/**
 * Builds {@link IsolatedProjectsModel} inside the Gradle daemon by asking Gradle's
 * {@link BuildFeatures} service, so the value reflects whatever Gradle actually resolved from
 * command line flags, system properties, gradle.properties and init scripts.
 *
 * <p>Requested as a build-level model, so {@code project} is the root project of the build.
 * Only that project's build-scoped services are touched, which keeps this safe under isolated
 * projects itself.
 */
public final class IsolatedProjectsModelBuilder implements ModelBuilderService {
  private static final long serialVersionUID = 1L;

  @Override
  public boolean canBuild(String modelName) {
    return IsolatedProjectsModel.class.getName().equals(modelName);
  }

  @Override
  public Object buildAll(String modelName, Project project) {
    return new DefaultIsolatedProjectsModel(isIsolatedProjectsActive(project));
  }

  private static boolean isIsolatedProjectsActive(Project project) {
    try {
      BuildFeatures buildFeatures = project.getObjects()
        .newInstance(BuildFeaturesHolder.class)
        .getBuildFeatures();
      return buildFeatures.getIsolatedProjects().getActive().getOrElse(false);
    } catch (Throwable e) {
      // BuildFeatures only exists since Gradle 8.5. Older Gradle versions predate stable
      // isolated projects support, so report it as off.
      return false;
    }
  }

  /** Gradle instantiates this via {@link org.gradle.api.model.ObjectFactory} and injects the service. */
  public abstract static class BuildFeaturesHolder {
    @Inject
    public abstract BuildFeatures getBuildFeatures();
  }
}
