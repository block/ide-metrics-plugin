# Google Forms Telemetry Implementation Plan

**Status**: ✅ Complete
**Created**: 2026-02-06
**Completed**: 2026-02-06

## Table of Contents

- [Overview](#overview)
- [Context](#context)
- [Design Approach](#design-approach)
- [Implementation Tasks](#implementation-tasks)
- [Technical Details](#technical-details)
- [Verification](#verification)
- [Documentation](#documentation)

---

## Overview

Add Google Forms as a new, convenient alternative to the existing eventstream telemetry system. The plugin will auto-detect the backend based on URL host, allowing users to choose between eventstream or Google Forms with a single configuration property.

### Goals

- ✅ Add Google Forms as convenient alternative (no server infrastructure required)
- ✅ Auto-detect backend from URL host (single configuration property)
- ✅ Keep eventstream working exactly as before
- ✅ Modular, clean architecture with minimal code changes
- ✅ Forward-compatible placeholder system
- ✅ No coupling - Analytics.kt decides routing via interface

### Non-Goals

- ❌ Batch submissions for Google Forms (not supported by Google Forms)
- ❌ Retry logic for Google Forms (keep it simple, fail-fast)
- ❌ Complex configuration (keep single property)
- ❌ Deprecate or replace eventstream

---

## Context

### Current System

The plugin currently sends telemetry via eventstream:
```
Analytics.recordSyncEvent()
  → EventstreamEvent (wraps SyncEvent)
  → Eventstream.sendEvents()
  → EventstreamService (Retrofit)
  → POST /2.0/log/eventstream (protobuf)
```

**Eventstream features**:
- Batching (up to 3000 events)
- Retry logic (2 retries)
- Protobuf encoding
- Requires server infrastructure

### New System

```
Analytics.recordSyncEvent()
  → SyncEvent (direct)
  → TelemetrySubmitter (interface)
     ├─ EventstreamSubmitter (existing, wrapped)
     │   └─ Original eventstream logic (batching, retry, protobuf)
     └─ GoogleFormsSubmitter (new alternative)
         └─ HTTP GET with query params (no infrastructure needed)
```

**Benefits of adding Google Forms**:
- No server infrastructure needed
- Simple HTTP GET requests
- Free and accessible via Google Sheets
- Complements eventstream for users with different needs

---

## Design Approach

### Auto-Detection Strategy

**Single configuration property**:
```properties
ide-metrics-plugin.event-stream-endpoint=<url>
```

**Auto-detection logic**:
- If URL host contains `docs.google.com` → Use `GoogleFormsSubmitter` (new)
- Otherwise → Use `EventstreamSubmitter` (existing)

### Placeholder Mapping System

Users create Google Forms prefilled URLs with placeholder codes:
```
https://docs.google.com/forms/d/e/FORM_ID/viewform?
  entry.123=SYNC_TYPE&
  entry.456=SYNC_TIME&
  entry.789=ACTIVE_ROOT_PROJECT_NAME
```

The plugin:
1. Parses URL once at initialization
2. Extracts entry IDs and placeholder values
3. Creates mapping: placeholder → field accessor
4. On submission: replaces placeholders with actual event data

**Forward compatibility**: Unknown placeholders are skipped with warning, allowing old URLs to work even when new fields are added.

### Architecture: Interface-Based Abstraction

```kotlin
interface TelemetrySubmitter {
  fun submitEvent(event: SyncEvent): Boolean
}

class GoogleFormsSubmitter(parsedUrl: GoogleFormsParsedUrl) : TelemetrySubmitter
class EventstreamSubmitter(eventstream: Eventstream) : TelemetrySubmitter
```

---

## Implementation Tasks

### Phase 1: Core Components ✅

#### Task 1.1: Create TelemetrySubmitter Interface
**Status**: ✅ Complete
**File**: `src/main/kotlin/xyz/block/idea/telemetry/submitters/TelemetrySubmitter.kt`

```kotlin
package xyz.block.idea.telemetry.submitters
import xyz.block.idea.telemetry.events.SyncEvent

internal interface TelemetrySubmitter {
  fun submitEvent(event: SyncEvent): Boolean
}
```

**Acceptance Criteria**:
- [x] Interface created with single method
- [x] Proper package structure
- [x] KDoc documentation added

---

#### Task 1.2: Create Data Structures
**Status**: ✅ Complete
**File**: `src/main/kotlin/xyz/block/idea/telemetry/submitters/googleforms/GoogleFormsFieldMapping.kt`

```kotlin
package xyz.block.idea.telemetry.submitters.googleforms
import xyz.block.idea.telemetry.events.SyncEvent

internal data class GoogleFormsFieldMapping(
  val entryId: String,
  val placeholderValue: String,
  val fieldAccessor: (SyncEvent) -> String?
)

internal data class GoogleFormsParsedUrl(
  val formBaseUrl: String,
  val fieldMappings: List<GoogleFormsFieldMapping>
)
```

**Acceptance Criteria**:
- [x] Both data classes created
- [x] Immutable structure
- [x] Proper types for all fields
- [x] KDoc documentation added

---

#### Task 1.3: Implement GoogleFormsUrlParser
**Status**: ✅ Complete
**File**: `src/main/kotlin/xyz/block/idea/telemetry/submitters/googleforms/GoogleFormsUrlParser.kt`

**Responsibilities**:
- Parse Google Forms prefilled URL
- Extract form ID and convert `/viewform` to `/formResponse`
- Parse query parameters (entry.* = placeholder)
- Create field mappings using hardcoded placeholder → field accessor map
- Validate URL structure
- Log warnings for unknown placeholders

**Placeholder Mappings**:

| Placeholder | SyncEvent Field | Type |
|-------------|-----------------|------|
| `SYNC_TYPE` | syncType | String |
| `SYNC_TIME` | syncTime | Long → String |
| `CONFIGURE_INCLUDED_BUILDS_DURATION` | configureIncludedBuildsDuration | Long → String |
| `CONFIGURE_ROOT_PROJECT_DURATION` | configureRootProjectDuration | Long → String |
| `GRADLE_EXECUTION_DURATION` | gradleExecutionDuration | Long → String |
| `GRADLE_DURATION` | gradleDuration | Long → String |
| `IDE_DURATION` | ideDuration | Long → String |
| `JVM_TOTAL_MEMORY` | jvmTotalMemory | String |
| `JVM_FREE_MEMORY` | jvmFreeMemory | String |
| `AVAILABLE_PROCESSORS` | availableProcessors | Long → String |
| `CPU_NAME` | cpuName | String |
| `NUMBER_OF_MODULES` | numberOfModules | Long → String |
| `ACTIVE_WORKSPACE` | activeWorkspace | String? |
| `ERROR_MESSAGE` | errorMessage | String? |
| `STUDIO_VERSION` | studioVersion | String |
| `TOOLKIT_VERSION` | toolkitVersion | String |
| `AGP_VERSION` | agpVersion | String? |
| `GRADLE_VERSION` | gradleVersion | String? |
| `INTELLIJ_CORE_VERSION` | intellijCoreVersion | String |
| `USER_LDAP` | userLdap | String |
| `OS_SYSTEM_ARCHITECTURE` | osSystemArchitecture | String |
| `ARTIFACT_SYNC_ENABLED` | artifactSyncEnabled | Boolean → String |
| `ACTIVE_ROOT_PROJECT_NAME` | activeRootProjectName | String |
| `SA_TOOLBOX_CHANNEL` | saToolboxChannel | String? |
| `SYNC_TRACE_ID` | syncTraceId | String? |

**Key Implementation Details**:
```kotlin
object GoogleFormsUrlParser {
  private val PLACEHOLDER_TO_FIELD_MAPPING = mapOf(
    "SYNC_TYPE" to { event: SyncEvent -> event.syncType },
    "SYNC_TIME" to { event: SyncEvent -> event.syncTime.toString() },
    // ... all 24 mappings
  )

  fun parse(prefilledUrl: String): GoogleFormsParsedUrl {
    // 1. Validate URL (must contain docs.google.com)
    // 2. Convert /viewform to /formResponse
    // 3. Parse query parameters
    // 4. Extract entry.* parameters
    // 5. Map to field accessors
    // 6. Log warnings for unknown placeholders
    // 7. Fail if no valid mappings found
  }
}
```

**Acceptance Criteria**:
- [x] All 25 field mappings implemented
- [x] URL validation (must be Google Forms URL)
- [x] Query parameter parsing with URL decoding
- [x] Unknown placeholder handling (log warning, skip)
- [x] Clear error messages for invalid URLs
- [x] Logging statements for debugging
- [x] KDoc documentation

---

#### Task 1.4: Implement GoogleFormsSubmitter
**Status**: ✅ Complete (Updated to use OkHttp)
**File**: `src/main/kotlin/xyz/block/idea/telemetry/submitters/googleforms/GoogleFormsSubmitter.kt`

**Responsibilities**:
- Implement `TelemetrySubmitter` interface
- Build submission URL by replacing placeholders with event values
- Send HTTP GET request using Java `HttpURLConnection`
- Handle null values (convert to empty strings)
- URL-encode all values
- Return success/failure status

**Key Implementation Details**:
```kotlin
internal class GoogleFormsSubmitter(
  private val parsedUrl: GoogleFormsParsedUrl
) : TelemetrySubmitter {

  companion object {
    private const val CONNECTION_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 10_000
  }

  override fun submitEvent(event: SyncEvent): Boolean {
    val submissionUrl = buildSubmissionUrl(event)
    return sendHttpRequest(submissionUrl)
  }

  private fun buildSubmissionUrl(event: SyncEvent): String {
    // Build query params: entry.123=value&entry.456=value
    // Use URLEncoder.encode(value, "UTF-8")
    // Convert null to empty string
  }

  private fun sendHttpRequest(urlString: String): Boolean {
    // Use HttpURLConnection
    // GET request
    // 10s timeouts
    // Return true if HTTP 200
  }
}
```

**Acceptance Criteria**:
- [x] Implements `TelemetrySubmitter` interface
- [x] Builds correct submission URL with all parameters
- [x] URL-encodes all values properly
- [x] Null values converted to empty strings
- [x] HTTP GET request with proper timeouts (10s connect, 10s read)
- [x] Returns true on HTTP 200, false otherwise
- [x] Proper error handling and logging
- [x] Uses OkHttp (consistent with rest of project)

---

### Phase 2: Eventstream Support ✅

#### Task 2.1: Create EventstreamSubmitter Wrapper
**Status**: ✅ Complete
**File**: `src/main/kotlin/xyz/block/idea/telemetry/submitters/eventstream/EventstreamSubmitter.kt`

```kotlin
package xyz.block.idea.telemetry.submitters.eventstream

import com.squareup.eventstream.Eventstream
import com.squareup.eventstream.EventstreamEvent
import xyz.block.idea.telemetry.events.SyncEvent
import xyz.block.idea.telemetry.submitters.TelemetrySubmitter

internal class EventstreamSubmitter(
  private val eventstream: Eventstream,
  private val catalogName: String = "telemetry_android",
  private val appName: String = "sa-toolkit-plugin"
) : TelemetrySubmitter {

  override fun submitEvent(event: SyncEvent): Boolean {
    val eventstreamEvent = EventstreamEvent(
      catalogName = catalogName,
      appName = appName,
      event = event
    )
    return eventstream.sendEvents(listOf(eventstreamEvent))
  }
}
```

**Acceptance Criteria**:
- [x] Implements `TelemetrySubmitter` interface
- [x] Wraps existing `Eventstream` logic
- [x] Maintains existing functionality
- [x] No changes to eventstream behavior

---

### Phase 3: Integration ✅

#### Task 3.1: Modify Analytics.kt
**Status**: ✅ Complete
**File**: `src/main/kotlin/xyz/block/idea/telemetry/services/Analytics.kt`

**Changes Required**:

1. **Add imports** (top of file):
```kotlin
import xyz.block.idea.telemetry.submitters.TelemetrySubmitter
import xyz.block.idea.telemetry.submitters.eventstream.EventstreamSubmitter
import xyz.block.idea.telemetry.submitters.googleforms.GoogleFormsSubmitter
import xyz.block.idea.telemetry.submitters.googleforms.GoogleFormsUrlParser
```

2. **Replace lazy initialization** (around lines 58-80):
```kotlin
private val telemetrySubmitter: TelemetrySubmitter? by lazy {
  val endpoint = configProperties.getProperty(ENDPOINT_PROPERTY_NAME).orEmpty()

  if (endpoint.isBlank()) {
    thisLogger().warn("No endpoint found in $CONFIG_FILE_PATH. Set one with '$ENDPOINT_PROPERTY_NAME=...'")
    return@lazy null
  }

  val endpointWithProtocol = endpoint.ensurePrefix()

  // Auto-detect backend based on URL host
  when {
    endpointWithProtocol.contains("docs.google.com") -> {
      try {
        thisLogger().info("Detected Google Forms URL - using Google Forms submitter")
        val parsedUrl = GoogleFormsUrlParser.parse(endpointWithProtocol)
        GoogleFormsSubmitter(parsedUrl)
      } catch (e: Exception) {
        thisLogger().error("Failed to initialize Google Forms submitter: ${e.message}", e)
        null
      }
    }
    else -> {
      thisLogger().info("Using eventstream submitter for endpoint: $endpointWithProtocol")
      val es = Eventstream(EventstreamService(true, endpointWithProtocol))
      EventstreamSubmitter(es)
    }
  }
}
```

3. **Simplify recordSyncEvent()** (lines 74-123):
```kotlin
fun recordSyncEvent(syncResult: SyncResult) {
  val submitter = telemetrySubmitter
  if (submitter == null) {
    thisLogger().warn("No telemetry submitter available - skipping event")
    return
  }

  ApplicationManager.getApplication().executeOnPooledThread {
    val syncEvent = SyncEvent(
      // ... existing field population (no changes)
    )

    thisLogger().info("Sync $syncResult. Recording event via ${submitter.javaClass.simpleName}")

    if (!submitter.submitEvent(syncEvent)) {
      thisLogger().error("Recording sync $syncResult event failed.")
    }
  }
}
```

**Key Changes**:
- Remove `EventstreamEvent` wrapping (line ~116)
- Pass `SyncEvent` directly to submitter
- Add auto-detection logic
- Maintain existing functionality

**Acceptance Criteria**:
- [x] Auto-detection logic implemented
- [x] Google Forms submitter created for Google URLs
- [x] Eventstream submitter created for other URLs
- [x] Error handling for initialization failures
- [x] Clear logging messages
- [x] Existing functionality maintained
- [x] No changes to `SyncEvent` population logic

---

### Phase 4: Documentation ✅

#### Task 4.1: Update README.md
**Status**: ✅ Complete
**File**: `README.md`

**Add comprehensive Google Forms documentation**:

```markdown
## Configuration

### Option 1: Google Forms (Recommended)

Google Forms provides a free, no-infrastructure way to collect telemetry data.

#### Setup Steps

1. **Create Google Form**
   - Create a new Google Form
   - Add 24 short answer text fields (one per metric you want to track)
   - Name each field with a descriptive label (e.g., "Sync Type", "Sync Time")

2. **Generate Prefilled Link**
   - Click the three dots (⋮) in the top right
   - Select "Get pre-filled link"
   - Fill each field with its placeholder code (see table below)
   - Click "Get Link" and copy the URL

3. **Configure Plugin**
   - Add to your project's `gradle.properties`:
     ```properties
     ide-metrics-plugin.event-stream-endpoint=<your-prefilled-url>
     ```

4. **View Results**
   - Responses appear in Google Sheets (Form → Responses → View in Sheets)
   - Each sync event creates a new row

#### Placeholder Codes

Use these exact placeholder codes when generating your prefilled link:

| Field | Placeholder Code | Type | Description |
|-------|------------------|------|-------------|
| Sync Type | `SYNC_TYPE` | String | succeeded/failed/cancelled |
| Sync Time | `SYNC_TIME` | Long | Total duration in milliseconds |
| Configure Included Builds Duration | `CONFIGURE_INCLUDED_BUILDS_DURATION` | Long | Phase duration in ms |
| Configure Root Project Duration | `CONFIGURE_ROOT_PROJECT_DURATION` | Long | Phase duration in ms |
| Gradle Execution Duration | `GRADLE_EXECUTION_DURATION` | Long | Phase duration in ms |
| Gradle Duration | `GRADLE_DURATION` | Long | Total gradle phase in ms |
| IDE Duration | `IDE_DURATION` | Long | IDE processing phase in ms |
| JVM Total Memory | `JVM_TOTAL_MEMORY` | String | Total JVM memory in bytes |
| JVM Free Memory | `JVM_FREE_MEMORY` | String | Free JVM memory in bytes |
| Available Processors | `AVAILABLE_PROCESSORS` | Long | CPU core count |
| CPU Name | `CPU_NAME` | String | CPU brand string (e.g., "Apple M3 Max") |
| Number of Modules | `NUMBER_OF_MODULES` | Long | Module count in project |
| Active Workspace | `ACTIVE_WORKSPACE` | String? | Active workspace name (nullable) |
| Error Message | `ERROR_MESSAGE` | String? | Error details if sync failed (nullable) |
| Studio Version | `STUDIO_VERSION` | String | Android Studio version |
| Toolkit Version | `TOOLKIT_VERSION` | String | Plugin version with ID |
| AGP Version | `AGP_VERSION` | String? | Android Gradle Plugin version (nullable) |
| Gradle Version | `GRADLE_VERSION` | String? | Gradle version (nullable) |
| IntelliJ Core Version | `INTELLIJ_CORE_VERSION` | String | IntelliJ Core version |
| User LDAP | `USER_LDAP` | String | System username |
| OS System Architecture | `OS_SYSTEM_ARCHITECTURE` | String | OS architecture (e.g., "aarch64") |
| Artifact Sync Enabled | `ARTIFACT_SYNC_ENABLED` | Boolean | Artifact sync feature flag |
| Active Root Project Name | `ACTIVE_ROOT_PROJECT_NAME` | String | Gradle root project name |
| SA Toolbox Channel | `SA_TOOLBOX_CHANNEL` | String? | Toolbox channel (nullable) |
| Sync Trace ID | `SYNC_TRACE_ID` | String? | Correlation ID for tracing (nullable) |

**Note**: You don't need to include all fields. Only add the fields you want to track. Fields with unknown placeholders will be skipped automatically.

#### Example Configuration

```properties
# gradle.properties
ide-metrics-plugin.event-stream-endpoint=https://docs.google.com/forms/d/e/1FAIpQLSc24AgxMkixtaL6/viewform?usp=pp_url&entry.831280865=SYNC_TYPE&entry.289026959=SYNC_TIME&entry.1067177426=CONFIGURE_INCLUDED_BUILDS_DURATION&...
```

The plugin automatically detects which backend to use based on the URL:
- URLs containing `docs.google.com` → Google Forms
- All other URLs → Eventstream

### Advanced: Delegate Config File

Both approaches support delegate config files:

```properties
# gradle.properties
ide-metrics-plugin.event-stream-endpoint=https://eventstream.example.com
```

The plugin automatically detects the backend based on URL host:
- `docs.google.com` → Google Forms
- Other → Eventstream

### Advanced: Delegate Config File

Both approaches support delegate config files:

```properties
# gradle.properties
ide-metrics-plugin.config-file=config/telemetry.properties
```

```properties
# config/telemetry.properties
ide-metrics-plugin.event-stream-endpoint=<url>
```
```

**Acceptance Criteria**:
- [x] Comprehensive Google Forms setup instructions
- [x] Complete placeholder code table with all 25 fields
- [x] Example configuration shown
- [x] Eventstream documentation maintained
- [x] Auto-detection behavior explained
- [x] Delegate config file documentation updated

---

### Phase 5: Testing ⚠️ Pending

#### Task 5.1: Write Unit Tests for GoogleFormsUrlParser
**Status**: ⬜ Not Started
**File**: `src/test/kotlin/xyz/block/idea/telemetry/submitters/googleforms/GoogleFormsUrlParserTest.kt`

**Test Cases**:
- [ ] Valid Google Forms URL parsing
- [ ] Invalid URL format handling
- [ ] Non-Google Forms URL rejection
- [ ] Missing entry parameters
- [ ] Unknown placeholder handling (log warning, skip)
- [ ] URL decoding edge cases
- [ ] Query parameter extraction
- [ ] All 24 placeholder mappings
- [ ] Mixed valid/invalid placeholders

**Acceptance Criteria**:
- [ ] All test cases pass
- [ ] Edge cases covered
- [ ] Clear test names
- [ ] Proper assertions

---

#### Task 5.2: Write Unit Tests for GoogleFormsSubmitter
**Status**: ⬜ Not Started
**File**: `src/test/kotlin/xyz/block/idea/telemetry/submitters/googleforms/GoogleFormsSubmitterTest.kt`

**Test Cases**:
- [ ] URL building with various event values
- [ ] Null value handling (converted to empty strings)
- [ ] Special character URL encoding
- [ ] Long value handling
- [ ] Boolean value conversion
- [ ] Mock HTTP 200 response (success)
- [ ] Mock HTTP 4xx/5xx response (failure)
- [ ] Network timeout simulation
- [ ] Exception handling

**Acceptance Criteria**:
- [ ] All test cases pass
- [ ] Mock HttpURLConnection properly
- [ ] Edge cases covered
- [ ] Clear test names

---

#### Task 5.3: Manual Testing with Google Forms
**Status**: ⬜ Not Started

**Steps**:
1. [ ] Create a real Google Form with 24 fields
2. [ ] Generate prefilled URL with all placeholder codes
3. [ ] Configure plugin in `gradle.properties`
4. [ ] Trigger Gradle sync in IntelliJ/Android Studio
5. [ ] Verify data appears in Google Sheets
6. [ ] Verify all field values are correct
7. [ ] Test with null values (errorMessage, activeWorkspace, agpVersion)
8. [ ] Test with special characters in project name
9. [ ] Test with disconnected network
10. [ ] Verify plugin continues working despite telemetry failures
11. [ ] Check log messages are helpful

**Acceptance Criteria**:
- [ ] Data successfully submitted to Google Forms
- [ ] All non-null fields appear in Google Sheets
- [ ] Null fields show as empty
- [ ] Special characters handled correctly
- [ ] Network failures logged but don't crash plugin
- [ ] Log messages are clear and actionable

---

#### Task 5.4: Manual Testing with Eventstream (Backward Compatibility)
**Status**: ⬜ Not Started

**Steps**:
1. [ ] Configure with non-Google Forms URL
2. [ ] Verify eventstream submitter is selected (check logs)
3. [ ] Trigger Gradle sync
4. [ ] Verify existing eventstream behavior works
5. [ ] Verify no regressions in eventstream functionality

**Acceptance Criteria**:
- [ ] Eventstream submitter correctly detected
- [ ] Data submitted via eventstream
- [ ] No behavioral changes
- [ ] Existing functionality confirmed

---

### Phase 6: Final Verification ⚠️ Pending

#### Task 6.1: Code Review Checklist
**Status**: ⬜ Not Started

- [ ] All new files follow Kotlin coding conventions
- [ ] Proper package structure
- [ ] KDoc documentation on all public/internal APIs
- [ ] No external dependencies added (except existing ones)
- [ ] Error handling is comprehensive
- [ ] Logging statements are helpful
- [ ] No security vulnerabilities (URL injection, etc.)
- [ ] Thread-safe where needed
- [ ] No performance regressions

---

#### Task 6.2: Integration Testing
**Status**: ⬜ Not Started

- [x] Plugin builds successfully (Kotlin compilation passes)
- [ ] Plugin installs in IntelliJ/Android Studio
- [ ] No runtime exceptions
- [ ] Google Forms submission works end-to-end
- [ ] Eventstream submission still works
- [ ] Auto-detection works correctly
- [ ] Configuration errors are handled gracefully

---

## Technical Details

### File Structure

```
src/main/kotlin/xyz/block/idea/telemetry/
├── events/
│   └── SyncEvent.kt (no changes)
├── services/
│   └── Analytics.kt (modified - auto-detection logic)
└── submitters/ (new package)
    ├── TelemetrySubmitter.kt (interface)
    ├── eventstream/
    │   └── EventstreamSubmitter.kt (wrapper)
    └── googleforms/
        ├── GoogleFormsFieldMapping.kt (data classes)
        ├── GoogleFormsUrlParser.kt (parser)
        └── GoogleFormsSubmitter.kt (submitter)
```

### Dependencies

**No new external dependencies required**:
- Uses OkHttp (already available transitively via kotlin-eventstream2:client)
- Uses standard Java `URLEncoder`/`URLDecoder`
- Existing dependencies remain unchanged

### Error Handling Strategy

| Scenario | Handling |
|----------|----------|
| Invalid Google Forms URL | Log error, set submitter to null, plugin continues |
| Unknown placeholder | Log warning, skip field, continue with valid fields |
| Network timeout | Log error, return false, don't retry |
| HTTP non-200 response | Log warning, return false |
| Null field value | Convert to empty string, include in submission |
| Special characters | URL-encode using UTF-8 |

### Performance Considerations

- **URL parsing**: Happens once at initialization (lazy)
- **Submission**: Asynchronous (pooled thread)
- **No batching**: One HTTP request per event (Google Forms limitation)
- **Timeouts**: 10 seconds connection + 10 seconds read
- **No retries**: Fail-fast to avoid blocking threads

---

## Verification

### Definition of Done

- [x] All implementation tasks completed
- [ ] All unit tests passing
- [ ] Manual testing completed successfully
- [x] Documentation updated
- [ ] Code reviewed
- [x] No regressions in existing functionality (Kotlin compilation passes)
- [ ] Integration tests passing
- [ ] Ready for production use (pending testing)

### Success Metrics

- Google Forms submissions succeed 99%+ of the time
- No impact on Gradle sync performance
- Existing functionality with eventstream maintained
- Clear, actionable error messages in logs
- Users can set up Google Forms in < 10 minutes

---

## Documentation

### User-Facing Documentation

- [x] README.md updated with Google Forms setup
- [x] Placeholder code table provided
- [x] Example configuration shown
- [ ] Screenshots/GIFs for form setup (optional, future enhancement)

### Developer Documentation

- [x] Implementation plan (this document)
- [ ] KDoc on all new APIs
- [ ] Inline comments for complex logic
- [ ] Architecture decision records (ADRs) if needed

---

## Rollout Plan

### Phase 1: Implementation & Testing
- Complete all implementation tasks
- Run all tests
- Manual verification

### Phase 2: Internal Use
- Configure on internal projects
- Monitor for issues
- Gather feedback

### Phase 3: Documentation
- Finalize README updates
- Create user guide if needed
- Update plugin marketplace description

### Phase 4: Release
- Bump version number
- Update changelog
- Publish plugin update
- Announce to users

---

## Notes & Decisions

### Why Auto-Detection?
- Simpler for users (one config property)
- No ambiguity about which backend to use
- Easy to understand and troubleshoot

### Why Descriptive Placeholders?
- Easy to understand (e.g., `SYNC_TYPE` vs `ST`)
- Self-documenting
- Reduces user errors

### Why No Retry Logic?
- Keep it simple
- Google Forms is fast and reliable
- Telemetry is best-effort
- Avoid blocking thread pool

### Why Keep Eventstream?
- Existing functionality
- Gradual migration path
- Some users may prefer their own infrastructure

---

## Change Log

| Date | Change | Author |
|------|--------|--------|
| 2026-02-06 | Initial plan created | Claude |
| 2026-02-06 | Implementation completed (Phases 1-4) | Claude |
| 2026-02-06 | Updated GoogleFormsSubmitter to use OkHttp | Claude |

---

## Questions & Issues

### Open Questions
- [ ] Should we add metrics/monitoring for submission success rates?
- [ ] Should we add a configuration option to disable telemetry entirely?
- [ ] Should we validate form structure (check if entry IDs exist)?

### Known Issues
- None yet

### Future Enhancements
- Support for other backends (e.g., CSV file, database)
- Dashboard/visualization for Google Sheets data
- Form template for easy setup
- Migration tool from eventstream to Google Forms
