package com.nexus.messenger.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexus.messenger.data.Chat
import com.nexus.messenger.viewmodel.ChatListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onChatSelected: (String) -> Unit,
    viewModel: ChatListViewModel = viewModel()
) {
    val chats by viewModel.chats.collectAsState()
    val error by viewModel.error.collectAsState()
    val newChatId by viewModel.newChatId.collectAsState()

    var showNewChatDialog by remember { mutableStateOf(false) }

    LaunchedEffect(newChatId) {
        newChatId?.let {
            onChatSelected(it)
            viewModel.clearNewChatId()
            showNewChatDialog = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chats") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNewChatDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "New Chat")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn {
                items(chats) { chat ->
                    ChatItem(chat = chat, onClick = { onChatSelected(chat.id) })
                }
            }

            if (error != null) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(error!!)
                }
            }
        }

        if (showNewChatDialog) {
            var email by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showNewChatDialog = false },
                title = { Text("New Chat") },
                text = {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("User Email") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.createNewChatByEmail(email)
                        }
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNewChatDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun ChatItem(chat: Chat, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(
            text = "Chat with ${chat.participants.joinToString(", ")}", // Simplification
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = chat.lastMessage.ifEmpty { "No messages yet" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    HorizontalDivider()
}
