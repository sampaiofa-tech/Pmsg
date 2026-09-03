package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.BurnerChannel
import com.example.data.model.EphemeralMessage
import com.example.data.model.Contact
import com.example.util.security.CryptoManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [EphemeralMessage::class, BurnerChannel::class, Contact::class],
    version = 5,
    exportSchema = false
)
abstract class VanishDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao
    abstract fun channelDao(): ChannelDao
    abstract fun contactDao(): ContactDao

    companion object {
        @Volatile
        private var INSTANCE: VanishDatabase? = null

        fun getInstance(): VanishDatabase? = INSTANCE

        fun getDatabase(context: Context, scope: CoroutineScope): VanishDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VanishDatabase::class.java,
                    "vanish_zero_trace_db"
                )
                .addCallback(VanishDatabaseCallback(scope))
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun performAntiForensicVacuum() {
            try {
                INSTANCE?.openHelper?.writableDatabase?.execSQL("PRAGMA incremental_vacuum(50)")
            } catch (_: Throwable) {}
        }

        fun performFullVacuum() {
            try {
                INSTANCE?.openHelper?.writableDatabase?.execSQL("VACUUM")
            } catch (_: Throwable) {}
        }

        private class VanishDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                try {
                    // Anti-forensics: Instruct SQLite to overwrite deleted cell content with zeros
                    db.execSQL("PRAGMA secure_delete = ON")
                    db.execSQL("PRAGMA auto_vacuum = INCREMENTAL")
                } catch (_: Throwable) {}
            }

            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                try {
                    db.execSQL("PRAGMA secure_delete = ON")
                    db.execSQL("PRAGMA auto_vacuum = INCREMENTAL")
                } catch (_: Throwable) {}
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialChannels(database)
                    }
                }
            }

            private suspend fun populateInitialChannels(database: VanishDatabase) {
                val now = System.currentTimeMillis()
                val ttl24h = 24 * 60 * 60 * 1000L

                val initialChannels = listOf(
                    BurnerChannel(
                        id = "channel_stealth_1",
                        name = "Alice (Criptografia)",
                        avatarColorHex = 0xFF00E5FF,
                        securityTag = "Criptografia E2EE • Zero Rastro",
                        channelCode = "VN-7401",
                        lastMessagePreview = "Toda mensagem aqui é apagada em 24h sem deixar vestígios 🔒",
                        lastMessageTimestamp = now - (15 * 60 * 1000L),
                        defaultTtlHours = 24f,
                        isPinned = true
                    ),
                    BurnerChannel(
                        id = "channel_stealth_2",
                        name = "Canal Anônimo #893",
                        avatarColorHex = 0xFF00E676,
                        securityTag = "Anônimo • Autodestruição 24h",
                        channelCode = "AN-8932",
                        lastMessagePreview = "Código de acesso descartável gerado.",
                        lastMessageTimestamp = now - (2 * 60 * 60 * 1000L),
                        defaultTtlHours = 24f,
                        isPinned = false
                    ),
                    BurnerChannel(
                        id = "channel_stealth_3",
                        name = "Sala Privada",
                        avatarColorHex = 0xFFFF6D00,
                        securityTag = "Sala Segura • TTL 24h",
                        channelCode = "BR-5510",
                        lastMessagePreview = "Mensagens vaporizam automaticamente.",
                        lastMessageTimestamp = now - (6 * 60 * 60 * 1000L),
                        defaultTtlHours = 24f,
                        isPinned = false
                    )
                )

                database.channelDao().insertAllDefaultChannels(initialChannels)

                // Populate initial ephemeral sample messages with strict 24h countdown and hardware encryption
                val sampleMessages = listOf(
                    EphemeralMessage(
                        roomId = "channel_stealth_1",
                        senderId = "ALICE",
                        senderName = "Alice",
                        content = CryptoManager.encrypt("Olá! Este é um chat seguro com proteção de zero rastros."),
                        timestamp = now - (20 * 60 * 1000L),
                        expiresAt = now - (20 * 60 * 1000L) + ttl24h,
                        ttlOptionHours = 24f
                    ),
                    EphemeralMessage(
                        roomId = "channel_stealth_1",
                        senderId = "ME",
                        senderName = "Você",
                        content = CryptoManager.encrypt("Perfeito. Após 24 horas essa conversa será apagada sem deixar nenhum rastro no dispositivo ou em servidores."),
                        timestamp = now - (18 * 60 * 1000L),
                        expiresAt = now - (18 * 60 * 1000L) + ttl24h,
                        ttlOptionHours = 24f
                    ),
                    EphemeralMessage(
                        roomId = "channel_stealth_1",
                        senderId = "ALICE",
                        senderName = "Alice",
                        content = CryptoManager.encrypt("Toda mensagem aqui é apagada em 24h sem deixar vestígios 🔒"),
                        timestamp = now - (15 * 60 * 1000L),
                        expiresAt = now - (15 * 60 * 1000L) + ttl24h,
                        ttlOptionHours = 24f
                    ),
                    EphemeralMessage(
                        roomId = "channel_stealth_2",
                        senderId = "ANON",
                        senderName = "Anônimo",
                        content = CryptoManager.encrypt("Canal temporário iniciado. Cronômetro de 24 horas ativo."),
                        timestamp = now - (2 * 60 * 60 * 1000L),
                        expiresAt = now - (2 * 60 * 60 * 1000L) + ttl24h,
                        ttlOptionHours = 24f
                    )
                )

                sampleMessages.forEach { msg ->
                    database.messageDao().insertMessage(msg)
                }
            }
        }
    }
}
