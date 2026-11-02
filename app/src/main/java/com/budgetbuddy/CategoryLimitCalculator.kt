package com.budgetbuddy

/*
 * Start of class
 * Name of class and related classes (parent/child classes): CategoryLimitCalculator
 * Parent class: Any; child classes: CategoryLimitRow, CategoryLimitStatus; related classes: AnalyticsActivity, Budget, BudgetCategory, and AnalyticsCalculator.
 * What the class does: Turns a month's category budgets and spending into per-category limit rows for Analytics.
 * What's important to other classes, if applicable: Its status bands match the gauge (50% and 85%), so category colours agree with the main gauge.
 * Code with comments begins below.
 */
object CategoryLimitCalculator {
    /*
     * Start of class
     * Name of class and related classes (parent/child classes): CategoryLimitStatus
     * Parent class: Enum; child classes: none; related classes: CategoryLimitCalculator and AnalyticsActivity.
     * What the class does: Names how comfortably a category sits inside its own limit.
     * What's important to other classes, if applicable: AnalyticsActivity picks Budster's line from this value.
     * Code with comments begins below.
     */
    enum class CategoryLimitStatus { ROOMY, SNUG, TIGHT, OVER }
    // End of class: CategoryLimitStatus

    /*
     * Start of class
     * Name of class and related classes (parent/child classes): CategoryLimitRow
     * Parent class: Any; child classes: none; related classes: CategoryLimitCalculator and AnalyticsActivity.
     * What the class does: Holds one category's limit, spending and status.
     * What's important to other classes, if applicable: remaining is negative when the category is over its limit.
     * Code with comments begins below.
     */
    data class CategoryLimitRow(
        val categoryId: String,
        val limit: Double,
        val spent: Double,
        val percentage: Double,
        val remaining: Double,
        val status: CategoryLimitStatus
    )
    // End of class: CategoryLimitRow

    /**
     * One row per category with a positive limit, most-used first.
     * [limits] maps category id to its limit; [spentByCategory] maps category id to expenses.
     */
    fun rows(limits: Map<String, Double>, spentByCategory: Map<String, Double>): List<CategoryLimitRow> =
        limits.filterValues { it.isFinite() && it > 0.0 }
            .map { (id, limit) ->
                val spent = spentByCategory[id]?.takeIf { it.isFinite() && it > 0.0 } ?: 0.0
                val percentage = AnalyticsCalculator.spentPercentageExact(spent, limit)
                CategoryLimitRow(id, limit, spent, percentage, limit - spent, statusFor(percentage))
            }
            .sortedWith(compareByDescending<CategoryLimitRow> { it.percentage }.thenBy { it.categoryId })

    fun statusFor(percentage: Double): CategoryLimitStatus = when {
        percentage >= 100.0 -> CategoryLimitStatus.OVER
        percentage >= 85.0 -> CategoryLimitStatus.TIGHT
        percentage >= 50.0 -> CategoryLimitStatus.SNUG
        else -> CategoryLimitStatus.ROOMY
    }
}
// End of class: CategoryLimitCalculator
