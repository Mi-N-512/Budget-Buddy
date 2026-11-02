package com.budgetbuddy

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.graphics.Color
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import com.budgetbuddy.databinding.ActivityAnalyticsBinding
import com.google.android.material.color.MaterialColors
import com.github.anastr.speedviewlib.components.Section
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.Locale


/*
 * Start of class
 * Name of class and related classes (parent/child classes): AnalyticsActivity
 * Parent class: BaseActivity; child classes: none; related classes: AnalyticsCalculator, LocalDataStore, GaugePalette, and TransactionRepo.
 * What the class does: Renders monthly analytics, budget gauge, summaries, and category charts.
 * What's important to other classes, if applicable: It must preserve BaseActivity appearance behavior and use LocalDataStore as the offline source of truth.
 * Code with comments begins below.
 */
class AnalyticsActivity : BaseActivity() {
    private lateinit var binding: ActivityAnalyticsBinding
    private var startDate = ""
    private var endDate = ""
    private var isExpanded = false
    private var currentMonth: Calendar = Calendar.getInstance()
    private val formattedMonth: String
        get() = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentMonth.time)
    private lateinit var repo: TransactionRepo
    private lateinit var localData: LocalDataStore
    private var showMaxInfo = false
    private var firstResume = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalyticsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repo = TransactionRepo(this)
        localData = LocalDataStore(this)
        displayCurrency()
        binding.imgBeetleJuice.contentDescription = localData.buddyName
        setupGaugeChart(formattedMonth)
        loadCategoryLimits(formattedMonth)
        loadBarGraphData(formattedMonth, showMaxInfo)

        updateMonthDisplay()
        setupExpandableAnalytics()

        binding.btnShowMaxInfo.setOnClickListener{
            showMaxInfo = !showMaxInfo
            if (showMaxInfo) binding.btnShowMaxInfo.text = getString(R.string.reset) else binding.btnShowMaxInfo.text = getString(R.string.show_on_graph)
            loadBarGraphData(binding.tvCurrentMonth.text.toString(), showMaxInfo)
        }
        binding.btnBack.setOnClickListener{
            clearText()
            binding.categoryBarGraph.clear()
            currentMonth.add(Calendar.MONTH, -1) //prev month
            loadAnalyticsView()
        }
        binding.btnForward.setOnClickListener{
            clearText()
            binding.categoryBarGraph.clear()
            currentMonth.add(Calendar.MONTH, 1) //next month
            loadAnalyticsView()
        }

        setupDatePickers()
        //navigation
        appNavigationSetup()
    }//end oncreate

    override fun onResume() {
        super.onResume()
        if (firstResume) {
            firstResume = false
        } else if (::localData.isInitialized) {
            loadAnalyticsView()
        }
    }

    private fun appNavigationSetup() {
        AppNavigation.bind(this, binding.bottomNavView, R.id.nav_analytics)
    }

    private fun updateMonthDisplay() {
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val formattedMonth = monthFormat.format(currentMonth.time)
        binding.tvCurrentMonth.text = formattedMonth
    }

    private fun setupDatePickers() {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = java.util.Calendar.getInstance()

        binding.tvStartDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    startDate = fmt.format(java.util.Calendar.getInstance().apply {
                        set(year, month, day)
                    }.time)
                    binding.tvStartDate.text = startDate
                    checkAndReloadGraph()
                },
                today.get(java.util.Calendar.YEAR),
                today.get(java.util.Calendar.MONTH),
                today.get(java.util.Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.tvEndDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    endDate = fmt.format(java.util.Calendar.getInstance().apply {
                        set(year, month, day)
                    }.time)
                    binding.tvEndDate.text = endDate
                    checkAndReloadGraph()
                },
                today.get(java.util.Calendar.YEAR),
                today.get(java.util.Calendar.MONTH),
                today.get(java.util.Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun clearDatePickers(){
        binding.tvStartDate.setText(getString(R.string.start_date))
        binding.tvEndDate.setText(getString(R.string.end_date))
        startDate = ""
        endDate = ""
    }

    private fun setupExpandableAnalytics(){
        //expand
        binding.btnMinMaxHeader.setOnClickListener{
            isExpanded = !isExpanded
            binding.hiddenAnalytics.visibility = if (isExpanded) View.VISIBLE else View.GONE
            binding.btnMinMaxHeader.setCompoundDrawablesWithIntrinsicBounds(
                0, 0,
                if (isExpanded) R.drawable.ic_upward_arrow else R.drawable.ic_downward_arrow,
                0
            )
        }
    }

    private fun clearText(){
        binding.tvFeedBack.text = ""
        binding.tvPercentage.text= ""
        binding.tvMinGoal.text = ""
        binding.tvMaxGoal.text = ""
    }

    private fun loadAnalyticsView(){
        displayCurrency()
        updateMonthDisplay()//show month
        clearDatePickers()
        checkAndReloadGraph()
        setupGaugeChart(binding.tvCurrentMonth.text.toString())
        loadCategoryLimits(binding.tvCurrentMonth.text.toString())
        loadBarGraphData(binding.tvCurrentMonth.text.toString(), showMaxInfo)
    }

    /** Lists every category with its own limit this month, coloured with the gauge palette. */
    private fun loadCategoryLimits(month: String) {
        val list = binding.categoryLimitsList
        list.removeAllViews()
        val monthKey = runCatching {
            SimpleDateFormat("yyyy-MM", Locale.US).format(
                requireNotNull(SimpleDateFormat("MMMM yyyy", Locale.getDefault()).parse(month))
            )
        }.getOrNull()
        val limits = localData.getBudget(month)?.categories.orEmpty()
            .mapValues { (_, category) -> category.allocation }
        val spent = localData.getTransactions()
            .filter { !it.isIncome && monthKey != null && it.date.startsWith(monthKey) }
            .groupBy(Transaction::categoryId)
            .mapValues { (_, values) -> values.sumOf(Transaction::amount) }
        val rows = CategoryLimitCalculator.rows(limits, spent)
        binding.tvCategoryLimitsEmpty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE

        val symbol = localData.currencySymbol
        val palette = localData.gaugePalette
        val surface = MaterialColors.getColor(binding.root, R.attr.budgetSurfaceColor)
        val iconBackgrounds = listOf(
            R.color.pastelPink, R.color.pastelOrange, R.color.pastelYellow,
            R.color.pastelGreen, R.color.pastelBlue, R.color.pastelPurple
        )
        rows.forEachIndexed { index, row ->
            val item = layoutInflater.inflate(R.layout.item_category_limit, list, false)
            RuntimePaletteApplier.applyIfCustom(item)
            val name = localData.categoryDisplayName(row.categoryId)
            val money = { value: Double -> getString(R.string.money_amount, symbol, value) }
            val color = AnalyticsCalculator.gaugeColor(row.percentage, palette)

            val icon = item.findViewById<android.widget.ImageView>(R.id.imgLimitIcon)
            CategoryIconCatalog.bind(icon, localData.categoryIcon(row.categoryId))
            icon.background = androidx.core.content.ContextCompat
                .getDrawable(this, R.drawable.circle_background)?.mutate()?.apply {
                    setTint(androidx.core.content.ContextCompat.getColor(this@AnalyticsActivity, iconBackgrounds[index % iconBackgrounds.size]))
                }
            item.findViewById<android.widget.TextView>(R.id.tvLimitName).text = name
            item.findViewById<android.widget.TextView>(R.id.tvLimitAmount).text =
                getString(R.string.category_limit_spent_of, money(row.spent), money(row.limit))
            item.findViewById<android.widget.ProgressBar>(R.id.limitProgress).apply {
                progressTintList = android.content.res.ColorStateList.valueOf(color)
                progressBackgroundTintList = android.content.res.ColorStateList.valueOf(ColorUtils.setAlphaComponent(color, 60))
                progress = AnalyticsCalculator.gaugeSpeed(row.percentage).toInt()
            }
            item.findViewById<android.widget.TextView>(R.id.tvLimitStatus).apply {
                text = when (row.status) {
                    CategoryLimitCalculator.CategoryLimitStatus.ROOMY -> getString(R.string.category_limit_roomy, money(row.remaining))
                    CategoryLimitCalculator.CategoryLimitStatus.SNUG -> getString(R.string.category_limit_snug, money(row.remaining))
                    CategoryLimitCalculator.CategoryLimitStatus.TIGHT -> getString(R.string.category_limit_tight, money(row.remaining))
                    CategoryLimitCalculator.CategoryLimitStatus.OVER -> getString(R.string.category_limit_over, money(-row.remaining))
                }
                setTextColor(readableAnalyticsColor(color, surface))
            }
            item.contentDescription = getString(
                R.string.category_limit_row_description,
                name,
                AnalyticsCalculator.spentPercentage(row.spent, row.limit)
            )
            list.addView(item)
        }
    }

    private fun loadBarGraphData(selectedMonth:String, showMaxInfo: Boolean){
        //convert display month to start and end date of month
        val displayedFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val parsedMonth = runCatching { displayedFormat.parse(selectedMonth) }.getOrNull()
        if (parsedMonth == null) {
            setupBarGraph(emptyList())
            return
        }
        val calendar = Calendar.getInstance()
        calendar.time = parsedMonth
        //first day of month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val firstDay = outputFormat.format(calendar.time)
        //last day of month
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val lastDay = outputFormat.format(calendar.time)

        val budgetLimits = localData.getBudget(selectedMonth)?.categories.orEmpty()
            .mapValues { (_, category) -> category.allocation }
        val usingCustomRange = startDate.isNotBlank() && endDate.isNotBlank()
        val onTransactionFetched: (List<Transaction>) -> Unit = { transactions ->
            val totals = transactions.filterNot(Transaction::isIncome)
                .groupBy(Transaction::categoryId)
                .mapValues { (_, values) -> values.sumOf(Transaction::amount) }
            // Biggest spender first. A horizontal chart draws index 0 at the bottom, so the
            // list is reversed to put the biggest bar at the top where people start reading.
            val bars = (totals.keys + budgetLimits.filterValues { it > 0.0 }.keys).distinct()
                .map { id ->
                    CategoryBar(
                        label = localData.categoryDisplayName(id),
                        spent = totals[id] ?: 0.0,
                        // Limits belong to one month; a custom date range has no fair limit to compare with.
                        limit = budgetLimits[id]?.takeIf { it > 0.0 && !usingCustomRange }
                    )
                }
                .filter { showMaxInfo || it.spent > 0.0 }
                .sortedByDescending(CategoryBar::spent)
                .reversed()
            if (showMaxInfo) setupStackedBarGraph(bars) else setupBarGraph(bars)
        }
        val onError: (Exception) -> Unit = { Log.e("BarGraph", "Error loading local transactions", it) }
        if (usingCustomRange) {
            repo.fetchInRange(startDate, endDate, onTransactionFetched, onError)
        } else {
            repo.fetchInRange(firstDay, lastDay, onTransactionFetched, onError)
        }
    }//end load bar graph data

    /** One row of the category chart; limit is null when the category has none to compare with. */
    private data class CategoryBar(val label: String, val spent: Double, val limit: Double?)

    /*This code uses MPAndroidChart (https://github.com/PhilJay/MPAndroidChart)
   * Licensed under the Apache License, Version 2.0
   * Copyright 2015 Philipp Jahoda*/
    //shows just plain category expenditure, coloured like the Category limits card
    private fun setupBarGraph(bars: List<CategoryBar>) {
        val barGraph = binding.categoryBarGraph
        barGraph.clear()
        if (bars.isEmpty()) {
            showEmptyChart()
            return
        }
        val entries = bars.mapIndexed { index, bar -> BarEntry(index.toFloat(), bar.spent.toFloat()) }
        val dataSet = BarDataSet(entries, getString(R.string.chart_spent_label))
        val palette = localData.gaugePalette
        val neutral = MaterialColors.getColor(binding.root, R.attr.budgetPrimaryColor)
        dataSet.colors = bars.map { bar ->
            bar.limit?.let { AnalyticsCalculator.gaugeColor(AnalyticsCalculator.spentPercentageExact(bar.spent, it), palette) }
                ?: neutral
        }
        styleChart(dataSet, bars.map(CategoryBar::label), showLegend = false)
    }

    /*This code uses MPAndroidChart (https://github.com/PhilJay/MPAndroidChart)
  * Licensed under the Apache License, Version 2.0
  * Copyright 2015 Philipp Jahoda*/
    //shows each category's spending stacked against what is left of its limit
    private fun setupStackedBarGraph(bars: List<CategoryBar>) {
        val barGraph = binding.categoryBarGraph
        barGraph.clear()
        if (bars.isEmpty()) {
            showEmptyChart()
            return
        }
        val entries = bars.mapIndexed { index, bar ->
            val left = bar.limit?.let { (it - bar.spent).coerceAtLeast(0.0) } ?: 0.0
            BarEntry(index.toFloat(), floatArrayOf(bar.spent.toFloat(), left.toFloat()))
        }
        val dataSet = BarDataSet(entries, "")
        val gaugePalette = localData.gaugePalette
        dataSet.setColors(gaugePalette.bad, gaugePalette.good) // spent, remaining
        dataSet.stackLabels = arrayOf(getString(R.string.chart_spent_label), getString(R.string.chart_left_label))
        styleChart(dataSet, bars.map(CategoryBar::label), showLegend = true)
        // Each stack segment would print its own amount on top of the next one; the
        // Category limits card above already lists the exact figures.
        dataSet.setDrawValues(false)
        binding.categoryBarGraph.invalidate()
    }

    /** Shared look for both chart modes: every category labelled, sized to fit, no hidden zoom. */
    private fun styleChart(dataSet: BarDataSet, labels: List<String>, showLegend: Boolean) {
        val barGraph = binding.categoryBarGraph
        val chartTextColor = MaterialColors.getColor(binding.root, R.attr.budgetTextColor)
        val font = ResourcesCompat.getFont(applicationContext, R.font.outfit_regular)
        val symbol = localData.currencySymbol
        dataSet.setDrawValues(true)
        dataSet.valueTextColor = chartTextColor
        dataSet.valueTextSize = 13f
        dataSet.valueTypeface = font
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String =
                if (value <= 0f) "" else getString(R.string.money_amount, symbol, value.toDouble())
        }

        val barData = BarData(dataSet)
        barData.barWidth = 0.6f
        barGraph.data = barData
        barGraph.setFitBars(true)

        // Grow with the number of categories so no label is skipped for lack of room.
        val density = resources.displayMetrics.density
        barGraph.layoutParams = barGraph.layoutParams.apply {
            height = ((CHART_BASE_HEIGHT_DP + CHART_ROW_HEIGHT_DP * labels.size) * density).toInt()
        }

        barGraph.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            textSize = 14f
            textColor = chartTextColor
            typeface = font
            granularity = 1f
            isGranularityEnabled = true
            setLabelCount(labels.size, false)
            axisMinimum = -0.5f
            axisMaximum = labels.size - 0.5f
            labelRotationAngle = 0f
            valueFormatter = IndexAxisValueFormatter(labels)
        }
        barGraph.axisLeft.apply {
            setDrawGridLines(false)
            textSize = 12f
            textColor = chartTextColor
            typeface = font
            axisMinimum = 0f
            // Room on the right for the money label at the end of the longest bar.
            spaceTop = 25f
        }
        barGraph.axisRight.isEnabled = false
        barGraph.legend.apply {
            isEnabled = showLegend
            textColor = chartTextColor
            typeface = font
            textSize = 13f
        }
        barGraph.description.isEnabled = false
        barGraph.setScaleEnabled(false)
        barGraph.isDragEnabled = false
        barGraph.setPinchZoom(false)
        barGraph.setTouchEnabled(false)
        barGraph.fitScreen()
        barGraph.animateY(800)
        barGraph.invalidate()
    }

    private fun showEmptyChart() {
        val barGraph = binding.categoryBarGraph
        barGraph.setNoDataText(getString(R.string.chart_empty))
        barGraph.setNoDataTextColor(MaterialColors.getColor(binding.root, R.attr.budgetTextColor))
        barGraph.setNoDataTextTypeface(ResourcesCompat.getFont(applicationContext, R.font.outfit_regular))
        barGraph.layoutParams = barGraph.layoutParams.apply {
            height = (CHART_EMPTY_HEIGHT_DP * resources.displayMetrics.density).toInt()
        }
        barGraph.invalidate()
    }

    //helper function to check if date range was selected
    private fun checkAndReloadGraph(){
        if(startDate.isNotBlank() && endDate.isNotBlank()){
            val displayedMonth = binding.tvCurrentMonth.text.toString()
            loadBarGraphData(displayedMonth, showMaxInfo)
        }
    }

    /*Gauge chart visualisation by SpeedView (https://github.com/anastr/SpeedView)
    * © 2017 Anas Altair – Licensed under Apache 2.0*/
    private fun setupGaugeChart(month:String){
        val gauge = binding.minMaxGauge
        val gaugeBackground = MaterialColors.getColor(binding.root, R.attr.budgetSurfaceColor)
        val indicatorColor = if (
            ColorUtils.calculateContrast(Color.WHITE, gaugeBackground) >=
            ColorUtils.calculateContrast(Color.BLACK, gaugeBackground)
        ) Color.WHITE else Color.BLACK
        // Keep the indicator created and sized by the XML. Replacing it at runtime can
        // create a zero-length needle on some devices/themes, which made AMOLED look empty.
        gauge.indicator.color = indicatorColor
        gauge.centerCircleColor = indicatorColor
        gauge.clearSections()
        /*percentage spent = amount spent / max Goal * 100
        minGoal = min goal from budget
        max goal = amount spent from budget */
        val budget = localData.getBudget(month)
        val monthKey = runCatching {
            val display = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            val storage = SimpleDateFormat("yyyy-MM", Locale.US)
            storage.format(requireNotNull(display.parse(month)))
        }.getOrDefault("")
        val balance = localData.getBalance(monthKey)
        val maximumGoal = localData.getEffectiveSpendingLimit(month)
        val symbol = localData.currencySymbol
        val gaugePalette = localData.gaugePalette
        val analyticsSurface = MaterialColors.getColor(binding.root, R.attr.budgetSurfaceColor)
        val goodText = readableAnalyticsColor(gaugePalette.good, analyticsSurface)
        val badText = readableAnalyticsColor(gaugePalette.bad, analyticsSurface)
        binding.tvAnalyticsIncome.text = getString(R.string.analytics_income_amount, symbol, balance.totalIncome)
        binding.tvAnalyticsIncome.setTextColor(goodText)
        binding.tvAnalyticsExpenses.text = getString(R.string.analytics_expenses_amount, symbol, balance.totalExpenses)
        binding.tvAnalyticsExpenses.setTextColor(badText)
        binding.tvAnalyticsCashFlow.text = getString(
            if (balance.closingBalance >= 0.0) R.string.analytics_positive_cash_flow else R.string.analytics_negative_cash_flow,
            symbol,
            kotlin.math.abs(balance.closingBalance)
        )
        binding.tvAnalyticsCashFlow.setTextColor(if (balance.closingBalance >= 0.0) goodText else badText)
        @SuppressLint("SetTextI18n")
        if (maximumGoal <= 0.0) {
                    gauge.clearSections()
                    gauge.addSections(
                        Section(
                            0f,
                            1f,
                            MaterialColors.getColor(binding.root, R.attr.budgetBackgroundColor),
                            gauge.speedometerWidth
                        )
                    )
                    gauge.speedTo(0f, 1000)
                    binding.btnMinMaxHeader.setText(R.string.spending_limit_not_set_header)
                    binding.tvMinGoal.setText(R.string.no_money)
                    binding.tvMaxGoal.setText(R.string.no_money)
                    binding.imgBeetleJuice.setImageResource(R.drawable.happy_buddy)
                    binding.tvFeedBack.setText(R.string.analytics_no_budget_mood)
                    binding.tvAnalyticsBudgetRemaining.setText(R.string.analytics_remaining_not_available)
                    binding.tvAnalyticsBudgetRemaining.setTextColor(
                        MaterialColors.getColor(binding.root, R.attr.budgetTextColor)
                    )
        } else {
                    val categoryTotal = budget?.budgetAmount ?: 0.0
                    // Always calculate from the transaction source of truth. Editing or replacing
                    // a budget must never erase the spending already recorded for that month.
                    val totalSpent = localData.getMonthlyExpenseTotal(month)
                    val percentageSpent = AnalyticsCalculator.spentPercentageExact(totalSpent, maximumGoal)

                    val allocationPercentage = BudgetLimitCalculator.allocationPercentageExact(categoryTotal, maximumGoal)
                    // Spending 0–50% leaves at least half the budget, 50–85% leaves
                    // 15–49%, and anything beyond 85% leaves less than 15%.
                    gauge.addSections(
                        Section(0f, 0.5f, gaugePalette.good, gauge.speedometerWidth),
                        Section(0.5f, 0.85f, gaugePalette.okay, gauge.speedometerWidth),
                        Section(0.85f, 1f, gaugePalette.bad, gauge.speedometerWidth)
                    )

                    // Budster reacts to the percentage of the monthly budget still remaining.
                    val buddyPic = binding.imgBeetleJuice
                    when (AnalyticsCalculator.buddyMood(totalSpent, maximumGoal)) {
                        AnalyticsCalculator.BuddyMood.HAPPY -> {
                            buddyPic.setImageResource(R.drawable.neutral_buddy_1)
                            binding.tvFeedBack.setText(R.string.budget_mood_happy)
                        }
                        AnalyticsCalculator.BuddyMood.NEUTRAL -> {
                            buddyPic.setImageResource(R.drawable.happy_buddy)
                            binding.tvFeedBack.setText(R.string.budget_mood_neutral)
                        }
                        AnalyticsCalculator.BuddyMood.ANGRY -> {
                            buddyPic.setImageResource(R.drawable.angry_buddy)
                            binding.tvFeedBack.setText(R.string.budget_mood_angry)
                        }
                    }
                    //set gauge value
                    gauge.speedTo(AnalyticsCalculator.gaugeSpeed(percentageSpent), 1000)
                    //to display information to user in view
                    binding.btnMinMaxHeader.text = getString(
                        R.string.spending_limit_used_detail,
                        percentageSpent,
                        symbol,
                        maximumGoal
                    )
                    binding.tvMinGoal.text = getString(R.string.plain_decimal_amount, categoryTotal)
                    binding.tvMaxGoal.text = getString(R.string.plain_decimal_amount, maximumGoal)
                    binding.tvPercentage.text = getString(R.string.category_budget_percentage, allocationPercentage)
                    val remaining = maximumGoal - totalSpent
                    binding.tvAnalyticsBudgetRemaining.text = if (remaining >= 0.0) {
                        getString(R.string.analytics_remaining_amount, symbol, remaining)
                    } else {
                        getString(R.string.analytics_over_limit_amount, symbol, -remaining)
                    }
                    val statusColor = AnalyticsCalculator.gaugeColor(percentageSpent, gaugePalette)
                    // The remaining amount must match the gauge band exactly in every theme.
                    binding.tvAnalyticsBudgetRemaining.setTextColor(statusColor)
        }

    }//end setupGaugeChart

    //fetching and displaying currency
    private fun displayCurrency() {
        binding.tvCurrency1.text = localData.currencySymbol
        binding.tvCurrency2.text = localData.currencySymbol
    }

    private fun readableAnalyticsColor(color: Int, surface: Int): Int {
        if (ColorUtils.calculateContrast(color, surface) >= 4.5) return color
        val target = if (ColorUtils.calculateLuminance(surface) < 0.45) Color.WHITE else Color.BLACK
        for (step in 1..10) {
            val candidate = ColorUtils.blendARGB(color, target, step / 10f)
            if (ColorUtils.calculateContrast(candidate, surface) >= 4.5) return candidate
        }
        return target
    }


    companion object {
        private const val CHART_BASE_HEIGHT_DP = 80
        private const val CHART_ROW_HEIGHT_DP = 52
        private const val CHART_EMPTY_HEIGHT_DP = 120
    }
}
// End of class: AnalyticsActivity

/* Gauge sections mirror Budster's mood thresholds based on monthly budget remaining. */
