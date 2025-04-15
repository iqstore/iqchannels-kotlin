package ru.iqchannels.sdk.room

import androidx.room.*

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessage(message: DatabaseMessage)

    @Query("SELECT * FROM messages")
    fun getAllMessages(): List<DatabaseMessage>?

    @Query("DELETE FROM messages WHERE localId = :localId")
    fun deleteMessageByLocalId(localId: Long): Int?

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    fun deleteMessageByChatId(chatId: Long): Int?
}