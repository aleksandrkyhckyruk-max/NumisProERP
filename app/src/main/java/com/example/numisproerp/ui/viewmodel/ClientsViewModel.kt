package com.numisproerp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.numisproerp.data.dao.ClientWithBalance
import com.numisproerp.data.dao.SaleWithProductName
import com.numisproerp.data.entities.Client
import com.numisproerp.data.repository.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ClientsUiState(
    val clients: List<ClientWithBalance> = emptyList(),
    val purchaseHistory: List<SaleWithProductName> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val showAddDialog: Boolean = false,
    val showSuccessMessage: Boolean = false,
    val errorMessage: String = ""
)

@HiltViewModel
class ClientsViewModel @Inject constructor(
    val repository: Repository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClientsUiState())
    val uiState: StateFlow<ClientsUiState> = _uiState.asStateFlow()

    fun loadClients() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val clientsFlow = repository.getClientsWithBalance()
            val clients = clientsFlow.first()

            val filteredClients = if (_uiState.value.searchQuery.isNotEmpty()) {
                clients.filter { client ->
                    client.name.contains(_uiState.value.searchQuery, ignoreCase = true) ||
                            client.phone.contains(_uiState.value.searchQuery, ignoreCase = true) ||
                            client.city.contains(_uiState.value.searchQuery, ignoreCase = true)
                }
            } else {
                clients
            }

            _uiState.value = _uiState.value.copy(
                clients = filteredClients,
                isLoading = false
            )
        }
    }

    fun loadPurchaseHistory(clientId: String) {
        viewModelScope.launch {
            val history = repository.getSalesHistoryForClient(clientId)
            _uiState.value = _uiState.value.copy(purchaseHistory = history)
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        loadClients()
    }

    fun toggleAddDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showAddDialog = show)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(showSuccessMessage = false)
    }

    fun addClient(name: String, phone: String, telegram: String, city: String, notes: String) {
        viewModelScope.launch {
            if (name.isBlank()) {
                _uiState.value = _uiState.value.copy(errorMessage = "Введіть ім'я клієнта")
                return@launch
            }

            val client = Client(
                clientId = "CLI-${System.currentTimeMillis()}",
                name = name,
                phone = phone,
                telegram = telegram,
                city = city,
                notes = notes
            )

            repository.insertClient(client)

            _uiState.value = _uiState.value.copy(
                showAddDialog = false,
                showSuccessMessage = true,
                errorMessage = ""
            )

            loadClients()
        }
    }

    fun updateClient(clientId: String, name: String, phone: String, telegram: String, city: String, notes: String) {
        viewModelScope.launch {
            val client = Client(
                clientId = clientId,
                name = name,
                phone = phone,
                telegram = telegram,
                city = city,
                notes = notes
            )
            repository.updateClient(client)
            loadClients()
        }
    }

    fun deleteClient(clientId: String) {
        viewModelScope.launch {
            repository.deleteClient(clientId)
            loadClients()
        }
    }
}

// ФАБРИКИ ТУТ НЕМАЄ !!! ВОНА В ОКРЕМОМУ ФАЙЛІ ClientsViewModelFactory.kt
