package edu.androidsec.notes.deeplink

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri

/** Что пришло во входящем интенте Activity. */
sealed interface IncomingNote {
    /** Открыть заметку с корректным id (существует ли она, проверит экран заметки). */
    data class Open(val id: Long) : IncomingNote

    /** Ссылка на заметку есть, но параметр некорректен: показать сообщение, ничего не открывать. */
    data object Invalid : IncomingNote

    /** Обычный запуск без ссылки. */
    data object None : IncomingNote
}

object NoteDeepLink {
    const val SCHEME = "notes"
    const val HOST = "note"

    /** Ссылка на заметку в фиксированном формате `notes://note/<id>`. */
    fun uriFor(noteId: Long): Uri = "$SCHEME://$HOST/$noteId".toUri()

    fun parse(intent: Intent?): IncomingNote {
        if (intent?.action != Intent.ACTION_VIEW) return IncomingNote.None
        val data = intent.data ?: return IncomingNote.None
        if (!SCHEME.equals(data.scheme, ignoreCase = true)) return IncomingNote.None
        if (data.host != HOST) return IncomingNote.None

        val segments = data.pathSegments
        if (segments.size != 1) return IncomingNote.Invalid

        val id = segments.single().toLongOrNull()
        return if (id != null && id > 0L) IncomingNote.Open(id) else IncomingNote.Invalid
    }
}
