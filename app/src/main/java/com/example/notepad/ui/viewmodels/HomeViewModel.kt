package com.example.notepad.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.notepad.data.models.Folder
import com.example.notepad.data.repository.FolderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FolderRepository(application)
    
    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders.asStateFlow()
    
    private val _selectedFolders = MutableStateFlow<Set<String>>(emptySet())
    val selectedFolders: StateFlow<Set<String>> = _selectedFolders.asStateFlow()
    
    init {
        loadFolders()
    }
    
    private fun loadFolders() {
        viewModelScope.launch {
            _folders.value = repository.getFolders()
        }
    }
    
    fun createFolder(name: String) {
        viewModelScope.launch {
            repository.createFolder(name)
            loadFolders()
        }
    }
    
    fun toggleFolderSelection(folderId: String) {
        _selectedFolders.value = if (_selectedFolders.value.contains(folderId)) {
            _selectedFolders.value - folderId
        } else {
            _selectedFolders.value + folderId
        }
    }
    
    fun clearSelection() {
        _selectedFolders.value = emptySet()
    }
    
    fun deleteSelectedFolders() {
        viewModelScope.launch {
            _selectedFolders.value.forEach { folderId ->
                repository.deleteFolder(folderId)
            }
            clearSelection()
            loadFolders()
        }
    }
} 