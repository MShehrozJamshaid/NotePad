package com.example.notepad.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.notepad.data.models.Folder
import com.example.notepad.data.models.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import org.json.JSONObject

class FolderRepository(private val context: Context) {
    private val foldersDir = File(context.filesDir, "folders")
    
    init {
        if (!foldersDir.exists()) {
            foldersDir.mkdirs()
        }
    }
    
    private fun saveFolderMetadata(folder: Folder) {
        val metadataFile = File(File(foldersDir, folder.id), "metadata.json")
        val json = JSONObject().apply {
            put("id", folder.id)
            put("name", folder.name)
            put("createdAt", folder.createdAt)
            folder.coverImageUri?.let { put("coverImageUri", it) }
        }
        metadataFile.parentFile?.mkdirs()
        metadataFile.writeText(json.toString())
    }
    
    private fun loadFolderMetadata(folderDir: File): Folder? {
        val metadataFile = File(folderDir, "metadata.json")
        if (!metadataFile.exists()) return null
        
        return try {
            val json = JSONObject(metadataFile.readText())
            Folder(
                id = json.getString("id"),
                name = json.getString("name"),
                createdAt = json.getLong("createdAt"),
                coverImageUri = if (json.has("coverImageUri")) json.getString("coverImageUri") else null
            )
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun createFolder(name: String): Folder = withContext(Dispatchers.IO) {
        val folder = Folder(name = name)
        val folderDir = File(foldersDir, folder.id)
        folderDir.mkdirs()
        saveFolderMetadata(folder)
        folder
    }
    
    suspend fun getFolders(): List<Folder> = withContext(Dispatchers.IO) {
        foldersDir.listFiles()?.mapNotNull { folderDir ->
            loadFolderMetadata(folderDir)
        }?.sortedByDescending { it.createdAt } ?: emptyList()
    }
    
    suspend fun getFolderById(folderId: String): Folder? = withContext(Dispatchers.IO) {
        val folderDir = File(foldersDir, folderId)
        if (folderDir.exists()) {
            loadFolderMetadata(folderDir)
        } else null
    }
    
    suspend fun updateFolder(folder: Folder) = withContext(Dispatchers.IO) {
        saveFolderMetadata(folder)
    }
    
    suspend fun deleteFolder(folderId: String): Boolean = withContext(Dispatchers.IO) {
        val folderFile = File(foldersDir, folderId)
        folderFile.deleteRecursively()
    }
    
    suspend fun saveImageToFolder(folderId: String, imageUri: Uri, title: String? = null): Note = withContext(Dispatchers.IO) {
        val folderFile = File(foldersDir, folderId)
        if (!folderFile.exists()) {
            folderFile.mkdir()
        }
        
        val fileName = "${System.currentTimeMillis()}.jpg"
        val destinationFile = File(folderFile, fileName)
        
        context.contentResolver.openInputStream(imageUri)?.use { input ->
            FileOutputStream(destinationFile).use { output ->
                input.copyTo(output)
            }
        }
        
        val note = Note(
            folderId = folderId,
            title = title ?: fileName,
            imageUri = destinationFile.absolutePath
        )
        
        // Update folder cover if it's the first image
        val folder = getFolderById(folderId)
        if (folder != null && folder.coverImageUri == null) {
            updateFolder(folder.copy(coverImageUri = destinationFile.absolutePath))
        }
        
        note
    }
    
    suspend fun saveTextNoteToFolder(folderId: String, title: String, content: String): Note = withContext(Dispatchers.IO) {
        val folderFile = File(foldersDir, folderId)
        if (!folderFile.exists()) {
            folderFile.mkdir()
        }
        
        val fileName = "${System.currentTimeMillis()}.txt"
        val noteFile = File(folderFile, fileName)
        noteFile.writeText(content)
        
        val note = Note(
            folderId = folderId,
            title = title,
            textContent = content,
            createdAt = System.currentTimeMillis()
        )
        
        // Save note metadata
        val metadataFile = File(folderFile, "${fileName}.json")
        val json = JSONObject().apply {
            put("id", note.id)
            put("title", note.title)
            put("textContent", note.textContent)
            put("createdAt", note.createdAt)
        }
        metadataFile.writeText(json.toString())
        
        note
    }
    
    suspend fun deleteNote(folderId: String, noteId: String): Boolean = withContext(Dispatchers.IO) {
        val folderFile = File(foldersDir, folderId)
        if (!folderFile.exists()) return@withContext false
        
        val noteFile = File(folderFile, noteId)
        val metadataFile = File(folderFile, "${noteId}.json")
        
        noteFile.delete()
        metadataFile.delete()
        
        true
    }
    
    suspend fun getNotesInFolder(folderId: String): List<Note> = withContext(Dispatchers.IO) {
        val folderFile = File(foldersDir, folderId)
        if (!folderFile.exists()) return@withContext emptyList()
        
        folderFile.listFiles()?.mapNotNull { file ->
            if (file.name.endsWith(".json")) {
                try {
                    val json = JSONObject(file.readText())
                    Note(
                        id = json.getString("id"),
                        folderId = folderId,
                        title = json.getString("title"),
                        textContent = if (json.has("textContent")) json.getString("textContent") else null,
                        imageUri = if (json.has("imageUri")) json.getString("imageUri") else null,
                        createdAt = json.getLong("createdAt")
                    )
                } catch (e: Exception) {
                    null
                }
            } else if (!file.name.endsWith(".json")) {
                Note(
                    id = file.name,
                    folderId = folderId,
                    title = file.nameWithoutExtension,
                    imageUri = file.absolutePath,
                    createdAt = file.lastModified()
                )
            } else null
        }?.sortedByDescending { it.createdAt } ?: emptyList()
    }
} 