Release procedure for this repository

# Releasing ide-metrics IDE plugin

The release process for the IDE plugin is automated and based on change detection that runs on a 
schedule. See `.github/workflows/tag-changes.yml`.

Publishing can also be manually invoked via the `publish-ide-metrics-plugin` action on Github. The 
version is defined as `pluginVersion` in `gradle.properties`.

# Releasing Gradle plugin and dependency libraries

1. Update `gradle-plugin/CHANGELOG.md`.
1. Update `README.md` if needed.
1. Bump `publish_version` in `gradle.properties` to next stable version (removing the `-SNAPSHOT` 
   suffix).
1. `git commit -am "chore(gradle): prepare for release x.y.z."`
1. Push this as a PR and merge it.
1. Publish to Maven Central by invoking the `Publish gradle plugin and dependencies` action on 
   Github.
1. `git tag -a vx.y.z-gradle -m "Gradle plugin version x.y.z."`
1. Update version number `gradle.properties` to next snapshot version (x.y.z-SNAPSHOT)
1. `git commit -am "chore(gradle): prepare next development version."`
1. `git push && git push --tags`
