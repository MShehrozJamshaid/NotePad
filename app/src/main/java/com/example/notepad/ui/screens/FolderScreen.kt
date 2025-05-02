package com.example.notepad.ui.screens

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.notepad.R
import com.example.notepad.data.models.Note
import com.example.notepad.ui.viewmodels.FolderViewModel
import com.example.notepad.utils.ShareUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Objects

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderScreen(
    folderId: String?,
    navController: NavController,
    viewModel: FolderViewModel = viewModel(
        factory = FolderViewModel.provideFactory(
            LocalContext.current.applicationContext as Application,
            folderId ?: ""
        )
    )
) {
    if (folderId == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Invalid folder ID")
        }
        return
    }

    val folder by viewModel.folder.collectAsState()
    val notes by viewModel.notes.collectAsState()
    var showImagePicker by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showTextNoteDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var newTitle by remember { mutableStateOf("") }
    var newTextNote by remember { mutableStateOf("") }
    var selectedNotes by remember { mutableStateOf(setOf<String>()) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val filteredNotes = remember(notes, searchQuery) {
        if (searchQuery.isBlank()) notes
        else notes.filter { note ->
            note.title.contains(searchQuery, ignoreCase = true) ||
            note.textContent?.contains(searchQuery, ignoreCase = true) == true
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.addImage(it, newTitle.takeIf { it.isNotBlank() }) }
    }

    val multipleImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            viewModel.addImage(uri)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
           val currentPhotoUri = photoUri
           currentPhotoUri?.let { viewModel.addImage(it) }
        }
    }
    
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission is granted. Continue the action or workflow in your app.
            val file = File(
                context.externalCacheDir, // Use external cache dir for temp files
                "IMG_" + SimpleDateFormat("yyyyMMdd_HHmmss").format(Date()) + ".jpg"
            )
            photoUri = FileProvider.getUriForFile(
                Objects.requireNonNull(context),
                context.packageName + ".provider", // Ensure this matches provider authority
                file
            )
            val currentPhotoUri = photoUri
            currentPhotoUri?.let { cameraLauncher.launch(it) } ?: println("Error: photoUri is null before launching camera")
            
        } else {
            // Explain to the user that the feature is unavailable because the
            // feature requires a permission that the user has denied. At the
            // same time, respect the user's decision. Don't link to system
            // settings in an effort to convince the user to change their
            // decision.
            // You could show a Snackbar or Dialog here.
            println("Camera permission denied") 
        }
    }

    fun launchCameraWithPermissionCheck() {
         when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) -> {
                // Permission already granted, launch camera
                 val file = File(
                    context.externalCacheDir, // Use external cache dir for temp files
                    "IMG_" + SimpleDateFormat("yyyyMMdd_HHmmss").format(Date()) + ".jpg"
                 )
                 photoUri = FileProvider.getUriForFile(
                    Objects.requireNonNull(context),
                    context.packageName + ".provider", // Ensure this matches provider authority
                    file
                 )
                val currentPhotoUri = photoUri
                currentPhotoUri?.let { cameraLauncher.launch(it) } ?: println("Error: photoUri is null before launching camera")
            }
            else -> {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF6B89F2),
            Color(0xFF8E9EEE),
            Color(0xFFC4C4F4)
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search notes...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                            )
                        )
                    } else {
                        Text(folder?.name ?: "Loading...")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    if (!isSearchActive) {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    } else {
                        IconButton(onClick = {
                            isSearchActive = false
                            searchQuery = ""
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Search")
                        }
                    }
                    
                    if (selectedNotes.isNotEmpty()) {
                        IconButton(onClick = { showShareDialog = true }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                        IconButton(onClick = { 
                            selectedNotes.forEach { noteId ->
                                viewModel.deleteNote(noteId)
                            }
                            selectedNotes = emptySet()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    } else {
                        Row {
                            IconButton(onClick = { showEditDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Folder")
                            }
                            IconButton(onClick = { 
                                selectedNotes = if (selectedNotes.isEmpty()) {
                                    notes.map { it.id }.toSet()
                                } else {
                                    emptySet()
                                }
                            }) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Select Notes")
                            }
                        }
                    }
                    
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .padding(8.dp)
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            )
        },
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(
                    onClick = { showImagePicker = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.Photo, contentDescription = "Import Image")
                }
                FloatingActionButton(
                    onClick = { launchCameraWithPermissionCheck() },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.Camera, contentDescription = "Take Photo")
                }
                FloatingActionButton(
                    onClick = { showTextNoteDialog = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.TextFields, contentDescription = "Add Text Note")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
        ) {
            if (filteredNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (searchQuery.isNotBlank()) {
                            Text(
                                "No matching notes found",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.app_logo),
                                contentDescription = null,
                                modifier = Modifier.size(96.dp)
                            )
                            Text(
                                "This folder is empty",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                "Add images or text notes!",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 128.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(padding)
                ) {
                    items(filteredNotes) { note ->
                        NoteItem(
                            note = note,
                            isSelected = selectedNotes.contains(note.id),
                            onSelect = { 
                                selectedNotes = if (selectedNotes.contains(note.id)) {
                                    selectedNotes - note.id
                                } else {
                                    selectedNotes + note.id
                                }
                            },
                            onClick = { 
                                if (selectedNotes.isNotEmpty()) {
                                    selectedNotes = if (selectedNotes.contains(note.id)) {
                                        selectedNotes - note.id
                                    } else {
                                        selectedNotes + note.id
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showImagePicker) {
        AlertDialog(
            onDismissRequest = { showImagePicker = false },
            title = { Text("Import Images") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Image Title (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = {
                                imagePickerLauncher.launch("image/*")
                                showImagePicker = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Single Image")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                multipleImagePickerLauncher.launch("image/*")
                                showImagePicker = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Multiple Images")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showImagePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEditDialog) {
        var editedName by remember { mutableStateOf(folder?.name ?: "") }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Folder") },
            text = {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Folder Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateFolder(editedName)
                        showEditDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showTextNoteDialog) {
        AlertDialog(
            onDismissRequest = { showTextNoteDialog = false },
            title = { Text("Add Text Note") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Note Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTextNote,
                        onValueChange = { newTextNote = it },
                        label = { Text("Note Content") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newTextNote.isNotBlank()) {
                            viewModel.addTextNote(newTitle, newTextNote)
                            newTitle = ""
                            newTextNote = ""
                            showTextNoteDialog = false
                        }
                    },
                    enabled = newTextNote.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTextNoteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showShareDialog) {
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text("Share Notes") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            selectedNotes.forEach { noteId ->
                                notes.find { it.id == noteId }?.let { note ->
                                    ShareUtils.shareNote(context, note)
                                }
                            }
                            showShareDialog = false
                            selectedNotes = emptySet()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share via...")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showShareDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun NoteItem(
    note: Note,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp)),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (note.imageUri != null) {
                AsyncImage(
                    model = note.imageUri,
                    contentDescription = note.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    note.textContent?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            if (isSelected) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        modifier = Modifier.align(Alignment.Center),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
} 