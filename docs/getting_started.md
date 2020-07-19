## Quick Start

scrcast provides a variety of configuration options for capturing, storing, and providing user interactions with your screen recordings.

### Configuring

```kotlin
val recorder = ScrCast.use(this)
recorder.apply {
    // configure options via DSL
    options {
        video {
            maxLengthSecs = 360
        }
        storage {
            directoryName = "scrcast-sample"
        }
        notification {
            title = "Super cool library"
            description = "shh session in progress"
            icon = resources.getDrawable(R.drawable.ic_camcorder, null).toBitmap()
            channel {
                id = "1337"
                name = "Recording Service"
            }
            showStop = true
            showPause = true
            showTimer = true
        }
        moveTaskToBack = false
        startDelayMs = 5_000
    }
}
```

### State

interaction with `MediaRecorder`is abstracted in a easy to use and manage interface, via explict state-changing accessors.

#### Start

```kotlin
recorder.record()
```

#### Stop

```kotlin
recorder.stopRecording()
```

#### Pause

```kotlin
recorder.pause()
```

#### Resume

```kotlin
recorder.resume()
```

### Callbacks

State changes are emitted via `RecordingCallbacks` as a single interface or via a discrete lambda `onRecordingStateChange`

Completed recording output file is also emittable in `RecordingCallbacks` via

 ```kotlin
 fun onRecordingFinished(file: File)
 ```

## Requirements

* AndroidX
* `minSdkVersion` 23+
* `compileSdkVersion` 28+
* Java 8+

Gradle (`.gradle`)

```kotlin
android {
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile).all {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
```

Gradle Kotlin DSL (`.gradle.kts`)

```kotlin
android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
```

## License

```text
Copyright 2020 bmcreations

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
