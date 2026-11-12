package com.example.readinglist

import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import coil.load

class BookAdapter(
    private val onStatusChanged: (Book, String) -> Unit,
    private val onDelete: (Book) -> Unit
) : RecyclerView.Adapter<BookAdapter.BookHolder>() {
    private val books = mutableListOf<Book>()
    fun submit(items: List<Book>) { books.clear(); books.addAll(items); notifyDataSetChanged() }

    inner class BookHolder(val row: LinearLayout) : RecyclerView.ViewHolder(row) {
        val cover = ImageView(row.context)
        val title = TextView(row.context)
        val author = TextView(row.context)
        val isbn = TextView(row.context)
        val status = Spinner(row.context)
        val delete = Button(row.context)
        init {
            row.orientation = LinearLayout.HORIZONTAL
            row.setPadding(12, 12, 12, 12)
            cover.layoutParams = LinearLayout.LayoutParams(88, 124)
            cover.scaleType = ImageView.ScaleType.CENTER_CROP
            val content = LinearLayout(row.context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(14, 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            title.textSize = 17f; title.setTypeface(null, android.graphics.Typeface.BOLD)
            author.textSize = 13f; isbn.textSize = 12f
            status.adapter = ArrayAdapter(row.context, android.R.layout.simple_spinner_dropdown_item,
                listOf("Want to Read", "Reading", "Finished"))
            delete.text = "Remove"
            content.addView(title); content.addView(author); content.addView(isbn)
            content.addView(status); content.addView(delete)
            row.addView(cover); row.addView(content)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookHolder =
        BookHolder(LinearLayout(parent.context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        })

    override fun onBindViewHolder(holder: BookHolder, position: Int) {
        val book = books[position]
        holder.title.text = book.title
        holder.author.text = book.authors
        holder.isbn.text = if (book.isbn.isBlank()) "ISBN not available" else "ISBN: ${book.isbn}"
        holder.cover.load(book.coverUrl) { crossfade(true); placeholder(android.R.drawable.ic_menu_gallery) }
        val options = listOf("Want to Read", "Reading", "Finished")
        holder.status.setSelection(options.indexOf(book.status).coerceAtLeast(0))
        holder.status.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, index: Int, id: Long) {
                if (options[index] != book.status) onStatusChanged(book, options[index])
            }
        }
        holder.delete.setOnClickListener { onDelete(book) }
    }

    override fun getItemCount() = books.size
}
