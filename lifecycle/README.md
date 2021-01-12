# Lifecycle-aware support

To add Lifecycle support, import the extension library:

```kotlin
implementation("dev.bmcreations:scrcast-lifecycle:0.1.0")
```

You can now observe RecordingState changes via an Observer:

```kotlin
recorder.observeRecordingState(lifecycleOwner, { state -> handleRecorderState(state) })
```
