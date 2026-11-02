package com.budgetbuddy

import com.budgetbuddy.CategoryLimitCalculator.CategoryLimitStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/*
 * Start of class
 * Name of class and related classes (parent/child classes): CategoryLimitCalculatorTest
 * Parent class: Any; child classes: none; related classes: CategoryLimitCalculator and JUnit.
 * What the class does: Verifies per-category limit rows, ordering and status bands.
 * What's important to other classes, if applicable: Its assertions document the behavior production classes must preserve.
 * Code with comments begins below.
 */
class CategoryLimitCalculatorTest {
    @Test
    fun rowsShowSpentRemainingAndStatusMostUsedFirst() {
        val rows = CategoryLimitCalculator.rows(
            limits = mapOf("Groceries" to 200.0, "Dining" to 100.0, "Transport" to 80.0),
            spentByCategory = mapOf("Groceries" to 120.0, "Dining" to 130.0, "Health" to 40.0)
        )
        assertEquals(listOf("Dining", "Groceries", "Transport"), rows.map { it.categoryId })
        val dining = rows[0]
        assertEquals(130.0, dining.percentage, 0.001)
        assertEquals(-30.0, dining.remaining, 0.001)
        assertEquals(CategoryLimitStatus.OVER, dining.status)
        assertEquals(CategoryLimitStatus.SNUG, rows[1].status)
        assertEquals(0.0, rows[2].spent, 0.001)
        assertEquals(CategoryLimitStatus.ROOMY, rows[2].status)
    }

    @Test
    fun categoriesWithoutAPositiveLimitAreLeftOut() {
        val rows = CategoryLimitCalculator.rows(
            mapOf("A" to 0.0, "B" to -5.0, "C" to Double.NaN, "D" to 10.0),
            mapOf("A" to 50.0, "D" to Double.POSITIVE_INFINITY)
        )
        assertEquals(listOf("D"), rows.map { it.categoryId })
        assertEquals(0.0, rows[0].spent, 0.001)
    }

    @Test
    fun statusBandsMatchTheGauge() {
        assertEquals(CategoryLimitStatus.ROOMY, CategoryLimitCalculator.statusFor(49.9))
        assertEquals(CategoryLimitStatus.SNUG, CategoryLimitCalculator.statusFor(50.0))
        assertEquals(CategoryLimitStatus.TIGHT, CategoryLimitCalculator.statusFor(85.0))
        assertEquals(CategoryLimitStatus.OVER, CategoryLimitCalculator.statusFor(100.0))
    }
}
// End of class: CategoryLimitCalculatorTest
