package edu.androidsec.notes.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import edu.androidsec.notes.appContainer

class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DONE) return
        val noteId = intent.getLongExtra(EXTRA_NOTE_ID, 0L)
        if (noteId <= 0L) return

        NotificationManagerCompat.from(context).cancel(noteId.toInt())
        context.appContainer.reminderScheduler.cancel(noteId)
        Log.i(TAG, "DONE received for note $noteId")
    }

    companion object {
        const val TAG = "ReminderActionReceiver"
        const val ACTION_DONE = "edu.androidsec.notes.action.DONE"
        const val EXTRA_NOTE_ID = "edu.androidsec.notes.extra.NOTE_ID"
    }
}
