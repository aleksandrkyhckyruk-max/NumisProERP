package com.numisproerp.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Замітка з опціональним нагадуванням.
 *
 * [attachments] зберігається як один рядок: пари `name|uri`, розділені символом
 * `\n`. Це уникає необхідності TypeConverter і додаткової міграції на нову
 * таблицю — досить додати TEXT-колонку. Допустимі типи файлів — PDF та Excel
 * (`xls`/`xlsx`); інші формати теж технічно дозволені, але UI має mime-фільтр.
 */
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey
    val noteId: String,
    val title: String,
    val text: String = "",
    val reminderDate: Long? = null,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val attachments: String = ""
)

/**
 * Серіалізовані вкладення замітки (`name|uri`, розділені `\n`).
 * Парс/серіалізатори для UI-шару.
 */
data class NoteAttachment(val name: String, val uri: String) {
    override fun toString(): String = "$name|$uri"

    companion object {
        fun parseAll(serialized: String): List<NoteAttachment> {
            if (serialized.isBlank()) return emptyList()
            return serialized.split('\n')
                .mapNotNull { line ->
                    val sep = line.indexOf('|')
                    if (sep < 0) return@mapNotNull null
                    NoteAttachment(
                        name = line.substring(0, sep),
                        uri = line.substring(sep + 1)
                    )
                }
        }

        fun serializeAll(items: List<NoteAttachment>): String =
            items.joinToString("\n") { it.toString() }
    }
}
