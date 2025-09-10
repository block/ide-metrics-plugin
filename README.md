# Block IDE Metrics Plugin

https://github.com/block/ide-metrics-plugin
https://plugins.jetbrains.com/plugin/28394-build-sync-metrics

## Usage

Users of this plugin will need to add this to their repo's gradle.properties file, creating that 
file if necessary:

```
# gradle.properties
ide-metrics-plugin.event-stream-endpoint=<endpoint>
```

Alternatively

```
# gradle.properties
ide-metrics-plugin.config-file=<relative path to preferred config file>
```
and
```
# preferred-config-file.properties
ide-metrics-plugin.event-stream-endpoint=<endpoint>
```
