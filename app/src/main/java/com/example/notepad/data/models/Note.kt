package com.example.notepad.data.models

import java.util.UUID

data class Note(
    val id: String = System.currentTimeMillis().toString(),
    val folderId: String,
    val title: String,
    val textContent: String? = null,
    val imageUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) 