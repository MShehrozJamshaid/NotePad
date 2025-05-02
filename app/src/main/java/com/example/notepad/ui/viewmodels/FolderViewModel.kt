package com.example.notepad.ui.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.notepad.data.models.Folder
import com.example.notepad.data.models.Note
import com.example.notepad.data.repository.FolderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FolderViewModel(
    application: Application,
    private val folderId: String
) : AndroidViewModel(application) {
    private val repository = FolderRepository(application)
    
    private val _folder = MutableStateFlow<Folder?>(null)
    val folder: StateFlow<Folder?> = _folder.asStateFlow()
    
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()
    
    init {
        loadFolder()
        loadNotes()
    }
    
    private fun loadFolder() {
        viewModelScope.launch {
            _folder.value = repository.getFolderById(folderId)
        }
    }
    
    private fun loadNotes() {
        viewModelScope.launch {
            _notes.value = repository.getNotesInFolder(folderId)
        }
    }
    
    fun addImage(imageUri: Uri, title: String? = null) {
        viewModelScope.launch {
            repository.saveImageToFolder(folderId, imageUri, title)
            loadNotes()
            loadFolder() // Reload folder to update cover image if needed
        }
    }
    
    fun addTextNote(title: String, content: String) {
        viewModelScope.launch {
            repository.saveTextNoteToFolder(folderId, title, content)
            loadNotes()
        }
    }
    
    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            repository.deleteNote(folderId, noteId)
            loadNotes()
        }
    }
    
    fun updateFolder(name: String) {
        viewModelScope.launch {
            _folder.value?.let { currentFolder ->
                val updatedFolder = currentFolder.copy(name = name)
                repository.updateFolder(updatedFolder)
                loadFolder()
            }
        }
    }
    
    companion object {
        fun provideFactory(
            application: Application,
            folderId: String
        ): androidx.lifecycle.ViewModelProvider.Factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return FolderViewModel(application, folderId) as T
            }
        }
    }
} 