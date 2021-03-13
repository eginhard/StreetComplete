package de.westnordost.streetcomplete.data

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase.*
import android.database.sqlite.SQLiteOpenHelper
import de.westnordost.streetcomplete.data.ConflictAlgorithm.*
import de.westnordost.streetcomplete.ktx.*
import de.westnordost.streetcomplete.ktx.getShortOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@SuppressLint("Recycle")
class AndroidDatabase @Inject constructor(private val dbHelper: SQLiteOpenHelper) : Database {
    private val db get() = dbHelper.writableDatabase

    override suspend fun exec(sql: String, args: Array<Any>?) = io {
        if (args == null) db.execSQL(sql) else db.execSQL(sql, args)
    }

    override suspend fun <T> query(
        table: String,
        columns: Array<String>?,
        where: String?,
        args: Array<Any>?,
        groupBy: String?,
        having: String?,
        orderBy: String?,
        limit: String?,
        transform: (CursorPosition) -> T
    ): Sequence<T> = io {
        val strArgs = args?.primitivesArrayToStringArray()
        db.query(table, columns, where, strArgs, groupBy, having, orderBy, limit).toSequence(transform)
    }

    override suspend fun insert(
        table: String,
        values: Collection<Pair<String, Any?>>,
        conflictAlgorithm: ConflictAlgorithm?
    ): Long = io {
        db.insertWithOnConflict(
            table,
            null,
            values.toContentValues(),
            conflictAlgorithm.toAndroidConflictAlgorithm()
        )
    }

    override suspend fun update(
        table: String,
        values: Collection<Pair<String, Any?>>,
        where: String?,
        args: Array<Any>?,
        conflictAlgorithm: ConflictAlgorithm?
    ): Int = io {
        db.updateWithOnConflict(
            table,
            values.toContentValues(),
            where,
            args?.primitivesArrayToStringArray(),
            conflictAlgorithm.toAndroidConflictAlgorithm()
        )
    }


    override suspend fun delete(table: String, where: String?, args: Array<Any>?): Int = io {
        val strArgs = args?.primitivesArrayToStringArray()
        db.delete(table, where, strArgs)
    }

    override suspend fun <T> transaction(body: suspend () -> T): T = io {
        db.beginTransaction()
        try {
            val result = body()
            db.setTransactionSuccessful()
            result
        } finally {
            db.endTransaction()
        }
    }

    private fun Array<Any>.primitivesArrayToStringArray() = Array(size) { i ->
        primitiveToString(this[i])
    }

    private fun primitiveToString(any: Any): String = when(any) {
        is Short, Int, Long, Float, Double -> any.toString()
        is String -> any
        else -> throw IllegalArgumentException("Cannot bind $any: Must be either Int, Long, Float, Double or String")
    }

    private suspend fun <T> io(block: suspend CoroutineScope.() -> T): T =
        withContext(Dispatchers.IO, block)
}

private inline fun <T> Cursor.toSequence(crossinline transform: (CursorPosition) -> T): Sequence<T> = use { cursor ->
    val c = AndroidCursorPosition(cursor)
    cursor.moveToFirst()
    sequence {
        while(!cursor.isAfterLast) {
            yield(transform(c))
            cursor.moveToNext()
        }
    }
}

class AndroidCursorPosition(private val cursor: Cursor): CursorPosition {
    override fun getShort(columnName: String): Short = cursor.getShort(columnName)
    override fun getInt(columnName: String): Int = cursor.getInt(columnName)
    override fun getLong(columnName: String): Long = cursor.getLong(columnName)
    override fun getDouble(columnName: String): Double = cursor.getDouble(columnName)
    override fun getFloat(columnName: String): Float = cursor.getFloat(columnName)
    override fun getBlob(columnName: String): ByteArray = cursor.getBlob(columnName)
    override fun getString(columnName: String): String = cursor.getString(columnName)
    override fun getShortOrNull(columnName: String): Short? = cursor.getShortOrNull(columnName)
    override fun getIntOrNull(columnName: String): Int? = cursor.getIntOrNull(columnName)
    override fun getLongOrNull(columnName: String): Long? = cursor.getLongOrNull(columnName)
    override fun getDoubleOrNull(columnName: String): Double? = cursor.getDoubleOrNull(columnName)
    override fun getFloatOrNull(columnName: String): Float? = cursor.getFloatOrNull(columnName)
    override fun getBlobOrNull(columnName: String): ByteArray? = cursor.getBlobOrNull(columnName)
    override fun getStringOrNull(columnName: String): String? = cursor.getStringOrNull(columnName)
}

private fun Collection<Pair<String, Any?>>.toContentValues() = ContentValues(size).also {
    for ((key, value) in this) {
        when (value) {
            null -> it.putNull(key)
            is String -> it.put(key, value)
            is Short -> it.put(key, value)
            is Int -> it.put(key, value)
            is Long -> it.put(key, value)
            is Float -> it.put(key, value)
            is Double -> it.put(key, value)
            is ByteArray -> it.put(key, value)
            else -> {
                val valueType = value.javaClass.canonicalName
                throw IllegalArgumentException("Illegal value type $valueType for key \"$key\"")
            }
        }
    }
}

private fun ConflictAlgorithm?.toAndroidConflictAlgorithm() = when(this) {
    ROLLBACK -> CONFLICT_ROLLBACK
    ABORT -> CONFLICT_ABORT
    FAIL -> CONFLICT_FAIL
    IGNORE -> CONFLICT_IGNORE
    REPLACE -> CONFLICT_REPLACE
    null -> CONFLICT_NONE
}
