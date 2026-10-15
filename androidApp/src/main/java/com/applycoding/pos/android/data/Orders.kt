package com.applycoding.pos.android.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** Orders are written locally first (offline first) and synced to the GraphQL backend later. */
@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val clientOrderId: String,
    val createdAtMillis: Long,
    val linesJson: String,
    val netCents: Long,
    val vatCents: Long,
    val grossCents: Long,
    val serverId: String? = null,
)

@Dao
interface OrderDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(order: OrderEntity)

    @Query("SELECT * FROM orders WHERE serverId IS NULL ORDER BY createdAtMillis")
    suspend fun pending(): List<OrderEntity>

    @Query("UPDATE orders SET serverId = :serverId WHERE clientOrderId = :clientOrderId")
    suspend fun markSynced(clientOrderId: String, serverId: String)

    @Query("SELECT * FROM orders ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<OrderEntity>>

    @Query("SELECT COALESCE(SUM(grossCents), 0) FROM orders")
    suspend fun takings(): Long
}

@Database(entities = [OrderEntity::class], version = 1, exportSchema = false)
abstract class PosDatabase : RoomDatabase() {
    abstract fun orders(): OrderDao

    companion object {
        fun create(context: Context): PosDatabase = Room.databaseBuilder(context, PosDatabase::class.java, "pos.db").build()
    }
}
