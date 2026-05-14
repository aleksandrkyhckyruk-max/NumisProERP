package com.numisproerp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.numisproerp.data.dao.ClientForSelection
import com.numisproerp.data.dao.ProductInStock
import com.numisproerp.data.entities.Sale
import com.numisproerp.data.repository.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SaleUiState(
    val clients: List<ClientForSelection> = emptyList(),
    val products: List<ProductInStock> = emptyList(),
    val isLoading: Boolean = false,
    val selectedClientId: String = "",
    val clientSearchQuery: String = "",
    val filteredClients: List<ClientForSelection> = emptyList(),
    val showSuccessMessage: Boolean = false,
    val errorMessage: String = ""
)

@HiltViewModel
class SaleViewModel @Inject constructor(
    val repository: Repository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SaleUiState())
    val uiState: StateFlow<SaleUiState> = _uiState.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val clients = repository.getClientsForSelection()
            val productsFlow = repository.getProductsInStock()
            val products = productsFlow.first()

            _uiState.value = _uiState.value.copy(
                clients = clients,
                products = products,
                isLoading = false
            )
        }
    }

    fun updateClientSearchQuery(query: String) {
        val filtered = if (query.isNotBlank()) {
            _uiState.value.clients.filter { client ->
                client.name.contains(query, ignoreCase = true)
            }
        } else {
            emptyList()
        }
        _uiState.value = _uiState.value.copy(
            clientSearchQuery = query,
            filteredClients = filtered
        )
    }

    fun updateSelectedClient(clientId: String) {
        _uiState.value = _uiState.value.copy(
            selectedClientId = clientId,
            clientSearchQuery = "",
            filteredClients = emptyList()
        )
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(showSuccessMessage = false)
    }
}

// ФАБРИКИ ТУТ НЕМАЄ !!! ВОНА В ОКРЕМОМУ ФАЙЛІ SaleViewModelFactory.kt
