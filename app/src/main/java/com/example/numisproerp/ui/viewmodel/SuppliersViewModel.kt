package com.numisproerp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.numisproerp.data.dao.PurchaseWithProductName
import com.numisproerp.data.dao.SupplierWithBalance
import com.numisproerp.data.entities.Supplier
import com.numisproerp.data.repository.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SuppliersUiState(
    val suppliers: List<SupplierWithBalance> = emptyList(),
    val purchaseHistory: List<PurchaseWithProductName> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val showAddDialog: Boolean = false
)

@HiltViewModel
class SuppliersViewModel @Inject constructor(
    val repository: Repository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SuppliersUiState())
    val uiState: StateFlow<SuppliersUiState> = _uiState.asStateFlow()

    fun loadSuppliers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            repository.getSuppliersWithBalance().collect { suppliers ->
                _uiState.value = _uiState.value.copy(
                    suppliers = applySearchFilter(suppliers, _uiState.value.searchQuery),
                    isLoading = false
                )
            }
        }
    }

    fun loadPurchaseHistory(supplierId: String) {
        viewModelScope.launch {
            val history = repository.getPurchaseHistoryForSupplier(supplierId)
            _uiState.value = _uiState.value.copy(purchaseHistory = history)
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        viewModelScope.launch {
            repository.getSuppliersWithBalance().collect { suppliers ->
                _uiState.value = _uiState.value.copy(
                    suppliers = applySearchFilter(suppliers, query)
                )
            }
        }
    }

    fun updateSupplier(supplierId: String, name: String, contact: String, type: String, comment: String) {
        viewModelScope.launch {
            val supplier = Supplier(
                supplierId = supplierId,
                name = name,
                contact = contact,
                type = type,
                comment = comment
            )
            repository.updateSupplier(supplier)
            loadSuppliers()
        }
    }

    fun deleteSupplier(supplierId: String) {
        viewModelScope.launch {
            repository.deleteSupplier(supplierId)
            loadSuppliers()
        }
    }

    fun addSupplier(name: String, contact: String, type: String, comment: String) {
        viewModelScope.launch {
            val newSupplier = Supplier(
                supplierId = "SUP-${System.currentTimeMillis()}",
                name = name,
                contact = contact,
                type = type,
                comment = comment
            )
            repository.insertSupplier(newSupplier)
            toggleAddDialog(false)
            loadSuppliers()
        }
    }

    fun toggleAddDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showAddDialog = show)
    }

    private fun applySearchFilter(suppliers: List<SupplierWithBalance>, query: String): List<SupplierWithBalance> {
        if (query.isBlank()) return suppliers
        return suppliers.filter { supplier ->
            supplier.name.contains(query, ignoreCase = true) ||
                    supplier.contact.contains(query, ignoreCase = true) ||
                    supplier.type.contains(query, ignoreCase = true)
        }
    }
}

// ФАБРИКИ ТУТ НЕМАЄ !!! ВОНА В ОКРЕМОМУ ФАЙЛІ SuppliersViewModelFactory.kt
