package com.nexus.messenger.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val chatsCollection = firestore.collection("chats")

    fun getChats(userId: String): Flow<List<Chat>> = callbackFlow {
        val listenerRegistration = chatsCollection
            .whereArrayContains("participants", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val chats = snapshot.documents.mapNotNull { it.toObject(Chat::class.java) }
                    trySend(chats).isSuccess
                }
            }

        awaitClose { listenerRegistration.remove() }
    }

    fun getMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val listenerRegistration = chatsCollection.document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { it.toObject(Message::class.java) }
                    trySend(messages).isSuccess
                }
            }

        awaitClose { listenerRegistration.remove() }
    }

    suspend fun sendMessage(chatId: String, senderId: String, text: String): Result<Unit> {
        return try {
            val messageId = UUID.randomUUID().toString()
            val message = Message(
                id = messageId,
                senderId = senderId,
                text = text,
                timestamp = System.currentTimeMillis()
            )

            // Add message to subcollection
            chatsCollection.document(chatId)
                .collection("messages")
                .document(messageId)
                .set(message)
                .await()

            // Update chat's last message info
            val chatUpdate = mapOf(
                "lastMessage" to text,
                "lastMessageTimestamp" to message.timestamp
            )
            chatsCollection.document(chatId).update(chatUpdate).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createChat(currentUserId: String, targetUserId: String): Result<String> {
        return try {
            // Check if chat already exists
            val existingChatsSnapshot = chatsCollection
                .whereArrayContains("participants", currentUserId)
                .get()
                .await()

            for (document in existingChatsSnapshot.documents) {
                val chat = document.toObject(Chat::class.java)
                if (chat != null && chat.participants.contains(targetUserId)) {
                    return Result.success(chat.id)
                }
            }

            // Create new chat
            val newChatId = UUID.randomUUID().toString()
            val newChat = Chat(
                id = newChatId,
                participants = listOf(currentUserId, targetUserId),
                lastMessage = "",
                lastMessageTimestamp = System.currentTimeMillis()
            )

            chatsCollection.document(newChatId).set(newChat).await()
            Result.success(newChatId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
