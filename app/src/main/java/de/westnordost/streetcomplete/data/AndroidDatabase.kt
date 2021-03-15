package de.westnordost.streetcomplete.data

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase.*
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteStatement
import de.westnordost.streetcomplete.data.ConflictAlgorithm.*
import de.westnordost.streetcomplete.ktx.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@SuppressLint("Recycle")
class AndroidDatabase @Inject constructor(private val dbHelper: SQLiteOpenHelper) : Database {
    private val db get() = dbHelper.writableDatabase

    override suspend fun exec(sql: String, args: Array<Any>?) = execute {
        if (args == null) db.execSQL(sql) else db.execSQL(sql, args)
    }

    override suspend fun <T> rawQuery(
        sql: String,
        args: Array<Any>?,
        transform: (CursorPosition) -> T
    ): List<T> = execute {
        val strArgs = args?.primitivesArrayToStringArray()
        db.rawQuery(sql, strArgs).toSequence(transform).toList()
    }

    override suspend fun <T> queryOne(
        table: String,
        columns: Array<String>?,
        where: String?,
        args: Array<Any>?,
        groupBy: String?,
        having: String?,
        orderBy: String?,
        transform: (CursorPosition) -> T
    ): T? = execute {
        val strArgs = args?.primitivesArrayToStringArray()
        db.query(false, table, columns, where, strArgs, groupBy, having, orderBy, "1").toSequence(transform).firstOrNull()
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
        distinct: Boolean,
        transform: (CursorPosition) -> T
    ): List<T> = execute {
        val strArgs = args?.primitivesArrayToStringArray()
        db.query(false, table, columns, where, strArgs, groupBy, having, orderBy, limit).toSequence(transform).toList()
    }

    override suspend fun insert(
        table: String,
        values: Collection<Pair<String, Any?>>,
        conflictAlgorithm: ConflictAlgorithm?
    ): Long = execute {
        db.insertWithOnConflict(
            table,
            null,
            values.toContentValues(),
            conflictAlgorithm.toConstant()
        )
    }

    override suspend fun insertMany(
        table: String,
        columnNames: Array<String>,
        valuesList: Iterable<Array<Any?>>,
        conflictAlgorithm: ConflictAlgorithm?
    ) {
        val conflictStr = conflictAlgorithm.toSQL()
        val columnNamesStr = columnNames.joinToString(",")
        val placeholdersStr = Array(columnNames.size) { "?" }.joinToString(",")
        val stmt = db.compileStatement("INSERT $conflictStr INTO $table ($columnNamesStr) VALUES ($placeholdersStr)")

        transaction {
            for (values in valuesList) {
                require(values.size == columnNames.size)
                for ((i, value) in values.withIndex()) {
                    stmt.bind(i, value)
                }
                stmt.executeInsert()
                stmt.clearBindings()
            }
            stmt.close()
        }
    }

    override suspend fun update(
        table: String,
        values: Collection<Pair<String, Any?>>,
        where: String?,
        args: Array<Any>?,
        conflictAlgorithm: ConflictAlgorithm?
    ): Int = execute {
        db.updateWithOnConflict(
            table,
            values.toContentValues(),
            where,
            args?.primitivesArrayToStringArray(),
            conflictAlgorithm.toConstant()
        )
    }


    override suspend fun delete(table: String, where: String?, args: Array<Any>?): Int = execute {
        val strArgs = args?.primitivesArrayToStringArray()
        db.delete(table, where, strArgs)
    }

    override suspend fun <T> transaction(block: suspend () -> T): T = withContext(Dispatchers.IO) {
        db.beginTransaction()
        try {
            val result = block()
            db.setTransactionSuccessful()
            result
        } finally {
            db.endTransaction()
        }
    }

    private suspend fun <T> execute(block: () -> T): T {
        // if we are already in a transaction, let's not create a new scope for every statement therein
        if (db.isOpen && db.inTransaction()) {
            block()
        }
        return withContext(Dispatchers.IO) { block() }
    }
}

private fun Array<Any>.primitivesArrayToStringArray() = Array(size) { i ->
    primitiveToString(this[i])
}

private fun primitiveToString(any: Any): String = when (any) {
    is Short, Int, Long, Float, Double -> any.toString()
    is String -> any
    else -> throw IllegalArgumentException("Cannot bind $any: Must be either Int, Long, Float, Double or String")
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

private fun ConflictAlgorithm?.toConstant() = when(this) {
    ROLLBACK -> CONFLICT_ROLLBACK
    ABORT -> CONFLICT_ABORT
    FAIL -> CONFLICT_FAIL
    IGNORE -> CONFLICT_IGNORE
    REPLACE -> CONFLICT_REPLACE
    null -> CONFLICT_NONE
}

private fun ConflictAlgorithm?.toSQL() = when(this) {
    ROLLBACK -> " OR ROLLBACK "
    ABORT -> " OR ABORT "
    FAIL -> " OR FAIL "
    IGNORE -> " OR IGNORE "
    REPLACE -> " OR REPLACE "
    null -> ""
}

private fun SQLiteStatement.bind(i: Int, value: Any?) {
    when(value) {
        null -> bindNull(i)
        is String -> bindString(i, value)
        is Double -> bindDouble(i, value)
        is Long -> bindLong(i, value)
        is ByteArray -> bindBlob(i, value)
        is Int -> bindLong(i, value.toLong())
        is Short -> bindLong(i, value.toLong())
        is Float -> bindDouble(i, value.toDouble())
        else -> {
            val valueType = value.javaClass.canonicalName
            throw IllegalArgumentException("Illegal value type $valueType at column $i")
        }
    }
}
