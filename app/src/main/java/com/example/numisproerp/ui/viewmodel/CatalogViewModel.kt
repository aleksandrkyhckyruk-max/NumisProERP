package com.numisproerp.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.numisproerp.data.entities.CatalogItem
import com.numisproerp.data.repository.CatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Активна вісь сортування каталогу. Кожна вісь має варіант ASC/DESC.
 */
enum class CatalogSortField { NAME, DATE, DENOMINATION, MINTAGE, MATERIAL, SERIES }

/**
 * Активна вісь фільтрації. `category` залишено як окремий "пресет" для
 * сумісності з UX, інші фільтри підключаються через [selectedFilters].
 */
enum class CatalogFilterField { CATEGORY, MATERIAL, SERIES, YEAR, QUALITY }

data class CatalogUiState(
    val items: List<CatalogItem> = emptyList(),
    val categories: List<String> = emptyList(),
    val materials: List<String> = emptyList(),
    val seriesList: List<String> = emptyList(),
    val years: List<String> = emptyList(),
    val qualities: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val sortField: CatalogSortField = CatalogSortField.NAME,
    val sortAscending: Boolean = true,
    /** Мапа поле → обране значення; порожньо = фільтр не активний. */
    val selectedFilters: Map<CatalogFilterField, String> = emptyMap(),
    val showSortDialog: Boolean = false,
    val showFilterDialog: Boolean = false,
    val isDataLoaded: Boolean = false
)

@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val repository: CatalogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    /**
     * Поточна підписка на `itemsFlow`. Кожен виклик `loadItems()` скасовує
     * попередню підписку, щоб уникнути нагромадження collect-ів при кожному
     * заході на CatalogScreen чи зміні фільтра/сортування.
     */
    private var itemsJob: Job? = null

    fun loadItems() {
        itemsJob?.cancel()
        itemsJob = viewModelScope.launch {
            try {
                repository.getAllItems().collect { allItems ->
                    val filtered = applyFilters(allItems)
                    _uiState.value = _uiState.value.copy(
                        items = applySorting(filtered),
                        isLoading = false,
                        isDataLoaded = allItems.isNotEmpty()
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isDataLoaded = false
                )
            }
        }

        // Підвантажуємо набори значень для всіх фільтрів окремою корутиною.
        viewModelScope.launch {
            try {
                val all = try { repository.getAllItemsSync() } catch (e: Exception) { emptyList() }
                if (all.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        categories = all.map { it.category }.filter { it.isNotBlank() }.distinct().sorted(),
                        materials = all.map { it.material }.filter { it.isNotBlank() }.distinct().sorted(),
                        seriesList = all.map { it.series }.filter { it.isNotBlank() }.distinct().sorted(),
                        years = all.mapNotNull { extractYear(it.dateIntroduced) }.distinct().sortedDescending(),
                        qualities = all.map { it.quality }.filter { it.isNotBlank() }.distinct().sorted()
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun selectCategory(category: String) {
        setFilter(CatalogFilterField.CATEGORY, category)
    }

    fun clearCategory() {
        clearFilter(CatalogFilterField.CATEGORY)
    }

    fun setFilter(field: CatalogFilterField, value: String) {
        val newFilters = _uiState.value.selectedFilters.toMutableMap()
        if (value.isBlank()) newFilters.remove(field) else newFilters[field] = value
        _uiState.value = _uiState.value.copy(
            selectedFilters = newFilters,
            isLoading = true
        )
        loadItems()
    }

    fun clearFilter(field: CatalogFilterField) {
        val newFilters = _uiState.value.selectedFilters.toMutableMap()
        newFilters.remove(field)
        _uiState.value = _uiState.value.copy(
            selectedFilters = newFilters,
            isLoading = true
        )
        loadItems()
    }

    fun clearAllFilters() {
        _uiState.value = _uiState.value.copy(
            selectedFilters = emptyMap(),
            isLoading = true
        )
        loadItems()
    }

    fun setSort(field: CatalogSortField, ascending: Boolean) {
        _uiState.value = _uiState.value.copy(sortField = field, sortAscending = ascending)
        loadItems()
    }

    fun toggleSortDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSortDialog = show)
    }

    fun toggleFilterDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showFilterDialog = show)
    }

    fun importExcelFile(context: Context, uri: Uri, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.importExcelFile(context, uri)
            if (result.isSuccess) {
                val count = result.getOrNull() ?: 0
                onComplete(true, "Завантажено $count товарів")
                loadItems()
            } else {
                onComplete(false, result.exceptionOrNull()?.message ?: "Помилка завантаження")
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun applyFilters(items: List<CatalogItem>): List<CatalogItem> {
        val filters = _uiState.value.selectedFilters
        if (filters.isEmpty()) return items
        return items.filter { item ->
            filters.all { (field, value) ->
                when (field) {
                    CatalogFilterField.CATEGORY -> item.category.equals(value, ignoreCase = true)
                    CatalogFilterField.MATERIAL -> item.material.equals(value, ignoreCase = true)
                    CatalogFilterField.SERIES -> item.series.equals(value, ignoreCase = true)
                    CatalogFilterField.YEAR -> extractYear(item.dateIntroduced) == value
                    CatalogFilterField.QUALITY -> item.quality.equals(value, ignoreCase = true)
                }
            }
        }
    }

    private fun applySorting(items: List<CatalogItem>): List<CatalogItem> {
        val state = _uiState.value
        val comparator: Comparator<CatalogItem> = when (state.sortField) {
            CatalogSortField.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            // Дата у форматі "DD.MM.YYYY" або з варіаціями. Парсимо до Long.
            CatalogSortField.DATE -> compareBy { parseDateMillis(it.dateIntroduced) }
            // Номінал — числовий: парсимо подвійні значення (1, 2, 0.25 і т. д.).
            CatalogSortField.DENOMINATION -> compareBy { parseNumber(it.denomination) }
            // Тираж — натуральне число (можуть бути роздільники).
            CatalogSortField.MINTAGE -> compareBy { parseNumber(it.mintage) }
            CatalogSortField.MATERIAL -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.material }
            CatalogSortField.SERIES -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.series }
        }
        val sorted = items.sortedWith(comparator)
        return if (state.sortAscending) sorted else sorted.reversed()
    }

    /** Витягує 4-значний рік з рядка дати (наприклад "12.03.2010" → "2010"). */
    private fun extractYear(date: String): String? {
        val match = Regex("""(\d{4})""").find(date) ?: return null
        return match.groupValues[1]
    }

    /** Парсить рядок дати "DD.MM.YYYY" / "YYYY-MM-DD" / "YYYY" у мс epoch. */
    private fun parseDateMillis(date: String): Long {
        if (date.isBlank()) return Long.MIN_VALUE
        val cleaned = date.trim()
        // DD.MM.YYYY
        Regex("""(\d{1,2})\.(\d{1,2})\.(\d{4})""").matchEntire(cleaned)?.let {
            val (d, m, y) = it.destructured
            return composeDate(y.toInt(), m.toInt(), d.toInt())
        }
        // YYYY-MM-DD
        Regex("""(\d{4})-(\d{1,2})-(\d{1,2})""").matchEntire(cleaned)?.let {
            val (y, m, d) = it.destructured
            return composeDate(y.toInt(), m.toInt(), d.toInt())
        }
        // Фолбек: лише рік.
        Regex("""(\d{4})""").find(cleaned)?.let {
            return composeDate(it.groupValues[1].toInt(), 1, 1)
        }
        return Long.MIN_VALUE
    }

    private fun composeDate(year: Int, month: Int, day: Int): Long {
        val cal = java.util.Calendar.getInstance()
        cal.clear()
        cal.set(year, (month - 1).coerceIn(0, 11), day.coerceIn(1, 31))
        return cal.timeInMillis
    }

    private fun parseNumber(raw: String): Double {
        if (raw.isBlank()) return Double.NEGATIVE_INFINITY
        // Видаляємо все, крім цифр, крапок і ком.
        val cleaned = raw.replace(Regex("""[^\d.,]"""), "").replace(",", ".")
        // Якщо значень кілька крапок — лишаємо лише першу.
        val firstDot = cleaned.indexOf('.')
        val normalized = if (firstDot >= 0) {
            cleaned.substring(0, firstDot + 1) + cleaned.substring(firstDot + 1).replace(".", "")
        } else cleaned
        return normalized.toDoubleOrNull() ?: Double.NEGATIVE_INFINITY
    }
}
