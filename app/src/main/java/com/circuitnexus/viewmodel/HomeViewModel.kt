package com.circuitnexus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.circuitnexus.data.CircuitRepository
import com.circuitnexus.model.Circuit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Lista de circuitos guardados con búsqueda y filtro de favoritos.
 */
class HomeViewModel(private val repository: CircuitRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _onlyFavorites = MutableStateFlow(false)
    val onlyFavorites: StateFlow<Boolean> = _onlyFavorites

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    val circuits: StateFlow<List<Circuit>> =
        combine(repository.allCircuits, _searchQuery, _onlyFavorites) { list, query, favoritesOnly ->
            list.asSequence()
                .filter { !favoritesOnly || it.isFavorite }
                .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
                .sortedByDescending { it.updatedAt }
                .toList()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(value: String) {
        _searchQuery.value = value
    }

    fun toggleFavoritesFilter() {
        _onlyFavorites.value = !_onlyFavorites.value
    }

    fun createNewCircuit(onCreated: (Long) -> Unit) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            val id = repository.createCircuit("Nuevo Circuito")
            _busy.value = false
            onCreated(id)
        }
    }

    fun duplicateCircuit(circuit: Circuit) {
        viewModelScope.launch { repository.duplicateCircuit(circuit.id) }
    }

    fun deleteCircuit(circuit: Circuit) {
        viewModelScope.launch { repository.deleteCircuit(circuit.id) }
    }

    fun toggleFavorite(circuit: Circuit) {
        viewModelScope.launch { repository.toggleFavorite(circuit.id, !circuit.isFavorite) }
    }
}
