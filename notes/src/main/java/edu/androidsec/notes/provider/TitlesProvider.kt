package edu.androidsec.notes.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Binder
import android.provider.BaseColumns
import android.util.Log
import edu.androidsec.notes.appContainer

class TitlesProvider : ContentProvider() {

    private val matcher = UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI(AUTHORITY, PATH_NOTES, CODE_NOTES)
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        if (matcher.match(uri) != CODE_NOTES) throw IllegalArgumentException("Unknown URI: $uri")

        val uid = Binder.getCallingUid()
        val packages = context?.packageManager?.getPackagesForUid(uid)?.joinToString()
        Log.i(TAG, "query from uid=$uid packages=$packages")

        val columns = projection ?: DEFAULT_COLUMNS
        val titles = requireContext().appContainer.database.noteDao().titlesBlocking()

        return MatrixCursor(columns).apply {
            titles.forEach { title ->
                addRow(
                    columns.map { column ->
                        when (column) {
                            BaseColumns._ID -> title.id
                            COLUMN_TITLE -> title.title
                            else -> throw IllegalArgumentException("Unknown column: $column")
                        }
                    },
                )
            }
        }
    }

    override fun getType(uri: Uri): String? =
        if (matcher.match(uri) == CODE_NOTES) {
            "vnd.android.cursor.dir/vnd.$AUTHORITY.$PATH_NOTES"
        } else {
            null
        }

    override fun insert(uri: Uri, values: ContentValues?): Uri? =
        throw UnsupportedOperationException("TitlesProvider is read-only")

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = throw UnsupportedOperationException("TitlesProvider is read-only")

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("TitlesProvider is read-only")

    companion object {
        const val TAG = "TitlesProvider"
        const val AUTHORITY = "edu.androidsec.notes.provider"
        const val PERMISSION = "edu.androidsec.notes.permission.READ_NOTES"
        const val PATH_NOTES = "notes"
        const val COLUMN_TITLE = "title"

        private const val CODE_NOTES = 1
        private val DEFAULT_COLUMNS = arrayOf(BaseColumns._ID, COLUMN_TITLE)
    }
}
