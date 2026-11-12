package com.example.readinglist

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

object OpenLibraryApi {
    data class BookInfo(val title: String, val authors: String, val isbn: String, val coverUrl: String)

    private fun getJson(url: String): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12000
            readTimeout = 12000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "ReadingListAndroidApp/1.0")
        }
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream.bufferedReader().use { it.readText() }
            if (code !in 200..299) throw IllegalStateException("Open Library returned HTTP $code")
            return JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun isValidIsbn13(value: String): Boolean {
        if (value.length != 13 || !value.all(Char::isDigit) ||
            !(value.startsWith("978") || value.startsWith("979"))) return false
        val sum = value.take(12).mapIndexed { index, char ->
            char.digitToInt() * if (index % 2 == 0) 1 else 3
        }.sum()
        val checkDigit = (10 - (sum % 10)) % 10
        return checkDigit == value.last().digitToInt()
    }

    fun lookupIsbn(isbn: String): BookInfo {
        val clean = isbn.filter { it.isDigit() }
        require(isValidIsbn13(clean)) {
            "Please scan or enter a valid ISBN-13 beginning 978/979 (including its check digit)."
        }
        val json = getJson("https://openlibrary.org/isbn/$clean.json")
        val title = json.optString("title", "Unknown title")
        val authorsArray = json.optJSONArray("authors")
        val authorNames = mutableListOf<String>()
        if (authorsArray != null) {
            for (i in 0 until authorsArray.length()) {
                val key = authorsArray.optJSONObject(i)?.optString("key") ?: continue
                try {
                    val author = getJson("https://openlibrary.org$key")
                    authorNames += author.optString("name", "")
                } catch (_: Exception) { }
            }
        }
        return BookInfo(title, authorNames.filter { it.isNotBlank() }.joinToString(", ").ifBlank { "Author not listed" },
            clean, "https://covers.openlibrary.org/b/isbn/$clean-M.jpg")
    }

    fun searchTitle(query: String): List<BookInfo> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val json = getJson("https://openlibrary.org/search.json?q=$encoded&limit=15")
        val docs = json.optJSONArray("docs") ?: return emptyList()
        val result = mutableListOf<BookInfo>()
        for (i in 0 until docs.length()) {
            val item = docs.optJSONObject(i) ?: continue
            val title = item.optString("title", "")
            if (title.isBlank()) continue
            val authorsArray = item.optJSONArray("author_name")
            val authors = if (authorsArray != null) (0 until authorsArray.length())
                .mapNotNull { authorsArray.optString(it).takeIf(String::isNotBlank) }
                .take(3).joinToString(", ") else "Author not listed"
            val isbnArray = item.optJSONArray("isbn")
            val isbn = if (isbnArray != null && isbnArray.length() > 0) isbnArray.optString(0) else ""
            val coverId = item.optInt("cover_i", 0)
            val cover = if (coverId > 0) "https://covers.openlibrary.org/b/id/$coverId-M.jpg" else ""
            result += BookInfo(title, authors.ifBlank { "Author not listed" }, isbn, cover)
        }
        return result
    }
}
