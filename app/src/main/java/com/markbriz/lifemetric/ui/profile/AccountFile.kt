package com.markbriz.lifemetric.ui.profile

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.markbriz.lifemetric.data.accountExportFilename
import java.io.File
import java.time.LocalDateTime

/** Writes the JSON to the app cache and opens the share sheet so the user can save it. */
fun shareAccountJson(context: Context, json: String, now: LocalDateTime = LocalDateTime.now()) {
    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    dir.listFiles()?.forEach { it.delete() }
    val file = File(dir, accountExportFilename(now))
    file.writeText(json)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, file.name)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(send, "Export Life Metric data").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}

/** Reads the picked document as text. */
fun readPickedJson(context: Context, uri: Uri): String {
    val stream = context.contentResolver.openInputStream(uri)
        ?: throw IllegalStateException("Unable to open the selected file.")
    return stream.use { it.bufferedReader().readText() }
}
