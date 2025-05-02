package com.example.notepad.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.notepad.data.models.Note
import java.io.File

object ShareUtils {
    fun shareFolder(context: Context, folderId: String, folderName: String) {
        val folder = File(context.filesDir, "folders/$folderId")
        if (!folder.exists()) return
        
        val files = folder.listFiles() ?: return
        if (files.isEmpty()) return
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND_MULTIPLE
            type = "image/*"
            putParcelableArrayListExtra(
                Intent.EXTRA_STREAM,
                ArrayList(files.map { Uri.fromFile(it) })
            )
            putExtra(Intent.EXTRA_SUBJECT, "Sharing folder: $folderName")
        }
        
        context.startActivity(Intent.createChooser(shareIntent, "Share folder via"))
    }
    
    fun shareViaWhatsApp(context: Context, folderId: String, folderName: String) {
        val folder = File(context.filesDir, "folders/$folderId")
        if (!folder.exists()) return
        
        val files = folder.listFiles() ?: return
        if (files.isEmpty()) return
        
        val whatsappIntent = Intent().apply {
            action = Intent.ACTION_SEND_MULTIPLE
            type = "image/*"
            setPackage("com.whatsapp")
            putParcelableArrayListExtra(
                Intent.EXTRA_STREAM,
                ArrayList(files.map { Uri.fromFile(it) })
            )
            putExtra(Intent.EXTRA_SUBJECT, "Sharing folder: $folderName")
        }
        
        if (whatsappIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(whatsappIntent)
        }
    }
    
    fun shareViaEmail(context: Context, folderId: String, folderName: String) {
        val folder = File(context.filesDir, "folders/$folderId")
        if (!folder.exists()) return
        
        val files = folder.listFiles() ?: return
        if (files.isEmpty()) return
        
        val emailIntent = Intent().apply {
            action = Intent.ACTION_SEND_MULTIPLE
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(""))
            putExtra(Intent.EXTRA_SUBJECT, "Sharing folder: $folderName")
            putParcelableArrayListExtra(
                Intent.EXTRA_STREAM,
                ArrayList(files.map { Uri.fromFile(it) })
            )
        }
        
        context.startActivity(Intent.createChooser(emailIntent, "Send email..."))
    }

    fun shareNote(context: Context, note: Note) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            
            if (note.imageUri != null) {
                // Share image
                val imageFile = File(note.imageUri)
                if (imageFile.exists()) {
                    val imageUri = Uri.fromFile(imageFile)
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                }
            }
            
            // Add text content
            val shareText = buildString {
                append("${note.title}\n\n")
                note.textContent?.let { append(it) }
            }
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        
        context.startActivity(Intent.createChooser(shareIntent, "Share Note"))
    }
} 