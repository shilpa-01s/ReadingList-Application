package com.example.readinglist

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var adapter: BookAdapter
    private lateinit var statusFilter: Spinner
    private lateinit var searchInput: EditText
    private val allBooks = mutableListOf<Book>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (auth.currentUser == null) { finish(); return }
        buildUi()
        loadBooks()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(16, 16, 16, 8) }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL }
        header.addView(TextView(this).apply {
            text = "My Reading List"; textSize = 25f; setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        header.addView(Button(this).apply { text = "Sign out"; setOnClickListener { auth.signOut(); startActivity(android.content.Intent(this@MainActivity, AuthActivity::class.java)); finish() } })
        root.addView(header)
        searchInput = EditText(this).apply {
            hint = "Search books by title"
            setSingleLine(true)
        }
        root.addView(searchInput)
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(Button(this).apply { text = "Search"; setOnClickListener { searchBooks() } })
        actions.addView(Button(this).apply { text = "Scan ISBN"; setOnClickListener { scanIsbn() } })
        actions.addView(Button(this).apply { text = "Add ISBN"; setOnClickListener { promptIsbn() } })
        root.addView(actions)
        statusFilter = Spinner(this)
        statusFilter.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            listOf("All Books", "Want to Read", "Reading", "Finished"))
        statusFilter.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) = renderBooks()
        }
        root.addView(statusFilter)
        adapter = BookAdapter({ book, status -> updateBook(book.copy(status = status)) }, { book -> deleteBook(book) })
        val list = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@MainActivity); adapter = this@MainActivity.adapter
        }
        root.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
    }

    private fun userBooks() = db.collection("users").document(auth.currentUser!!.uid).collection("books")

    private fun loadBooks() {
        userBooks().addSnapshotListener(this) { snapshot, error ->
            if (error != null) { toast("Could not load books: ${error.localizedMessage}"); return@addSnapshotListener }
            allBooks.clear()
            snapshot?.documents?.forEach { doc ->
                val data = doc.data ?: return@forEach
                allBooks += Book.fromMap(data).copy(id = doc.id)
            }
            renderBooks()
        }
    }

    private fun renderBooks() {
        if (!::adapter.isInitialized) return
        val filter = if (::statusFilter.isInitialized) statusFilter.selectedItem?.toString() ?: "All Books" else "All Books"
        val filtered = allBooks.filter { filter == "All Books" || it.status == filter }
        adapter.submit(filtered.sortedByDescending { it.addedAt })
    }

    private fun searchBooks() {
        val query = searchInput.text.toString().trim()
        if (query.isBlank()) { toast("Enter a book title first."); return }
        toast("Searching Open Library…")
        executor.execute {
            try {
                val results = OpenLibraryApi.searchTitle(query)
                runOnUiThread {
                    if (results.isEmpty()) { toast("No books found."); return@runOnUiThread }
                    val labels = results.map { "${it.title} — ${it.authors}" }.toTypedArray()
                    android.app.AlertDialog.Builder(this).setTitle("Choose a book")
                        .setItems(labels) { _, which -> confirmAdd(results[which]) }.show()
                }
            } catch (e: Exception) { runOnUiThread { toast(e.localizedMessage ?: "Search failed") } }
        }
    }

    private fun scanIsbn() {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_EAN_13)
            .build()

        val scanner = GmsBarcodeScanning.getClient(this, options)

        scanner.startScan()
            .addOnSuccessListener { barcode: Barcode ->

                val isbn = barcode.rawValue
                    .orEmpty()
                    .filter { it.isDigit() }

                if (isbn.length == 13 &&
                    (isbn.startsWith("978") || isbn.startsWith("979"))) {

                    lookupIsbn(isbn)

                } else {
                    toast("Invalid ISBN-13 barcode")
                }
            }
            .addOnCanceledListener {
                // User cancelled scanning
            }
            .addOnFailureListener { error ->
                toast("Scanner error: ${error.localizedMessage}")
            }
    }
    private fun promptIsbn() {
        val input = EditText(this).apply { hint = "13-digit ISBN-13 (978/979)"; inputType = 3 }
        android.app.AlertDialog.Builder(this).setTitle("Enter ISBN").setView(input)
            .setPositiveButton("Look up") { _, _ -> lookupIsbn(input.text.toString()) }
            .setNegativeButton("Cancel", null).show()
    }

    private fun lookupIsbn(raw: String) {
        val isbn = raw.filter { it.isDigit() }
        executor.execute {
            try {
                val info = OpenLibraryApi.lookupIsbn(isbn)
                runOnUiThread { confirmAdd(info) }
            } catch (e: Exception) { runOnUiThread { toast(e.localizedMessage ?: "ISBN lookup failed") } }
        }
    }

    private fun confirmAdd(info: OpenLibraryApi.BookInfo) {
        val book = Book(
            id = UUID.randomUUID().toString(), title = info.title, authors = info.authors,
            isbn = info.isbn, coverUrl = info.coverUrl
        )
        android.app.AlertDialog.Builder(this).setTitle("Add this book?")
            .setMessage("${book.title}\n${book.authors}\nISBN: ${book.isbn}")
            .setPositiveButton("Add") { _, _ -> saveBook(book) }
            .setNegativeButton("Cancel", null).show()
    }

    private fun saveBook(book: Book) {
        userBooks().document(book.id).set(book.toMap())
            .addOnSuccessListener { toast("Book added to your reading list.") }
            .addOnFailureListener { toast("Save failed: ${it.localizedMessage}") }
    }

    private fun updateBook(book: Book) {
        userBooks().document(book.id).update("status", book.status)
            .addOnFailureListener { toast("Could not update reading status.") }
    }

    private fun deleteBook(book: Book) {
        android.app.AlertDialog.Builder(this).setMessage("Remove “${book.title}” from your list?")
            .setPositiveButton("Remove") { _, _ ->
                userBooks().document(book.id).delete().addOnFailureListener { toast("Could not remove book.") }
            }.setNegativeButton("Cancel", null).show()
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    override fun onDestroy() { super.onDestroy(); executor.shutdown() }
}
