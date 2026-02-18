# Block IDE Metrics Plugin

https://github.com/block/ide-metrics-plugin
https://plugins.jetbrains.com/plugin/28394-build-sync-metrics

## Usage

This plugin supports two telemetry backends. The plugin automatically detects which backend to use based on the URL you configure.

### Option 1: Eventstream

Configure with your eventstream server endpoint:

```properties
# gradle.properties
ide-metrics-plugin.event-stream-endpoint=<endpoint>
```

### Option 2: Google Forms (Simpler Setup)

For users who want a simpler setup without infrastructure, use Google Forms:

#### Setup Steps

1. **Create Google Form**
   - Create a new Google Form with fields for the metrics you want to track
   - Add short answer text fields
   - Name each field with a descriptive label (e.g., "Sync Type", "Sync Time")

2. **Generate Prefilled Link**
   - Click the three dots (⋮) → "Get pre-filled link"
   - Fill each field with its placeholder code (see table below)
   - Click "Get Link" and copy the URL

3. **Configure Plugin**
   - Add to your project's `gradle.properties`:
     ```properties
     ide-metrics-plugin.event-stream-endpoint=<your-prefilled-google-forms-url>
     ```

#### Placeholder Codes

Use these exact placeholder codes when generating your prefilled link:

| Field | Placeholder Code | Description |
|-------|------------------|-------------|
| Sync Type | `SYNC_TYPE` | succeeded/failed/cancelled |
| Sync Time | `SYNC_TIME` | Total duration in milliseconds |
| Configure Included Builds Duration | `CONFIGURE_INCLUDED_BUILDS_DURATION` | Phase duration in ms |
| Configure Root Project Duration | `CONFIGURE_ROOT_PROJECT_DURATION` | Phase duration in ms |
| Gradle Execution Duration | `GRADLE_EXECUTION_DURATION` | Phase duration in ms |
| Gradle Duration | `GRADLE_DURATION` | Total gradle phase in ms |
| IDE Duration | `IDE_DURATION` | IDE processing phase in ms |
| JVM Total Memory | `JVM_TOTAL_MEMORY` | Bytes as string |
| JVM Free Memory | `JVM_FREE_MEMORY` | Bytes as string |
| Available Processors | `AVAILABLE_PROCESSORS` | CPU core count |
| CPU Name | `CPU_NAME` | CPU brand string |
| Number of Modules | `NUMBER_OF_MODULES` | Module count |
| Active Workspace | `ACTIVE_WORKSPACE` | Active workspace name |
| Error Message | `ERROR_MESSAGE` | Error details if sync failed |
| Studio Version | `STUDIO_VERSION` | Android Studio version |
| Toolkit Version | `TOOLKIT_VERSION` | Plugin version |
| AGP Version | `AGP_VERSION` | Android Gradle Plugin version |
| Gradle Version | `GRADLE_VERSION` | Gradle version |
| IntelliJ Core Version | `INTELLIJ_CORE_VERSION` | IntelliJ Core version |
| User LDAP | `USER_LDAP` | System username |
| OS System Architecture | `OS_SYSTEM_ARCHITECTURE` | OS architecture |
| Artifact Sync Enabled | `ARTIFACT_SYNC_ENABLED` | Artifact sync flag |
| Active Root Project Name | `ACTIVE_ROOT_PROJECT_NAME` | Gradle root project name |
| SA Toolbox Channel | `SA_TOOLBOX_CHANNEL` | Toolbox channel |
| Sync Trace ID | `SYNC_TRACE_ID` | Correlation ID |

**Note**: You don't need to include all fields. Only add the fields you want to track. Unknown placeholders will be skipped.

#### Example Prefilled URL

```
https://docs.google.com/forms/d/e/FORM_ID/formResponse?usp=pp_url&entry.1=SYNC_TYPE&entry.2=SYNC_TIME&entry.3=CONFIGURE_INCLUDED_BUILDS_DURATION&...
```

**Note**: The plugin automatically detects which backend to use based on the URL:
- URLs containing `docs.google.com` → Google Forms
- All other URLs → Eventstream

### Advanced: Delegate Config File

Both approaches support delegate config files:

```
# gradle.properties
ide-metrics-plugin.config-file=<relative path to preferred config file>
```
and
```
# preferred-config-file.properties
ide-metrics-plugin.event-stream-endpoint=<endpoint>
```
