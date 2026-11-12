package com.example.readinglist

data class Book(
    val id: String = "",
    val title: String = "",
    val authors: String = "",
    val isbn: String = "",
    val coverUrl: String = "",
    val status: String = "Want to Read",
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id, "title" to title, "authors" to authors, "isbn" to isbn,
        "coverUrl" to coverUrl, "status" to status, "addedAt" to addedAt
    )

    companion object {
        fun fromMap(map: Map<String, Any>): Book = Book(
            id = map["id"] as? String ?: "",
            title = map["title"] as? String ?: "",
            authors = map["authors"] as? String ?: "",
            isbn = map["isbn"] as? String ?: "",
            coverUrl = map["coverUrl"] as? String ?: "",
            status = map["status"] as? String ?: "Want to Read",
            addedAt = (map["addedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }
}
