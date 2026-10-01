package com.example.engine

import com.example.domain.model.AllocationType
import com.example.domain.model.UnderlyingIndex

data class AllocationResult(
    val calculatedQuantity: Int,
    val calculatedLots: Int,
    val estimatedCapitalRequiredPaise: Long,
    val explanation: String
)

object AllocationCalculator {

    /**
     * Calculates customer trade quantity according to configured copy mode:
     * 1. FIXED_QUANTITY: Customer specified exact units (e.g. 25 units).
     * 2. RATIO: Multiplier ratio * leader quantity (e.g. 0.50 * 100 = 50 units).
     * 3. CAPITAL_PERCENTAGE: Fraction of available customer margin.
     * 4. FIXED_CAPITAL_AMOUNT: Fixed rupee amount budget.
     *
     * Guaranteed to return an integer that is a valid multiple of the underlying index lot size
     * and strictly bounded by customer risk limits.
     */
    fun calculateCustomerQuantity(
        allocationType: AllocationType,
        leaderQuantity: Int,
        fixedQuantityUnits: Int,
        multiplierRatio: Double,
        allocationValuePaise: Long,
        allocationPct: Double,
        availableMarginPaise: Long,
        estimatedPricePaise: Long,
        underlying: UnderlyingIndex,
        maxMultiplier: Int = 1,
        maxQuantityUnitsAllowed: Int = 250,
        maxCapitalPerTradePaise: Long = 5000000L
    ): AllocationResult {
        val lotSize = underlying.lotSize
        val singleLotCostPaise = estimatedPricePaise * lotSize

        if (singleLotCostPaise <= 0L) {
            return AllocationResult(0, 0, 0L, "Invalid price or contract specifications")
        }

        val effectiveMaxCapital = if (maxCapitalPerTradePaise > 0L) {
            maxCapitalPerTradePaise.coerceAtMost(availableMarginPaise)
        } else {
            availableMarginPaise
        }

        return when (allocationType) {
            AllocationType.FIXED_QUANTITY -> {
                // Example: Leader 100, Customer fixed 25 -> Customer 25
                val targetUnits = fixedQuantityUnits.coerceAtLeast(lotSize)
                val lots = (targetUnits / lotSize).coerceAtLeast(1)
                val normalizedQty = (lots * lotSize).coerceAtMost(maxQuantityUnitsAllowed)
                val requiredCapital = normalizedQty * estimatedPricePaise

                if (requiredCapital > availableMarginPaise) {
                    AllocationResult(
                        calculatedQuantity = 0,
                        calculatedLots = 0,
                        estimatedCapitalRequiredPaise = requiredCapital,
                        explanation = "Fixed quantity ($normalizedQty units) requires ₹${requiredCapital / 100}, exceeds available margin ₹${availableMarginPaise / 100}"
                    )
                } else {
                    AllocationResult(
                        calculatedQuantity = normalizedQty,
                        calculatedLots = lots,
                        estimatedCapitalRequiredPaise = requiredCapital,
                        explanation = "Fixed quantity allocated: $normalizedQty units ($lots lots)"
                    )
                }
            }

            AllocationType.RATIO -> {
                // Example: Leader 100, Ratio 0.50 -> Customer 50
                // Example: Leader 100, Ratio 0.25 -> Customer 25
                val targetUnitsDouble = leaderQuantity * multiplierRatio.coerceIn(0.01, 10.0)
                val lots = (targetUnitsDouble / lotSize).toInt().coerceAtLeast(1)
                val normalizedQty = (lots * lotSize).coerceAtMost(maxQuantityUnitsAllowed)
                val requiredCapital = normalizedQty * estimatedPricePaise

                if (requiredCapital > availableMarginPaise) {
                    // Try scaling down lots to fit available margin
                    val maxAffordableLots = (availableMarginPaise / singleLotCostPaise).toInt()
                    if (maxAffordableLots <= 0) {
                        AllocationResult(
                            calculatedQuantity = 0,
                            calculatedLots = 0,
                            estimatedCapitalRequiredPaise = requiredCapital,
                            explanation = "Insufficient margin for ratio $multiplierRatio (${normalizedQty} units requires ₹${requiredCapital / 100})"
                        )
                    } else {
                        val affordableQty = maxAffordableLots * lotSize
                        AllocationResult(
                            calculatedQuantity = affordableQty,
                            calculatedLots = maxAffordableLots,
                            estimatedCapitalRequiredPaise = affordableQty * estimatedPricePaise,
                            explanation = "Ratio $multiplierRatio adjusted to affordable margin: $affordableQty units ($maxAffordableLots lots)"
                        )
                    }
                } else {
                    AllocationResult(
                        calculatedQuantity = normalizedQty,
                        calculatedLots = lots,
                        estimatedCapitalRequiredPaise = requiredCapital,
                        explanation = "Ratio allocation ($multiplierRatio x $leaderQuantity): $normalizedQty units ($lots lots)"
                    )
                }
            }

            AllocationType.CAPITAL_PERCENTAGE, AllocationType.PERCENTAGE -> {
                // Example: ₹50,000 margin, 20% allocation -> ₹10,000 budget
                val fraction = (allocationPct / 100.0).coerceIn(0.01, 1.0)
                val budget = ((availableMarginPaise * fraction).toLong()).coerceAtMost(effectiveMaxCapital)

                if (budget < singleLotCostPaise) {
                    AllocationResult(
                        calculatedQuantity = 0,
                        calculatedLots = 0,
                        estimatedCapitalRequiredPaise = singleLotCostPaise,
                        explanation = "Budget of ₹${budget / 100} is insufficient for 1 lot (Requires ₹${singleLotCostPaise / 100})"
                    )
                } else {
                    val rawLots = (budget / singleLotCostPaise).toInt()
                    val cappedLots = (rawLots * lotSize).coerceAtMost(maxQuantityUnitsAllowed) / lotSize
                    val finalLots = cappedLots.coerceAtLeast(1)
                    val finalQuantity = finalLots * lotSize
                    val requiredCapital = finalQuantity * estimatedPricePaise
                    AllocationResult(
                        calculatedQuantity = finalQuantity,
                        calculatedLots = finalLots,
                        estimatedCapitalRequiredPaise = requiredCapital,
                        explanation = "Allocated $finalQuantity units ($finalLots lots) using $allocationPct% capital budget"
                    )
                }
            }

            AllocationType.FIXED_CAPITAL_AMOUNT, AllocationType.FIXED_AMOUNT -> {
                val budget = allocationValuePaise.coerceAtMost(effectiveMaxCapital)
                if (budget < singleLotCostPaise) {
                    AllocationResult(
                        calculatedQuantity = 0,
                        calculatedLots = 0,
                        estimatedCapitalRequiredPaise = singleLotCostPaise,
                        explanation = "Fixed budget of ₹${budget / 100} is insufficient for 1 lot (Requires ₹${singleLotCostPaise / 100})"
                    )
                } else {
                    val rawLots = (budget / singleLotCostPaise).toInt()
                    val cappedLots = (rawLots * lotSize).coerceAtMost(maxQuantityUnitsAllowed) / lotSize
                    val finalLots = cappedLots.coerceAtLeast(1)
                    val finalQuantity = finalLots * lotSize
                    val requiredCapital = finalQuantity * estimatedPricePaise
                    AllocationResult(
                        calculatedQuantity = finalQuantity,
                        calculatedLots = finalLots,
                        estimatedCapitalRequiredPaise = requiredCapital,
                        explanation = "Allocated $finalQuantity units ($finalLots lots) within ₹${budget / 100} budget"
                    )
                }
            }
        }
    }

    /**
     * Backward-compatible overload for legacy calls.
     */
    fun calculateQuantity(
        allocationType: AllocationType,
        allocationValuePaise: Long,
        allocationPct: Double,
        availableMarginPaise: Long,
        estimatedPricePaise: Long,
        underlying: UnderlyingIndex,
        maxMultiplier: Int = 1,
        maxQuantityLotsAllowed: Int = 20
    ): AllocationResult {
        return calculateCustomerQuantity(
            allocationType = allocationType,
            leaderQuantity = 100,
            fixedQuantityUnits = 25,
            multiplierRatio = 0.50,
            allocationValuePaise = allocationValuePaise,
            allocationPct = allocationPct,
            availableMarginPaise = availableMarginPaise,
            estimatedPricePaise = estimatedPricePaise,
            underlying = underlying,
            maxMultiplier = maxMultiplier,
            maxQuantityUnitsAllowed = maxQuantityLotsAllowed * underlying.lotSize,
            maxCapitalPerTradePaise = 5000000L
        )
    }

    fun isValidLotQuantity(quantity: Int, underlying: UnderlyingIndex): Boolean {
        if (quantity <= 0) return false
        return (quantity % underlying.lotSize) == 0
    }
}
