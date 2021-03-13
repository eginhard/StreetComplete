package de.westnordost.streetcomplete.data

interface Database {

    suspend fun exec(sql: String, args: Array<Any>? = null)

    suspend fun <T> query(
        table: String,
        columns: Array<String>? = null,
        where: String? = null,
        args: Array<Any>? = null,
        groupBy: String? = null,
        having: String? = null,
        orderBy: String? = null,
        limit: String? = null,
        transform: (CursorPosition) -> T
    ): Sequence<T>

    suspend fun insert(
        table: String,
        values: Collection<Pair<String, Any?>>,
        conflictAlgorithm: ConflictAlgorithm? = null
    ): Long

    suspend fun insertOrIgnore(table: String, values: Collection<Pair<String, Any?>>): Long =
        insert(table, values, ConflictAlgorithm.IGNORE)

    suspend fun replace(table: String, values: Collection<Pair<String, Any?>>): Long =
        insert(table, values, ConflictAlgorithm.REPLACE)

    suspend fun update(
        table: String,
        values: Collection<Pair<String, Any?>>,
        where: String? = null,
        args: Array<Any>? = null,
        conflictAlgorithm: ConflictAlgorithm? = null
    ): Int

    suspend fun delete(
        table: String,
        where: String? = null,
        args: Array<Any>? = null
    ): Int

    suspend fun <T> transaction(body: suspend () -> T): T
}

enum class ConflictAlgorithm {
    ROLLBACK,
    ABORT,
    FAIL,
    IGNORE,
    REPLACE
}

/** Data available at the current cursor position */
interface CursorPosition {
    /* It would be really nice if the interface would be just
       operator fun <T> get(columnName: String): T
       if T is one of the below types. But this is not possible right now in Kotlin AFAIK */
    fun getShort(columnName: String): Short
    fun getInt(columnName: String): Int
    fun getLong(columnName: String): Long
    fun getDouble(columnName: String): Double
    fun getFloat(columnName: String): Float
    fun getBlob(columnName: String): ByteArray
    fun getString(columnName: String): String
    fun getShortOrNull(columnName: String): Short?
    fun getIntOrNull(columnName: String): Int?
    fun getLongOrNull(columnName: String): Long?
    fun getDoubleOrNull(columnName: String): Double?
    fun getFloatOrNull(columnName: String): Float?
    fun getBlobOrNull(columnName: String): ByteArray?
    fun getStringOrNull(columnName: String): String?
}
