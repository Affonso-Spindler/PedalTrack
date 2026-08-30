# PedalTrack v1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the v1 Android app that lets the user log indoor cycling sessions after the fact — picking a synced Health Connect exercise session, entering km (and optional carga), saving it locally and to Health Connect, and reviewing history/summary stats.

**Architecture:** Kotlin + Jetpack Compose, MVVM (ViewModel + Repository). A `CyclingRepository` sits between a Room database (source of truth) and a `HealthConnectManager` wrapper (reads recent stationary-bike sessions, writes `DistanceRecord`). Three screens — Lançar, Histórico, Resumo — share one repository instance, wired through a bottom-nav `NavHost`.

**Tech Stack:** Kotlin 2.0.21, Android Gradle Plugin 8.5.2, Jetpack Compose (BOM 2024.06.00), Material3, Navigation Compose 2.7.7, Room 2.6.1 (KSP), Health Connect Client 1.1.0-alpha07, kotlinx-coroutines 1.8.1, JUnit4 + kotlinx-coroutines-test for unit tests.

**Spec:** [docs/superpowers/specs/2026-08-30-pedaltrack-design.md](../specs/2026-08-30-pedaltrack-design.md)

## Global Constraints

- Package/namespace/applicationId: `com.affonso.pedaltrack`.
- `minSdk = 26`, `compileSdk = 34`, `targetSdk = 34` (Health Connect Client requires minSdk 26).
- Scope v1: indoor cycling only — no other activity types.
- Room is the source of truth for the app's own screens; a failed Health Connect write never blocks a local save (spec: "Fonte de verdade dupla, com prioridade local").
- Every commit message follows Conventional Commits: `tipo(escopo): descrição`.
- No emulator/Android Studio is available in the environment executing this plan — Room `androidTest` instrumented tests and full manual Health Connect verification are written but can only be *run* on a real device (the user's Galaxy S25+) or an emulator with Health Connect + Google Play services. Every other step (Gradle build, JVM unit tests) must actually be executed and pass before committing.

---

### Task 1: Environment setup + project scaffolding

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts` (root)
- Create: `gradle.properties`
- Create: `.gitignore`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/affonso/pedaltrack/MainActivity.kt`
- Create: `app/src/main/java/com/affonso/pedaltrack/ui/theme/Theme.kt`

**Interfaces:**
- Produces: a buildable Android app module (`:app`) with Compose, Room (KSP), Health Connect Client, Navigation Compose, and test dependencies already declared, so every later task only adds files.

- [ ] **Step 1: Install JDK 17**

```bash
winget install --id EclipseAdoptium.Temurin.17.JDK -e --silent --accept-package-agreements --accept-source-agreements
```

- [ ] **Step 2: Install the Android SDK command-line tools**

```bash
mkdir -p "$LOCALAPPDATA/Android/Sdk/cmdline-tools"
curl -L -o "$TEMP/cmdline-tools.zip" "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"
unzip -q "$TEMP/cmdline-tools.zip" -d "$LOCALAPPDATA/Android/Sdk/cmdline-tools"
mv "$LOCALAPPDATA/Android/Sdk/cmdline-tools/cmdline-tools" "$LOCALAPPDATA/Android/Sdk/cmdline-tools/latest"
```

- [ ] **Step 3: Export env vars for this session and install SDK packages**

```bash
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.20.101-hotspot"
export ANDROID_HOME="$LOCALAPPDATA/Android/Sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

If `JAVA_HOME` above doesn't match the installed version, list `/c/Program Files/Eclipse Adoptium/` and use the actual `jdk-17.*-hotspot` folder name.

- [ ] **Step 4: Persist the env vars for future sessions**

```bash
setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
setx ANDROID_HOME "C:\Users\affon\AppData\Local\Android\Sdk"
```

- [ ] **Step 5: Create `settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "PedalTrack"
include(":app")
```

- [ ] **Step 6: Create the root `build.gradle.kts`**

```kotlin
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
}
```

- [ ] **Step 7: Create `gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx2048m
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

- [ ] **Step 8: Create `.gitignore`**

```
*.iml
.gradle/
/local.properties
.idea/
.DS_Store
build/
captures/
.cxx/
```

- [ ] **Step 9: Create `app/build.gradle.kts`**

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.affonso.pedaltrack"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.affonso.pedaltrack"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.health.connect:connect-client:1.1.0-alpha07")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
```

- [ ] **Step 10: Create `app/src/main/AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/auto">

    <uses-permission android:name="android.permission.health.READ_EXERCISE" />
    <uses-permission android:name="android.permission.health.WRITE_DISTANCE" />
    <uses-permission android:name="android.permission.health.READ_HEART_RATE" />
    <uses-permission android:name="android.permission.health.READ_TOTAL_CALORIES_BURNED" />

    <application
        android:allowBackup="true"
        android:label="PedalTrack">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <activity-alias
            android:name="ViewPermissionUsageActivity"
            android:exported="true"
            android:targetActivity=".MainActivity"
            android:permission="android.permission.START_VIEW_PERMISSION_USAGE">
            <intent-filter>
                <action android:name="android.intent.action.VIEW_PERMISSION_USAGE" />
                <category android:name="android.intent.category.HEALTH_PERMISSIONS" />
            </intent-filter>
        </activity-alias>
    </application>
</manifest>
```

- [ ] **Step 11: Create `app/src/main/java/com/affonso/pedaltrack/ui/theme/Theme.kt`**

```kotlin
package com.affonso.pedaltrack.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun PedalTrackTheme(content: @Composable () -> Unit) {
    val colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme = colorScheme, content = content)
}
```

- [ ] **Step 12: Create `app/src/main/java/com/affonso/pedaltrack/MainActivity.kt`** (placeholder content — replaced in Task 9)

```kotlin
package com.affonso.pedaltrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.affonso.pedaltrack.ui.theme.PedalTrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PedalTrackTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Text("PedalTrack")
                }
            }
        }
    }
}
```

- [ ] **Step 13: Generate the Gradle wrapper**

```bash
curl -L -o "$TEMP/gradle-bin.zip" "https://services.gradle.org/distributions/gradle-8.9-bin.zip"
unzip -q "$TEMP/gradle-bin.zip" -d "$TEMP/gradle-dist"
"$TEMP/gradle-dist/gradle-8.9/bin/gradle" wrapper --gradle-version 8.9 --distribution-type bin
```

- [ ] **Step 14: Verify the project builds**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 15: Commit**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties .gitignore app/ gradle/ gradlew gradlew.bat
git commit -m "chore(setup): scaffold Android project with Compose, Room and Health Connect deps"
```

---

### Task 2: Domain logic — session filtering and summary calculation

**Files:**
- Create: `app/src/main/java/com/affonso/pedaltrack/domain/Models.kt`
- Create: `app/src/main/java/com/affonso/pedaltrack/domain/SessionFilter.kt`
- Create: `app/src/main/java/com/affonso/pedaltrack/domain/SummaryCalculator.kt`
- Test: `app/src/test/java/com/affonso/pedaltrack/domain/SessionFilterTest.kt`
- Test: `app/src/test/java/com/affonso/pedaltrack/domain/SummaryCalculatorTest.kt`

**Interfaces:**
- Produces: `HealthConnectSession`, `CyclingSessionRecord`, `SummaryMetrics`, `SummaryPeriod` data types; `SessionFilter.loggable(...)`; `SummaryCalculator.calculate(...)` and `SummaryCalculator.filterByPeriod(...)` — used by every later task.

- [ ] **Step 1: Create the domain models**

`app/src/main/java/com/affonso/pedaltrack/domain/Models.kt`:

```kotlin
package com.affonso.pedaltrack.domain

import java.time.Instant

data class HealthConnectSession(
    val healthConnectSessionId: String,
    val startTime: Instant,
    val endTime: Instant,
    val durationMin: Int,
    val calories: Double?,
    val avgHeartRate: Int?
)

data class CyclingSessionRecord(
    val id: Long,
    val healthConnectSessionId: String,
    val startTime: Instant,
    val endTime: Instant,
    val durationMin: Int,
    val calories: Double?,
    val avgHeartRate: Int?,
    val km: Double,
    val carga: String?,
    val createdAt: Instant
)

data class SummaryMetrics(
    val totalKm: Double,
    val avgKmPerSession: Double,
    val totalCalories: Double,
    val avgDurationMin: Double,
    val sessionCount: Int
)

enum class SummaryPeriod { WEEK, MONTH, ALL }
```

- [ ] **Step 2: Write the failing test for `SessionFilter`**

`app/src/test/java/com/affonso/pedaltrack/domain/SessionFilterTest.kt`:

```kotlin
package com.affonso.pedaltrack.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class SessionFilterTest {

    private fun session(id: String, start: Instant) = HealthConnectSession(
        healthConnectSessionId = id,
        startTime = start,
        endTime = start.plusSeconds(1800),
        durationMin = 30,
        calories = 250.0,
        avgHeartRate = 130
    )

    @Test
    fun `excludes sessions already logged`() {
        val sessions = listOf(
            session("hc-1", Instant.parse("2026-08-01T10:00:00Z")),
            session("hc-2", Instant.parse("2026-08-02T10:00:00Z"))
        )

        val result = SessionFilter.loggable(sessions, loggedIds = setOf("hc-1"))

        assertEquals(listOf("hc-2"), result.map { it.healthConnectSessionId })
    }

    @Test
    fun `sorts remaining sessions from most recent to oldest`() {
        val sessions = listOf(
            session("hc-1", Instant.parse("2026-08-01T10:00:00Z")),
            session("hc-2", Instant.parse("2026-08-03T10:00:00Z")),
            session("hc-3", Instant.parse("2026-08-02T10:00:00Z"))
        )

        val result = SessionFilter.loggable(sessions, loggedIds = emptySet())

        assertEquals(listOf("hc-2", "hc-3", "hc-1"), result.map { it.healthConnectSessionId })
    }
}
```

- [ ] **Step 3: Run the test and confirm it fails to compile (no `SessionFilter` yet)**

Run: `./gradlew testDebugUnitTest --tests "com.affonso.pedaltrack.domain.SessionFilterTest"`
Expected: FAIL — `unresolved reference: SessionFilter`

- [ ] **Step 4: Implement `SessionFilter`**

`app/src/main/java/com/affonso/pedaltrack/domain/SessionFilter.kt`:

```kotlin
package com.affonso.pedaltrack.domain

object SessionFilter {
    fun loggable(
        healthConnectSessions: List<HealthConnectSession>,
        loggedIds: Set<String>
    ): List<HealthConnectSession> =
        healthConnectSessions
            .filterNot { it.healthConnectSessionId in loggedIds }
            .sortedByDescending { it.startTime }
}
```

- [ ] **Step 5: Run the test again and confirm it passes**

Run: `./gradlew testDebugUnitTest --tests "com.affonso.pedaltrack.domain.SessionFilterTest"`
Expected: `BUILD SUCCESSFUL`, 2 tests passed

- [ ] **Step 6: Write the failing test for `SummaryCalculator`**

`app/src/test/java/com/affonso/pedaltrack/domain/SummaryCalculatorTest.kt`:

```kotlin
package com.affonso.pedaltrack.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class SummaryCalculatorTest {

    private fun record(id: Long, start: Instant, km: Double, calories: Double?, durationMin: Int) = CyclingSessionRecord(
        id = id,
        healthConnectSessionId = "hc-$id",
        startTime = start,
        endTime = start.plusSeconds(durationMin * 60L),
        durationMin = durationMin,
        calories = calories,
        avgHeartRate = 130,
        km = km,
        carga = null,
        createdAt = start
    )

    @Test
    fun `calculate returns zeroed metrics for an empty list`() {
        val result = SummaryCalculator.calculate(emptyList())

        assertEquals(SummaryMetrics(0.0, 0.0, 0.0, 0.0, 0), result)
    }

    @Test
    fun `calculate averages and totals across sessions`() {
        val sessions = listOf(
            record(1, Instant.parse("2026-08-01T10:00:00Z"), km = 10.0, calories = 200.0, durationMin = 30),
            record(2, Instant.parse("2026-08-02T10:00:00Z"), km = 20.0, calories = 300.0, durationMin = 60)
        )

        val result = SummaryCalculator.calculate(sessions)

        assertEquals(30.0, result.totalKm, 0.001)
        assertEquals(15.0, result.avgKmPerSession, 0.001)
        assertEquals(500.0, result.totalCalories, 0.001)
        assertEquals(45.0, result.avgDurationMin, 0.001)
        assertEquals(2, result.sessionCount)
    }

    @Test
    fun `calculate treats missing calories as zero`() {
        val sessions = listOf(record(1, Instant.parse("2026-08-01T10:00:00Z"), km = 10.0, calories = null, durationMin = 30))

        val result = SummaryCalculator.calculate(sessions)

        assertEquals(0.0, result.totalCalories, 0.001)
    }

    @Test
    fun `filterByPeriod keeps only sessions within the window`() {
        val now = Instant.parse("2026-08-30T00:00:00Z")
        val sessions = listOf(
            record(1, now.minusSeconds(3 * 86400), km = 10.0, calories = 100.0, durationMin = 30),
            record(2, now.minusSeconds(20 * 86400), km = 10.0, calories = 100.0, durationMin = 30),
            record(3, now.minusSeconds(40 * 86400), km = 10.0, calories = 100.0, durationMin = 30)
        )

        assertEquals(listOf(1L), SummaryCalculator.filterByPeriod(sessions, SummaryPeriod.WEEK, now).map { it.id })
        assertEquals(listOf(1L, 2L), SummaryCalculator.filterByPeriod(sessions, SummaryPeriod.MONTH, now).map { it.id })
        assertEquals(listOf(1L, 2L, 3L), SummaryCalculator.filterByPeriod(sessions, SummaryPeriod.ALL, now).map { it.id })
    }
}
```

- [ ] **Step 7: Run the test and confirm it fails to compile**

Run: `./gradlew testDebugUnitTest --tests "com.affonso.pedaltrack.domain.SummaryCalculatorTest"`
Expected: FAIL — `unresolved reference: SummaryCalculator`

- [ ] **Step 8: Implement `SummaryCalculator`**

`app/src/main/java/com/affonso/pedaltrack/domain/SummaryCalculator.kt`:

```kotlin
package com.affonso.pedaltrack.domain

import java.time.Instant
import java.time.temporal.ChronoUnit

object SummaryCalculator {

    fun calculate(sessions: List<CyclingSessionRecord>): SummaryMetrics {
        if (sessions.isEmpty()) return SummaryMetrics(0.0, 0.0, 0.0, 0.0, 0)
        val totalKm = sessions.sumOf { it.km }
        val totalCalories = sessions.sumOf { it.calories ?: 0.0 }
        val avgDuration = sessions.map { it.durationMin }.average()
        return SummaryMetrics(
            totalKm = totalKm,
            avgKmPerSession = totalKm / sessions.size,
            totalCalories = totalCalories,
            avgDurationMin = avgDuration,
            sessionCount = sessions.size
        )
    }

    fun filterByPeriod(
        sessions: List<CyclingSessionRecord>,
        period: SummaryPeriod,
        now: Instant
    ): List<CyclingSessionRecord> = when (period) {
        SummaryPeriod.ALL -> sessions
        SummaryPeriod.WEEK -> sessions.filter { it.startTime >= now.minus(7, ChronoUnit.DAYS) }
        SummaryPeriod.MONTH -> sessions.filter { it.startTime >= now.minus(30, ChronoUnit.DAYS) }
    }
}
```

- [ ] **Step 9: Run the test again and confirm it passes**

Run: `./gradlew testDebugUnitTest --tests "com.affonso.pedaltrack.domain.SummaryCalculatorTest"`
Expected: `BUILD SUCCESSFUL`, 4 tests passed

- [ ] **Step 10: Commit**

```bash
git add app/src/main/java/com/affonso/pedaltrack/domain/ app/src/test/java/com/affonso/pedaltrack/domain/
git commit -m "feat(domain): add session filtering and summary calculation"
```

---

### Task 3: Room persistence layer

**Files:**
- Create: `app/src/main/java/com/affonso/pedaltrack/data/local/CyclingSessionEntity.kt`
- Create: `app/src/main/java/com/affonso/pedaltrack/data/local/InstantConverter.kt`
- Create: `app/src/main/java/com/affonso/pedaltrack/data/local/CyclingSessionDao.kt`
- Create: `app/src/main/java/com/affonso/pedaltrack/data/local/PedalTrackDatabase.kt`
- Test: `app/src/androidTest/java/com/affonso/pedaltrack/data/local/CyclingSessionDaoTest.kt`

**Interfaces:**
- Consumes: nothing from earlier tasks (entity mirrors the spec's data model independently).
- Produces: `CyclingSessionEntity`, `CyclingSessionDao` (`insert`, `update`, `deleteById`, `observeAll`, `getAll`, `getAllHealthConnectIds`, `getById`), `PedalTrackDatabase.getInstance(context)` — consumed by the repository in Task 5.

- [ ] **Step 1: Create the entity**

`app/src/main/java/com/affonso/pedaltrack/data/local/CyclingSessionEntity.kt`:

```kotlin
package com.affonso.pedaltrack.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "cycling_sessions",
    indices = [Index(value = ["healthConnectSessionId"], unique = true)]
)
data class CyclingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val healthConnectSessionId: String,
    val startTime: Instant,
    val endTime: Instant,
    val durationMin: Int,
    val calories: Double?,
    val avgHeartRate: Int?,
    val km: Double,
    val carga: String?,
    val createdAt: Instant
)
```

- [ ] **Step 2: Create the `Instant` type converter**

`app/src/main/java/com/affonso/pedaltrack/data/local/InstantConverter.kt`:

```kotlin
package com.affonso.pedaltrack.data.local

import androidx.room.TypeConverter
import java.time.Instant

class InstantConverter {
    @TypeConverter
    fun fromEpochMilli(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun toEpochMilli(instant: Instant?): Long? = instant?.toEpochMilli()
}
```

- [ ] **Step 3: Create the DAO**

`app/src/main/java/com/affonso/pedaltrack/data/local/CyclingSessionDao.kt`:

```kotlin
package com.affonso.pedaltrack.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CyclingSessionDao {
    @Insert
    suspend fun insert(session: CyclingSessionEntity): Long

    @Update
    suspend fun update(session: CyclingSessionEntity)

    @Query("DELETE FROM cycling_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM cycling_sessions ORDER BY startTime DESC")
    fun observeAll(): Flow<List<CyclingSessionEntity>>

    @Query("SELECT * FROM cycling_sessions ORDER BY startTime DESC")
    suspend fun getAll(): List<CyclingSessionEntity>

    @Query("SELECT healthConnectSessionId FROM cycling_sessions")
    suspend fun getAllHealthConnectIds(): List<String>

    @Query("SELECT * FROM cycling_sessions WHERE id = :id")
    suspend fun getById(id: Long): CyclingSessionEntity?
}
```

- [ ] **Step 4: Create the database**

`app/src/main/java/com/affonso/pedaltrack/data/local/PedalTrackDatabase.kt`:

```kotlin
package com.affonso.pedaltrack.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [CyclingSessionEntity::class], version = 1, exportSchema = false)
@TypeConverters(InstantConverter::class)
abstract class PedalTrackDatabase : RoomDatabase() {
    abstract fun cyclingSessionDao(): CyclingSessionDao

    companion object {
        @Volatile private var INSTANCE: PedalTrackDatabase? = null

        fun getInstance(context: Context): PedalTrackDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PedalTrackDatabase::class.java,
                    "pedaltrack.db"
                ).build().also { INSTANCE = it }
            }
    }
}
```

- [ ] **Step 5: Write the instrumented DAO test**

`app/src/androidTest/java/com/affonso/pedaltrack/data/local/CyclingSessionDaoTest.kt`:

```kotlin
package com.affonso.pedaltrack.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class CyclingSessionDaoTest {
    private lateinit var db: PedalTrackDatabase
    private lateinit var dao: CyclingSessionDao

    private fun entity(hcId: String, km: Double = 10.0) = CyclingSessionEntity(
        healthConnectSessionId = hcId,
        startTime = Instant.parse("2026-08-01T10:00:00Z"),
        endTime = Instant.parse("2026-08-01T10:30:00Z"),
        durationMin = 30,
        calories = 250.0,
        avgHeartRate = 130,
        km = km,
        carga = "media",
        createdAt = Instant.parse("2026-08-01T10:31:00Z")
    )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PedalTrackDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.cyclingSessionDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndRetrieveSession() = runBlocking {
        dao.insert(entity("hc-1"))

        val all = dao.getAll()
        assertEquals(1, all.size)
        assertEquals("hc-1", all[0].healthConnectSessionId)
    }

    @Test
    fun duplicateHealthConnectIdThrows() = runBlocking {
        dao.insert(entity("hc-1"))
        var threw = false
        try {
            dao.insert(entity("hc-1", km = 20.0))
        } catch (e: Exception) {
            threw = true
        }
        assertTrue(threw)
    }

    @Test
    fun getAllHealthConnectIdsReturnsLoggedIds() = runBlocking {
        dao.insert(entity("hc-1"))
        dao.insert(entity("hc-2"))

        assertEquals(setOf("hc-1", "hc-2"), dao.getAllHealthConnectIds().toSet())
    }

    @Test
    fun deleteByIdRemovesSession() = runBlocking {
        val id = dao.insert(entity("hc-1"))
        dao.deleteById(id)
        assertNull(dao.getById(id))
    }
}
```

- [ ] **Step 6: Verify the module compiles**

Run: `./gradlew compileDebugKotlin compileDebugAndroidTestKotlin`
Expected: `BUILD SUCCESSFUL`

This test requires a connected device or emulator to actually run — not available in this environment. Run it later with `./gradlew connectedAndroidTest --tests "com.affonso.pedaltrack.data.local.CyclingSessionDaoTest"` after connecting the S25+ over USB with USB debugging enabled (`adb devices` should list it first).

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/affonso/pedaltrack/data/local/ app/src/androidTest/
git commit -m "feat(data): add Room persistence for cycling sessions"
```

---

### Task 4: Health Connect integration

**Files:**
- Create: `app/src/main/java/com/affonso/pedaltrack/data/healthconnect/HealthConnectManager.kt`

**Interfaces:**
- Consumes: `HealthConnectSession` (Task 2).
- Produces: `HealthConnectManager` interface (`permissions()`, `hasAllPermissions()`, `readRecentStationaryBikeSessions(since)`, `writeDistanceRecord(startTime, endTime, km)`) and `HealthConnectManagerImpl` — consumed by the repository in Task 5 and by `MainActivity` in Task 9.

- [ ] **Step 1: Implement the Health Connect manager**

`app/src/main/java/com/affonso/pedaltrack/data/healthconnect/HealthConnectManager.kt`:

```kotlin
package com.affonso.pedaltrack.data.healthconnect

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Length
import com.affonso.pedaltrack.domain.HealthConnectSession
import java.time.Instant
import java.time.temporal.ChronoUnit

interface HealthConnectManager {
    fun permissions(): Set<String>
    suspend fun hasAllPermissions(): Boolean
    suspend fun readRecentStationaryBikeSessions(since: Instant): List<HealthConnectSession>
    suspend fun writeDistanceRecord(startTime: Instant, endTime: Instant, km: Double): Result<Unit>
}

class HealthConnectManagerImpl(private val client: HealthConnectClient) : HealthConnectManager {

    override fun permissions(): Set<String> = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(DistanceRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
    )

    override suspend fun hasAllPermissions(): Boolean =
        client.permissionController.getGrantedPermissions().containsAll(permissions())

    override suspend fun readRecentStationaryBikeSessions(since: Instant): List<HealthConnectSession> {
        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.after(since)
            )
        )
        return response.records
            .filter { it.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_STATIONARY_BIKE }
            .map { toHealthConnectSession(it) }
    }

    private suspend fun toHealthConnectSession(record: ExerciseSessionRecord): HealthConnectSession {
        val aggregate = client.aggregate(
            AggregateRequest(
                metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL, HeartRateRecord.BPM_AVG),
                timeRangeFilter = TimeRangeFilter.between(record.startTime, record.endTime)
            )
        )
        return HealthConnectSession(
            healthConnectSessionId = record.metadata.id,
            startTime = record.startTime,
            endTime = record.endTime,
            durationMin = ChronoUnit.MINUTES.between(record.startTime, record.endTime).toInt(),
            calories = aggregate[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories,
            avgHeartRate = aggregate[HeartRateRecord.BPM_AVG]?.toInt()
        )
    }

    override suspend fun writeDistanceRecord(startTime: Instant, endTime: Instant, km: Double): Result<Unit> =
        try {
            client.insertRecords(
                listOf(
                    DistanceRecord(
                        startTime = startTime,
                        startZoneOffset = null,
                        endTime = endTime,
                        endZoneOffset = null,
                        distance = Length.kilometers(km),
                        metadata = Metadata.manualEntry()
                    )
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
}
```

- [ ] **Step 2: Verify the module compiles**

Run: `./gradlew compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

This class talks to the real Health Connect service, so it can't be meaningfully unit tested with fakes — it's exercised through the manual end-to-end check in Task 9 (real Watch session → app → Health Connect data screen) once you have the app on your S25+.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/affonso/pedaltrack/data/healthconnect/
git commit -m "feat(health-connect): add read/write integration for stationary bike sessions"
```

---

### Task 5: Cycling repository

**Files:**
- Create: `app/src/main/java/com/affonso/pedaltrack/repository/CyclingRepository.kt`
- Test: `app/src/test/java/com/affonso/pedaltrack/repository/CyclingRepositoryTest.kt`

**Interfaces:**
- Consumes: `CyclingSessionDao`, `CyclingSessionEntity` (Task 3); `HealthConnectManager`, `HealthConnectSession` (Task 4); `SessionFilter`, `SummaryCalculator`, `CyclingSessionRecord`, `SummaryMetrics`, `SummaryPeriod` (Task 2).
- Produces: `LogResult(healthConnectSynced: Boolean)`, `CyclingRepository` interface (`getLoggableSessions()`, `logSession(session, km, carga)`, `observeHistory()`, `updateSession(id, km, carga)`, `deleteSession(id)`, `getSummary(period)`) and `CyclingRepositoryImpl` — consumed by every ViewModel in Tasks 6-8 and by `MainActivity` in Task 9.

- [ ] **Step 1: Write the failing repository tests**

`app/src/test/java/com/affonso/pedaltrack/repository/CyclingRepositoryTest.kt`:

```kotlin
package com.affonso.pedaltrack.repository

import com.affonso.pedaltrack.data.healthconnect.HealthConnectManager
import com.affonso.pedaltrack.data.local.CyclingSessionDao
import com.affonso.pedaltrack.data.local.CyclingSessionEntity
import com.affonso.pedaltrack.domain.HealthConnectSession
import com.affonso.pedaltrack.domain.SummaryPeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

private class FakeDao : CyclingSessionDao {
    val sessions = mutableListOf<CyclingSessionEntity>()
    private var nextId = 1L
    private val flow = MutableStateFlow<List<CyclingSessionEntity>>(emptyList())

    override suspend fun insert(session: CyclingSessionEntity): Long {
        if (sessions.any { it.healthConnectSessionId == session.healthConnectSessionId }) {
            throw IllegalStateException("duplicate healthConnectSessionId")
        }
        val withId = session.copy(id = nextId++)
        sessions.add(withId)
        flow.value = sessions.toList()
        return withId.id
    }

    override suspend fun update(session: CyclingSessionEntity) {
        val index = sessions.indexOfFirst { it.id == session.id }
        sessions[index] = session
        flow.value = sessions.toList()
    }

    override suspend fun deleteById(id: Long) {
        sessions.removeAll { it.id == id }
        flow.value = sessions.toList()
    }

    override fun observeAll(): Flow<List<CyclingSessionEntity>> = flow
    override suspend fun getAll(): List<CyclingSessionEntity> = sessions.toList()
    override suspend fun getAllHealthConnectIds(): List<String> = sessions.map { it.healthConnectSessionId }
    override suspend fun getById(id: Long): CyclingSessionEntity? = sessions.find { it.id == id }
}

private class FakeHealthConnectManager(
    private val sessionsToReturn: List<HealthConnectSession> = emptyList(),
    private val writeShouldFail: Boolean = false
) : HealthConnectManager {
    override fun permissions(): Set<String> = emptySet()
    override suspend fun hasAllPermissions(): Boolean = true
    override suspend fun readRecentStationaryBikeSessions(since: Instant): List<HealthConnectSession> = sessionsToReturn
    override suspend fun writeDistanceRecord(startTime: Instant, endTime: Instant, km: Double): Result<Unit> =
        if (writeShouldFail) Result.failure(RuntimeException("sync failed")) else Result.success(Unit)
}

class CyclingRepositoryTest {

    private val sampleSession = HealthConnectSession(
        healthConnectSessionId = "hc-1",
        startTime = Instant.parse("2026-08-01T10:00:00Z"),
        endTime = Instant.parse("2026-08-01T10:30:00Z"),
        durationMin = 30,
        calories = 250.0,
        avgHeartRate = 130
    )

    @Test
    fun `getLoggableSessions excludes already logged sessions`() = runBlocking {
        val dao = FakeDao()
        dao.insert(
            CyclingSessionEntity(
                healthConnectSessionId = "hc-1",
                startTime = sampleSession.startTime,
                endTime = sampleSession.endTime,
                durationMin = 30,
                calories = 250.0,
                avgHeartRate = 130,
                km = 10.0,
                carga = null,
                createdAt = Instant.now()
            )
        )
        val repository = CyclingRepositoryImpl(dao, FakeHealthConnectManager(sessionsToReturn = listOf(sampleSession)))

        val result = repository.getLoggableSessions()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `logSession saves locally even when Health Connect write fails`() = runBlocking {
        val dao = FakeDao()
        val repository = CyclingRepositoryImpl(dao, FakeHealthConnectManager(writeShouldFail = true))

        val result = repository.logSession(sampleSession, km = 12.5, carga = "media")

        assertTrue(result.isSuccess)
        assertEquals(false, result.getOrNull()?.healthConnectSynced)
        assertEquals(1, dao.sessions.size)
        assertEquals(12.5, dao.sessions[0].km, 0.001)
    }

    @Test
    fun `getSummary calculates metrics for the selected period`() = runBlocking {
        val dao = FakeDao()
        val now = Instant.now()
        dao.insert(
            CyclingSessionEntity(
                healthConnectSessionId = "hc-1",
                startTime = now.minusSeconds(3600),
                endTime = now,
                durationMin = 60,
                calories = 400.0,
                avgHeartRate = 140,
                km = 20.0,
                carga = null,
                createdAt = now
            )
        )
        val repository = CyclingRepositoryImpl(dao, FakeHealthConnectManager())

        val summary = repository.getSummary(SummaryPeriod.ALL)

        assertEquals(20.0, summary.totalKm, 0.001)
        assertEquals(1, summary.sessionCount)
    }
}
```

- [ ] **Step 2: Run the tests and confirm they fail to compile**

Run: `./gradlew testDebugUnitTest --tests "com.affonso.pedaltrack.repository.CyclingRepositoryTest"`
Expected: FAIL — `unresolved reference: CyclingRepositoryImpl`

- [ ] **Step 3: Implement the repository**

`app/src/main/java/com/affonso/pedaltrack/repository/CyclingRepository.kt`:

```kotlin
package com.affonso.pedaltrack.repository

import com.affonso.pedaltrack.data.healthconnect.HealthConnectManager
import com.affonso.pedaltrack.data.local.CyclingSessionDao
import com.affonso.pedaltrack.data.local.CyclingSessionEntity
import com.affonso.pedaltrack.domain.CyclingSessionRecord
import com.affonso.pedaltrack.domain.HealthConnectSession
import com.affonso.pedaltrack.domain.SessionFilter
import com.affonso.pedaltrack.domain.SummaryCalculator
import com.affonso.pedaltrack.domain.SummaryMetrics
import com.affonso.pedaltrack.domain.SummaryPeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.temporal.ChronoUnit

data class LogResult(val healthConnectSynced: Boolean)

interface CyclingRepository {
    suspend fun getLoggableSessions(): List<HealthConnectSession>
    suspend fun logSession(session: HealthConnectSession, km: Double, carga: String?): Result<LogResult>
    fun observeHistory(): Flow<List<CyclingSessionRecord>>
    suspend fun updateSession(id: Long, km: Double, carga: String?)
    suspend fun deleteSession(id: Long)
    suspend fun getSummary(period: SummaryPeriod): SummaryMetrics
}

class CyclingRepositoryImpl(
    private val dao: CyclingSessionDao,
    private val healthConnectManager: HealthConnectManager
) : CyclingRepository {

    override suspend fun getLoggableSessions(): List<HealthConnectSession> {
        val since = Instant.now().minus(30, ChronoUnit.DAYS)
        val hcSessions = healthConnectManager.readRecentStationaryBikeSessions(since)
        val loggedIds = dao.getAllHealthConnectIds().toSet()
        return SessionFilter.loggable(hcSessions, loggedIds)
    }

    override suspend fun logSession(session: HealthConnectSession, km: Double, carga: String?): Result<LogResult> =
        try {
            dao.insert(
                CyclingSessionEntity(
                    healthConnectSessionId = session.healthConnectSessionId,
                    startTime = session.startTime,
                    endTime = session.endTime,
                    durationMin = session.durationMin,
                    calories = session.calories,
                    avgHeartRate = session.avgHeartRate,
                    km = km,
                    carga = carga,
                    createdAt = Instant.now()
                )
            )
            val syncResult = healthConnectManager.writeDistanceRecord(session.startTime, session.endTime, km)
            Result.success(LogResult(healthConnectSynced = syncResult.isSuccess))
        } catch (e: Exception) {
            Result.failure(e)
        }

    override fun observeHistory(): Flow<List<CyclingSessionRecord>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun updateSession(id: Long, km: Double, carga: String?) {
        val existing = dao.getById(id) ?: return
        dao.update(existing.copy(km = km, carga = carga))
    }

    override suspend fun deleteSession(id: Long) = dao.deleteById(id)

    override suspend fun getSummary(period: SummaryPeriod): SummaryMetrics {
        val all = dao.getAll().map { it.toDomain() }
        val filtered = SummaryCalculator.filterByPeriod(all, period, Instant.now())
        return SummaryCalculator.calculate(filtered)
    }
}

private fun CyclingSessionEntity.toDomain() = CyclingSessionRecord(
    id = id,
    healthConnectSessionId = healthConnectSessionId,
    startTime = startTime,
    endTime = endTime,
    durationMin = durationMin,
    calories = calories,
    avgHeartRate = avgHeartRate,
    km = km,
    carga = carga,
    createdAt = createdAt
)
```

- [ ] **Step 4: Run the tests again and confirm they pass**

Run: `./gradlew testDebugUnitTest --tests "com.affonso.pedaltrack.repository.CyclingRepositoryTest"`
Expected: `BUILD SUCCESSFUL`, 3 tests passed

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/affonso/pedaltrack/repository/ app/src/test/java/com/affonso/pedaltrack/repository/
git commit -m "feat(repository): combine local storage and Health Connect data"
```

---

### Task 6: Log Session screen

**Files:**
- Create: `app/src/main/java/com/affonso/pedaltrack/ui/log/LogSessionViewModel.kt`
- Create: `app/src/main/java/com/affonso/pedaltrack/ui/log/LogSessionScreen.kt`
- Test: `app/src/test/java/com/affonso/pedaltrack/ui/log/LogSessionViewModelTest.kt`

**Interfaces:**
- Consumes: `CyclingRepository`, `LogResult` (Task 5); `HealthConnectSession` (Task 2).
- Produces: `LogSessionUiState`, `LogSessionViewModel` (`uiState`, `loadSessions()`, `submit(session, km, carga)`), `LogSessionScreen(uiState, onSubmit)` composable — consumed by navigation in Task 9.

- [ ] **Step 1: Write the failing ViewModel tests**

`app/src/test/java/com/affonso/pedaltrack/ui/log/LogSessionViewModelTest.kt`:

```kotlin
package com.affonso.pedaltrack.ui.log

import com.affonso.pedaltrack.domain.CyclingSessionRecord
import com.affonso.pedaltrack.domain.HealthConnectSession
import com.affonso.pedaltrack.domain.SummaryMetrics
import com.affonso.pedaltrack.domain.SummaryPeriod
import com.affonso.pedaltrack.repository.CyclingRepository
import com.affonso.pedaltrack.repository.LogResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

private class FakeCyclingRepository(
    private val loggableSessions: List<HealthConnectSession> = emptyList(),
    private val logResult: Result<LogResult> = Result.success(LogResult(true))
) : CyclingRepository {
    var loggedKm: Double? = null

    override suspend fun getLoggableSessions(): List<HealthConnectSession> = loggableSessions
    override suspend fun logSession(session: HealthConnectSession, km: Double, carga: String?): Result<LogResult> {
        loggedKm = km
        return logResult
    }
    override fun observeHistory(): Flow<List<CyclingSessionRecord>> = flowOf(emptyList())
    override suspend fun updateSession(id: Long, km: Double, carga: String?) {}
    override suspend fun deleteSession(id: Long) {}
    override suspend fun getSummary(period: SummaryPeriod): SummaryMetrics = SummaryMetrics(0.0, 0.0, 0.0, 0.0, 0)
}

@OptIn(ExperimentalCoroutinesApi::class)
class LogSessionViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(dispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private val sampleSession = HealthConnectSession(
        healthConnectSessionId = "hc-1",
        startTime = Instant.parse("2026-08-01T10:00:00Z"),
        endTime = Instant.parse("2026-08-01T10:30:00Z"),
        durationMin = 30,
        calories = 250.0,
        avgHeartRate = 130
    )

    @Test
    fun `loads loggable sessions on init`() = runTest {
        val viewModel = LogSessionViewModel(FakeCyclingRepository(loggableSessions = listOf(sampleSession)))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.loggableSessions.size)
        assertEquals(false, viewModel.uiState.value.loading)
    }

    @Test
    fun `submit shows warning when Health Connect sync fails`() = runTest {
        val repository = FakeCyclingRepository(
            loggableSessions = listOf(sampleSession),
            logResult = Result.success(LogResult(healthConnectSynced = false))
        )
        val viewModel = LogSessionViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.submit(sampleSession, 12.5, "media")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(12.5, repository.loggedKm)
        assertTrue(viewModel.uiState.value.syncWarning != null)
    }
}
```

- [ ] **Step 2: Run the tests and confirm they fail to compile**

Run: `./gradlew testDebugUnitTest --tests "com.affonso.pedaltrack.ui.log.LogSessionViewModelTest"`
Expected: FAIL — `unresolved reference: LogSessionViewModel`

- [ ] **Step 3: Implement the ViewModel**

`app/src/main/java/com/affonso/pedaltrack/ui/log/LogSessionViewModel.kt`:

```kotlin
package com.affonso.pedaltrack.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.affonso.pedaltrack.domain.HealthConnectSession
import com.affonso.pedaltrack.repository.CyclingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LogSessionUiState(
    val loading: Boolean = true,
    val loggableSessions: List<HealthConnectSession> = emptyList(),
    val error: String? = null,
    val syncWarning: String? = null
)

class LogSessionViewModel(private val repository: CyclingRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(LogSessionUiState())
    val uiState: StateFlow<LogSessionUiState> = _uiState.asStateFlow()

    init {
        loadSessions()
    }

    fun loadSessions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                val sessions = repository.getLoggableSessions()
                _uiState.value = _uiState.value.copy(loading = false, loggableSessions = sessions)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun submit(session: HealthConnectSession, km: Double, carga: String?) {
        viewModelScope.launch {
            repository.logSession(session, km, carga)
                .onSuccess { logResult ->
                    _uiState.value = _uiState.value.copy(
                        syncWarning = if (!logResult.healthConnectSynced)
                            "Salvo localmente, mas não sincronizou com o Health Connect"
                        else null
                    )
                    loadSessions()
                }
                .onFailure { e -> _uiState.value = _uiState.value.copy(error = e.message) }
        }
    }
}
```

- [ ] **Step 4: Run the tests again and confirm they pass**

Run: `./gradlew testDebugUnitTest --tests "com.affonso.pedaltrack.ui.log.LogSessionViewModelTest"`
Expected: `BUILD SUCCESSFUL`, 2 tests passed

- [ ] **Step 5: Implement the screen**

`app/src/main/java/com/affonso/pedaltrack/ui/log/LogSessionScreen.kt`:

```kotlin
package com.affonso.pedaltrack.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.affonso.pedaltrack.domain.HealthConnectSession
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun LogSessionScreen(
    uiState: LogSessionUiState,
    onSubmit: (HealthConnectSession, Double, String?) -> Unit
) {
    var selected by remember { mutableStateOf<HealthConnectSession?>(null) }

    if (uiState.loading) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) { CircularProgressIndicator() }
        return
    }

    val current = selected
    if (current != null) {
        LogSessionForm(
            session = current,
            onCancel = { selected = null },
            onConfirm = { km, carga ->
                onSubmit(current, km, carga)
                selected = null
            }
        )
        return
    }

    if (uiState.loggableSessions.isEmpty()) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) { Text("Nenhum treino novo nos últimos 30 dias") }
        return
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        items(uiState.loggableSessions) { session ->
            SessionCard(session = session, onClick = { selected = session })
        }
    }
}

@Composable
private fun SessionCard(session: HealthConnectSession, onClick: () -> Unit) {
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.systemDefault()) }
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), onClick = onClick) {
        Column(Modifier.padding(12.dp)) {
            Text(formatter.format(session.startTime))
            Text("${session.durationMin} min" + (session.calories?.let { " · ${it.toInt()} kcal" } ?: ""))
            session.avgHeartRate?.let { Text("FC média: $it bpm") }
        }
    }
}

@Composable
private fun LogSessionForm(
    session: HealthConnectSession,
    onCancel: () -> Unit,
    onConfirm: (Double, String?) -> Unit
) {
    var km by remember { mutableStateOf("") }
    var carga by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Sessão de ${session.durationMin} min")
        OutlinedTextField(
            value = km,
            onValueChange = { km = it },
            label = { Text("Km") },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )
        OutlinedTextField(
            value = carga,
            onValueChange = { carga = it },
            label = { Text("Carga (opcional)") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        Button(
            onClick = { km.toDoubleOrNull()?.let { onConfirm(it, carga.ifBlank { null }) } },
            modifier = Modifier.padding(top = 16.dp)
        ) { Text("Salvar") }
        Button(onClick = onCancel, modifier = Modifier.padding(top = 8.dp)) { Text("Cancelar") }
    }
}
```

- [ ] **Step 6: Verify the module compiles**

Run: `./gradlew compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/affonso/pedaltrack/ui/log/ app/src/test/java/com/affonso/pedaltrack/ui/log/
git commit -m "feat(ui): add log session screen"
```

---

### Task 7: History screen

**Files:**
- Create: `app/src/main/java/com/affonso/pedaltrack/ui/history/HistoryViewModel.kt`
- Create: `app/src/main/java/com/affonso/pedaltrack/ui/history/HistoryScreen.kt`
- Test: `app/src/test/java/com/affonso/pedaltrack/ui/history/HistoryViewModelTest.kt`

**Interfaces:**
- Consumes: `CyclingRepository` (Task 5); `CyclingSessionRecord` (Task 2).
- Produces: `HistoryUiState`, `HistoryViewModel` (`uiState`, `update(id, km, carga)`, `delete(id)`), `HistoryScreen(sessions, onUpdate, onDelete)` composable — consumed by navigation in Task 9.

- [ ] **Step 1: Write the failing ViewModel tests**

`app/src/test/java/com/affonso/pedaltrack/ui/history/HistoryViewModelTest.kt`:

```kotlin
package com.affonso.pedaltrack.ui.history

import com.affonso.pedaltrack.domain.CyclingSessionRecord
import com.affonso.pedaltrack.domain.HealthConnectSession
import com.affonso.pedaltrack.domain.SummaryMetrics
import com.affonso.pedaltrack.domain.SummaryPeriod
import com.affonso.pedaltrack.repository.CyclingRepository
import com.affonso.pedaltrack.repository.LogResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

private class FakeCyclingRepository(
    private val history: MutableStateFlow<List<CyclingSessionRecord>>
) : CyclingRepository {
    var deletedId: Long? = null
    var updatedKm: Double? = null

    override suspend fun getLoggableSessions(): List<HealthConnectSession> = emptyList()
    override suspend fun logSession(session: HealthConnectSession, km: Double, carga: String?): Result<LogResult> =
        Result.success(LogResult(true))
    override fun observeHistory(): Flow<List<CyclingSessionRecord>> = history
    override suspend fun updateSession(id: Long, km: Double, carga: String?) { updatedKm = km }
    override suspend fun deleteSession(id: Long) { deletedId = id }
    override suspend fun getSummary(period: SummaryPeriod): SummaryMetrics = SummaryMetrics(0.0, 0.0, 0.0, 0.0, 0)
}

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(dispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private val sampleRecord = CyclingSessionRecord(
        id = 1,
        healthConnectSessionId = "hc-1",
        startTime = Instant.parse("2026-08-01T10:00:00Z"),
        endTime = Instant.parse("2026-08-01T10:30:00Z"),
        durationMin = 30,
        calories = 250.0,
        avgHeartRate = 130,
        km = 12.5,
        carga = "media",
        createdAt = Instant.parse("2026-08-01T10:31:00Z")
    )

    @Test
    fun `exposes sessions from repository`() = runTest {
        val viewModel = HistoryViewModel(FakeCyclingRepository(MutableStateFlow(listOf(sampleRecord))))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.sessions.size)
    }

    @Test
    fun `delete forwards id to repository`() = runTest {
        val repository = FakeCyclingRepository(MutableStateFlow(listOf(sampleRecord)))
        val viewModel = HistoryViewModel(repository)

        viewModel.delete(1)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1L, repository.deletedId)
    }

    @Test
    fun `update forwards km to repository`() = runTest {
        val repository = FakeCyclingRepository(MutableStateFlow(listOf(sampleRecord)))
        val viewModel = HistoryViewModel(repository)

        viewModel.update(1, 15.0, "alta")
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(repository.updatedKm == 15.0)
    }
}
```

- [ ] **Step 2: Run the tests and confirm they fail to compile**

Run: `./gradlew testDebugUnitTest --tests "com.affonso.pedaltrack.ui.history.HistoryViewModelTest"`
Expected: FAIL — `unresolved reference: HistoryViewModel`

- [ ] **Step 3: Implement the ViewModel**

`app/src/main/java/com/affonso/pedaltrack/ui/history/HistoryViewModel.kt`:

```kotlin
package com.affonso.pedaltrack.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.affonso.pedaltrack.domain.CyclingSessionRecord
import com.affonso.pedaltrack.repository.CyclingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(val sessions: List<CyclingSessionRecord> = emptyList())

class HistoryViewModel(private val repository: CyclingRepository) : ViewModel() {
    val uiState: StateFlow<HistoryUiState> = repository.observeHistory()
        .map { HistoryUiState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState())

    fun update(id: Long, km: Double, carga: String?) {
        viewModelScope.launch { repository.updateSession(id, km, carga) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.deleteSession(id) }
    }
}
```

- [ ] **Step 4: Run the tests again and confirm they pass**

Run: `./gradlew testDebugUnitTest --tests "com.affonso.pedaltrack.ui.history.HistoryViewModelTest"`
Expected: `BUILD SUCCESSFUL`, 3 tests passed

- [ ] **Step 5: Implement the screen**

`app/src/main/java/com/affonso/pedaltrack/ui/history/HistoryScreen.kt`:

```kotlin
package com.affonso.pedaltrack.ui.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.affonso.pedaltrack.domain.CyclingSessionRecord
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(
    sessions: List<CyclingSessionRecord>,
    onUpdate: (Long, Double, String?) -> Unit,
    onDelete: (Long) -> Unit
) {
    var editing by remember { mutableStateOf<CyclingSessionRecord?>(null) }
    val currentlyEditing = editing

    if (currentlyEditing != null) {
        EditSessionForm(
            session = currentlyEditing,
            onCancel = { editing = null },
            onConfirm = { km, carga ->
                onUpdate(currentlyEditing.id, km, carga)
                editing = null
            }
        )
        return
    }

    val formatter = DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.systemDefault())

    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        items(sessions) { session ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                onClick = { editing = session }
            ) {
                Row(Modifier.padding(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(formatter.format(session.startTime))
                        Text("${session.km} km · ${session.durationMin} min")
                        session.carga?.let { Text("Carga: $it") }
                    }
                    IconButton(onClick = { onDelete(session.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Apagar")
                    }
                }
            }
        }
    }
}

@Composable
private fun EditSessionForm(
    session: CyclingSessionRecord,
    onCancel: () -> Unit,
    onConfirm: (Double, String?) -> Unit
) {
    var km by remember { mutableStateOf(session.km.toString()) }
    var carga by remember { mutableStateOf(session.carga.orEmpty()) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Editar sessão")
        OutlinedTextField(
            value = km,
            onValueChange = { km = it },
            label = { Text("Km") },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )
        OutlinedTextField(
            value = carga,
            onValueChange = { carga = it },
            label = { Text("Carga (opcional)") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        Button(
            onClick = { km.toDoubleOrNull()?.let { onConfirm(it, carga.ifBlank { null }) } },
            modifier = Modifier.padding(top = 16.dp)
        ) { Text("Salvar") }
        Button(onClick = onCancel, modifier = Modifier.padding(top = 8.dp)) { Text("Cancelar") }
    }
}
```

`Card` needs the clickable Material3 overload — it already requires `onClick`, matching the one used in `LogSessionScreen.kt` (Task 6).

- [ ] **Step 6: Verify the module compiles**

Run: `./gradlew compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/affonso/pedaltrack/ui/history/ app/src/test/java/com/affonso/pedaltrack/ui/history/
git commit -m "feat(ui): add history screen with edit and delete"
```

---

### Task 8: Summary screen

**Files:**
- Create: `app/src/main/java/com/affonso/pedaltrack/ui/summary/SummaryViewModel.kt`
- Create: `app/src/main/java/com/affonso/pedaltrack/ui/summary/SummaryScreen.kt`
- Test: `app/src/test/java/com/affonso/pedaltrack/ui/summary/SummaryViewModelTest.kt`

**Interfaces:**
- Consumes: `CyclingRepository`, `SummaryMetrics`, `SummaryPeriod` (Tasks 2, 5).
- Produces: `SummaryUiState`, `SummaryViewModel` (`uiState`, `setPeriod(period)`), `SummaryScreen(uiState, onPeriodChange)` composable — consumed by navigation in Task 9.

- [ ] **Step 1: Write the failing ViewModel tests**

`app/src/test/java/com/affonso/pedaltrack/ui/summary/SummaryViewModelTest.kt`:

```kotlin
package com.affonso.pedaltrack.ui.summary

import com.affonso.pedaltrack.domain.CyclingSessionRecord
import com.affonso.pedaltrack.domain.HealthConnectSession
import com.affonso.pedaltrack.domain.SummaryMetrics
import com.affonso.pedaltrack.domain.SummaryPeriod
import com.affonso.pedaltrack.repository.CyclingRepository
import com.affonso.pedaltrack.repository.LogResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

private class FakeCyclingRepository : CyclingRepository {
    override suspend fun getLoggableSessions(): List<HealthConnectSession> = emptyList()
    override suspend fun logSession(session: HealthConnectSession, km: Double, carga: String?): Result<LogResult> =
        Result.success(LogResult(true))
    override fun observeHistory(): Flow<List<CyclingSessionRecord>> = flowOf(emptyList())
    override suspend fun updateSession(id: Long, km: Double, carga: String?) {}
    override suspend fun deleteSession(id: Long) {}
    override suspend fun getSummary(period: SummaryPeriod): SummaryMetrics = when (period) {
        SummaryPeriod.WEEK -> SummaryMetrics(10.0, 10.0, 100.0, 30.0, 1)
        SummaryPeriod.MONTH -> SummaryMetrics(40.0, 10.0, 400.0, 30.0, 4)
        SummaryPeriod.ALL -> SummaryMetrics(100.0, 10.0, 1000.0, 30.0, 10)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SummaryViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(dispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `loads month summary by default`() = runTest {
        val viewModel = SummaryViewModel(FakeCyclingRepository())
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(40.0, viewModel.uiState.value.metrics.totalKm, 0.001)
    }

    @Test
    fun `setPeriod switches metrics`() = runTest {
        val viewModel = SummaryViewModel(FakeCyclingRepository())
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.setPeriod(SummaryPeriod.WEEK)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(10.0, viewModel.uiState.value.metrics.totalKm, 0.001)
        assertEquals(SummaryPeriod.WEEK, viewModel.uiState.value.period)
    }
}
```

- [ ] **Step 2: Run the tests and confirm they fail to compile**

Run: `./gradlew testDebugUnitTest --tests "com.affonso.pedaltrack.ui.summary.SummaryViewModelTest"`
Expected: FAIL — `unresolved reference: SummaryViewModel`

- [ ] **Step 3: Implement the ViewModel**

`app/src/main/java/com/affonso/pedaltrack/ui/summary/SummaryViewModel.kt`:

```kotlin
package com.affonso.pedaltrack.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.affonso.pedaltrack.domain.SummaryMetrics
import com.affonso.pedaltrack.domain.SummaryPeriod
import com.affonso.pedaltrack.repository.CyclingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SummaryUiState(
    val period: SummaryPeriod = SummaryPeriod.MONTH,
    val metrics: SummaryMetrics = SummaryMetrics(0.0, 0.0, 0.0, 0.0, 0)
)

class SummaryViewModel(private val repository: CyclingRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SummaryUiState())
    val uiState: StateFlow<SummaryUiState> = _uiState.asStateFlow()

    init {
        setPeriod(SummaryPeriod.MONTH)
    }

    fun setPeriod(period: SummaryPeriod) {
        viewModelScope.launch {
            val metrics = repository.getSummary(period)
            _uiState.value = SummaryUiState(period, metrics)
        }
    }
}
```

- [ ] **Step 4: Run the tests again and confirm they pass**

Run: `./gradlew testDebugUnitTest --tests "com.affonso.pedaltrack.ui.summary.SummaryViewModelTest"`
Expected: `BUILD SUCCESSFUL`, 2 tests passed

- [ ] **Step 5: Implement the screen**

`app/src/main/java/com/affonso/pedaltrack/ui/summary/SummaryScreen.kt`:

```kotlin
package com.affonso.pedaltrack.ui.summary

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.affonso.pedaltrack.domain.SummaryPeriod

@Composable
fun SummaryScreen(
    uiState: SummaryUiState,
    onPeriodChange: (SummaryPeriod) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row {
            FilterChip(selected = uiState.period == SummaryPeriod.WEEK, onClick = { onPeriodChange(SummaryPeriod.WEEK) }, label = { Text("Semana") })
            FilterChip(selected = uiState.period == SummaryPeriod.MONTH, onClick = { onPeriodChange(SummaryPeriod.MONTH) }, label = { Text("Mês") })
            FilterChip(selected = uiState.period == SummaryPeriod.ALL, onClick = { onPeriodChange(SummaryPeriod.ALL) }, label = { Text("Tudo") })
        }
        Text("Sessões: ${uiState.metrics.sessionCount}", modifier = Modifier.padding(top = 16.dp))
        Text("Km total: ${"%.1f".format(uiState.metrics.totalKm)}")
        Text("Km médio: ${"%.1f".format(uiState.metrics.avgKmPerSession)}")
        Text("Calorias totais: ${"%.0f".format(uiState.metrics.totalCalories)}")
        Text("Duração média: ${"%.0f".format(uiState.metrics.avgDurationMin)} min")
    }
}
```

- [ ] **Step 6: Verify the module compiles**

Run: `./gradlew compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/affonso/pedaltrack/ui/summary/ app/src/test/java/com/affonso/pedaltrack/ui/summary/
git commit -m "feat(ui): add summary screen with period filter"
```

---

### Task 9: Navigation, permission gating, and final verification

**Files:**
- Create: `app/src/main/java/com/affonso/pedaltrack/ui/navigation/PedalTrackNavHost.kt`
- Modify: `app/src/main/java/com/affonso/pedaltrack/MainActivity.kt` (replace Task 1 placeholder)

**Interfaces:**
- Consumes: `CyclingRepository` (Task 5); `HealthConnectManagerImpl` (Task 4); `LogSessionScreen`/`LogSessionViewModel` (Task 6); `HistoryScreen`/`HistoryViewModel` (Task 7); `SummaryScreen`/`SummaryViewModel` (Task 8).
- Produces: `PedalTrackNavHost(repository)` composable — the app's full runtime wiring, nothing else depends on this.

- [ ] **Step 1: Implement the bottom-nav host**

`app/src/main/java/com/affonso/pedaltrack/ui/navigation/PedalTrackNavHost.kt`:

```kotlin
package com.affonso.pedaltrack.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.affonso.pedaltrack.repository.CyclingRepository
import com.affonso.pedaltrack.ui.history.HistoryScreen
import com.affonso.pedaltrack.ui.history.HistoryViewModel
import com.affonso.pedaltrack.ui.log.LogSessionScreen
import com.affonso.pedaltrack.ui.log.LogSessionViewModel
import com.affonso.pedaltrack.ui.summary.SummaryScreen
import com.affonso.pedaltrack.ui.summary.SummaryViewModel

private sealed class Destination(val route: String, val label: String) {
    data object Log : Destination("log", "Lançar")
    data object History : Destination("history", "Histórico")
    data object Summary : Destination("summary", "Resumo")
}

private val destinations = listOf(Destination.Log, Destination.History, Destination.Summary)

@Composable
fun PedalTrackNavHost(repository: CyclingRepository) {
    val navController = rememberNavController()
    val factory = remember(repository) { pedalTrackViewModelFactory(repository) }

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route
            NavigationBar {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        },
                        icon = {
                            val icon = when (destination) {
                                Destination.Log -> Icons.Filled.DirectionsBike
                                Destination.History -> Icons.Filled.History
                                Destination.Summary -> Icons.Filled.BarChart
                            }
                            Icon(icon, contentDescription = destination.label)
                        },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Log.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Destination.Log.route) {
                val viewModel: LogSessionViewModel = viewModel(factory = factory)
                val uiState by viewModel.uiState.collectAsState()
                LogSessionScreen(uiState = uiState, onSubmit = viewModel::submit)
            }
            composable(Destination.History.route) {
                val viewModel: HistoryViewModel = viewModel(factory = factory)
                val uiState by viewModel.uiState.collectAsState()
                HistoryScreen(sessions = uiState.sessions, onUpdate = viewModel::update, onDelete = viewModel::delete)
            }
            composable(Destination.Summary.route) {
                val viewModel: SummaryViewModel = viewModel(factory = factory)
                val uiState by viewModel.uiState.collectAsState()
                SummaryScreen(uiState = uiState, onPeriodChange = viewModel::setPeriod)
            }
        }
    }
}

private fun pedalTrackViewModelFactory(repository: CyclingRepository) = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        LogSessionViewModel::class.java -> LogSessionViewModel(repository) as T
        HistoryViewModel::class.java -> HistoryViewModel(repository) as T
        SummaryViewModel::class.java -> SummaryViewModel(repository) as T
        else -> throw IllegalArgumentException("Unknown ViewModel: $modelClass")
    }
}
```

Add the missing `remember` import (`androidx.compose.runtime.remember`) alongside the others already listed above.

- [ ] **Step 2: Replace `MainActivity.kt` with the permission-gated entry point**

`app/src/main/java/com/affonso/pedaltrack/MainActivity.kt`:

```kotlin
package com.affonso.pedaltrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.lifecycleScope
import com.affonso.pedaltrack.data.healthconnect.HealthConnectManagerImpl
import com.affonso.pedaltrack.data.local.PedalTrackDatabase
import com.affonso.pedaltrack.repository.CyclingRepositoryImpl
import com.affonso.pedaltrack.ui.navigation.PedalTrackNavHost
import com.affonso.pedaltrack.ui.theme.PedalTrackTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var healthConnectManager: HealthConnectManagerImpl
    private lateinit var repository: CyclingRepositoryImpl
    private var permissionsGranted by mutableStateOf<Boolean?>(null)

    private val requestPermissions = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) {
        lifecycleScope.launch { permissionsGranted = healthConnectManager.hasAllPermissions() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val client = HealthConnectClient.getOrCreate(applicationContext)
        healthConnectManager = HealthConnectManagerImpl(client)
        val dao = PedalTrackDatabase.getInstance(applicationContext).cyclingSessionDao()
        repository = CyclingRepositoryImpl(dao, healthConnectManager)

        lifecycleScope.launch { permissionsGranted = healthConnectManager.hasAllPermissions() }

        setContent {
            PedalTrackTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    when (permissionsGranted) {
                        true -> PedalTrackNavHost(repository = repository)
                        false -> PermissionRequestScreen(
                            onRequestClick = { requestPermissions.launch(healthConnectManager.permissions()) }
                        )
                        null -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRequestScreen(onRequestClick: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("PedalTrack precisa de permissão para ler seus treinos de bike indoor e escrever a distância no Health Connect.")
        Button(onClick = onRequestClick, modifier = Modifier.padding(top = 16.dp)) {
            Text("Conceder permissão")
        }
    }
}
```

- [ ] **Step 3: Run the full build and test suite**

Run: `./gradlew build`
Expected: `BUILD SUCCESSFUL` — this compiles the app, `androidTest` sources, and runs every JVM unit test written in Tasks 2, 5, 6, 7 and 8.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/affonso/pedaltrack/ui/navigation/ app/src/main/java/com/affonso/pedaltrack/MainActivity.kt
git commit -m "feat(nav): wire bottom navigation and Health Connect permission flow"
```

- [ ] **Step 5: Push and hand off for on-device verification**

```bash
git push origin master
```

Then, on your own machine with Android Studio or `adb`/`sdkmanager` set up:
1. Connect the Galaxy S25+ over USB with USB debugging enabled, confirm with `adb devices`.
2. Install: `./gradlew installDebug`.
3. Open PedalTrack, grant the Health Connect permission when prompted.
4. Confirm at least one recent indoor-cycling session (already synced from the Galaxy Watch 7) appears on the "Lançar" tab.
5. Log it with a km value, confirm it moves to "Histórico" and the "Resumo" tab's totals update.
6. Open the Health Connect app → Data and access → check that a new distance entry exists for that time range.
7. Optionally now run `./gradlew connectedAndroidTest` to execute the Task 3 Room instrumented test on the connected device.
