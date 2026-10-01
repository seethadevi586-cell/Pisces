package com.example.data.local

import androidx.room.TypeConverter
import com.example.domain.model.*

class Converters {
    @TypeConverter
    fun fromUserRole(value: UserRole): String = value.name
    @TypeConverter
    fun toUserRole(value: String): UserRole = enumValueOf(value)

    @TypeConverter
    fun fromUserStatus(value: UserStatus): String = value.name
    @TypeConverter
    fun toUserStatus(value: String): UserStatus = enumValueOf(value)

    @TypeConverter
    fun fromOrderSide(value: OrderSide): String = value.name
    @TypeConverter
    fun toOrderSide(value: String): OrderSide = enumValueOf(value)

    @TypeConverter
    fun fromOptionType(value: OptionType): String = value.name
    @TypeConverter
    fun toOptionType(value: String): OptionType = enumValueOf(value)

    @TypeConverter
    fun fromUnderlyingIndex(value: UnderlyingIndex): String = value.name
    @TypeConverter
    fun toUnderlyingIndex(value: String): UnderlyingIndex = enumValueOf(value)

    @TypeConverter
    fun fromInstrumentType(value: InstrumentType): String = value.name
    @TypeConverter
    fun toInstrumentType(value: String): InstrumentType = enumValueOf(value)

    @TypeConverter
    fun fromOrderType(value: OrderType): String = value.name
    @TypeConverter
    fun toOrderType(value: String): OrderType = enumValueOf(value)

    @TypeConverter
    fun fromSignalType(value: SignalType): String = value.name
    @TypeConverter
    fun toSignalType(value: String): SignalType = enumValueOf(value)

    @TypeConverter
    fun fromSignalStatus(value: SignalStatus): String = value.name
    @TypeConverter
    fun toSignalStatus(value: String): SignalStatus = enumValueOf(value)

    @TypeConverter
    fun fromOrderStatus(value: OrderStatus): String = value.name
    @TypeConverter
    fun toOrderStatus(value: String): OrderStatus = enumValueOf(value)

    @TypeConverter
    fun fromPositionStatus(value: PositionStatus): String = value.name
    @TypeConverter
    fun toPositionStatus(value: String): PositionStatus = enumValueOf(value)

    @TypeConverter
    fun fromAllocationType(value: AllocationType): String = value.name
    @TypeConverter
    fun toAllocationType(value: String): AllocationType = enumValueOf(value)

    @TypeConverter
    fun fromBrokerCode(value: BrokerCode): String = value.name
    @TypeConverter
    fun toBrokerCode(value: String): BrokerCode = enumValueOf(value)

    @TypeConverter
    fun fromBrokerAccountStatus(value: BrokerAccountStatus): String = value.name
    @TypeConverter
    fun toBrokerAccountStatus(value: String): BrokerAccountStatus = enumValueOf(value)

    @TypeConverter
    fun fromStrategyStatus(value: StrategyStatus): String = value.name
    @TypeConverter
    fun toStrategyStatus(value: String): StrategyStatus = enumValueOf(value)

    @TypeConverter
    fun fromNotificationType(value: NotificationType): String = value.name
    @TypeConverter
    fun toNotificationType(value: String): NotificationType = enumValueOf(value)

    @TypeConverter
    fun fromSimulationFailureMode(value: SimulationFailureMode): String = value.name
    @TypeConverter
    fun toSimulationFailureMode(value: String): SimulationFailureMode = enumValueOf(value)
}
