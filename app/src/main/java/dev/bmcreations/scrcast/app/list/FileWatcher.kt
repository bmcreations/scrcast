package dev.bmcreations.scrcast.app.list

import android.util.Log
import dev.vishna.watchservice.KWatchChannel
import dev.vishna.watchservice.KWatchEvent
import dev.vishna.watchservice.asWatchChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import java.io.File

class FileWatcher(private val directory: File) : AutoCloseable, CoroutineScope by CoroutineScope(Dispatchers.IO) {


    private var watchChannel = directory.asWatchChannel()


    private val files = mutableListOf<File>().also {
        it.addAll(directory.listFiles()?.toList() ?: emptyList())
    }

    var isWatching = false


    override fun close() {
        isWatching = false
        watchChannel.close()
    }

    fun watch(callback: (List<File>) -> Unit) {
        // initialize watcher with resumed files
        if (watchChannel.isClosedForReceive || watchChannel.isClosedForSend) {
            watchChannel = directory.asWatchChannel()
        }

        callback(files)

        launch {
            watchChannel.consumeEach { event ->
                isWatching = true
                val (kind, file) = event.kind to event.file

                if (file != directory) {
                    var updated = false

                    Log.d("FileWatcher", "kind=$kind, ${file.absolutePath}")

                    when (kind) {
                        KWatchEvent.Kind.Initialized -> {
                            updated = true
                            files.add(file)
                        }
                        KWatchEvent.Kind.Created -> {
                            updated = true
                            files.add(file)
                        }
                        KWatchEvent.Kind.Modified -> {
                            val match = files.indexOfFirst { it.name == file.name }
                            if (match >= 0) {
                                files[match] = file
                            }
                        }
                        KWatchEvent.Kind.Deleted -> {
                            updated = true
                            files.remove(file)
                        }
                    }
                    if (updated) {
                        callback(files)
                    }
                }
            }
        }
    }
}

fun File.watch(callback: (List<File>) -> Unit): FileWatcher {
    return FileWatcher(this).apply { watch(callback) }
}

