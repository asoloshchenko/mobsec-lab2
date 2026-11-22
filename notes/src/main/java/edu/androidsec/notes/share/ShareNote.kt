package edu.androidsec.notes.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import edu.androidsec.notes.R
import edu.androidsec.notes.domain.Note
import java.io.File

object ShareNote {
    const val AUTHORITY = "edu.androidsec.notes.files"

    fun shareNoteAsFile(context: Context, note: Note): File {
        val file = NoteExporter.export(context, note)
        val uri = FileProvider.getUriForFile(context, AUTHORITY, file)

        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, context.getString(R.string.share_chooser_title))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        context.startActivity(chooser)
        return file
    }
}
