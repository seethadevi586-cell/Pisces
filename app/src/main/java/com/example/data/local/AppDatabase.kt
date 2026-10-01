package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.local.dao.TradingDao
import com.example.data.local.entity.*

@Database(
    entities = [
        UserEntity::class,
        BrokerAccountEntity::class,
        StrategyEntity::class,
        StrategyFollowerEntity::class,
        RiskLimitEntity::class,
        SignalEntity::class,
        CopyOrderEntity::class,
        PositionEntity::class,
        AuditLogEntity::class,
        NotificationEntity::class,
        SystemConfigEntity::class,
        LeaderTradeEventEntity::class,
        TradeSignalEntity::class,
        PositionMappingEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tradingDao(): TradingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "trade_mirror.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
