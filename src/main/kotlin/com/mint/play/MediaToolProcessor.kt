package com.mint.play

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import kotlin.concurrent.thread

internal class MediaToolProcessor(context: Context) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    fun export(
        sourceUri: Uri,
        destinationUri: Uri,
        removeVideo: Boolean,
        outputIsAudio: Boolean,
        startMs: Long? = null,
        endMs: Long? = null,
        onFinished: (Result<Unit>) -> Unit
    ): Transformer {
        val extension = if (outputIsAudio) "m4a" else "mp4"
        val outputFile = File(appContext.cacheDir, "mintplay_tool_${System.currentTimeMillis()}.$extension")
        val mediaItemBuilder = MediaItem.Builder().setUri(sourceUri)

        if (startMs != null && endMs != null) {
            mediaItemBuilder.setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMs)
                    .setEndPositionMs(endMs)
                    .build()
            )
        }

        val editedItem = EditedMediaItem.Builder(mediaItemBuilder.build())
            .setRemoveVideo(removeVideo)
            .build()

        val transformer = Transformer.Builder(appContext)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    thread(name = "MintPlayOutputCopy") {
                        val result = runCatching {
                            val output = appContext.contentResolver.openOutputStream(destinationUri)
                                ?: throw IOException("Couldn't open the save location.")
                            output.use { destination ->
                                FileInputStream(outputFile).use { input -> input.copyTo(destination) }
                            }
                        }
                        outputFile.delete()
                        mainHandler.post { onFinished(result) }
                    }
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    outputFile.delete()
                    mainHandler.post { onFinished(Result.failure(exportException)) }
                }
            })
            .build()

        try {
            transformer.start(editedItem, outputFile.absolutePath)
        } catch (exception: Exception) {
            outputFile.delete()
            throw exception
        }
        return transformer
    }
}
