package com.example.readinglist

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class AuthActivity : Activity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var email: EditText
    private lateinit var password: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (FirebaseApp.initializeApp(this) == null) {
            showSetupMessage()
            return
        }
        auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) { openMain(); return }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(28, 48, 28, 24)
        }
        root.addView(TextView(this).apply {
            text = "My Reading List"; textSize = 28f; setTypeface(null, android.graphics.Typeface.BOLD)
        })
        root.addView(TextView(this).apply { text = "Sign in or create an account to sync your books." })
        email = EditText(this).apply { hint = "Email address"; inputType = 33 }
        password = EditText(this).apply { hint = "Password (6+ characters)"; inputType = 129 }
        root.addView(email); root.addView(password)
        val signIn = Button(this).apply { text = "Sign in"; setOnClickListener { authenticate(false) } }
        val create = Button(this).apply { text = "Create account"; setOnClickListener { authenticate(true) } }
        root.addView(signIn); root.addView(create)
        setContentView(root)
    }

    private fun showSetupMessage() {
        val text = TextView(this).apply {
            setPadding(28, 48, 28, 24); textSize = 17f
            text = "Firebase is not configured yet. Follow SETUP.md and add your Firebase app's google-services.json to the app/ folder, then rebuild."
        }
        setContentView(text)
    }

    private fun authenticate(create: Boolean) {
        val e = email.text.toString().trim()
        val p = password.text.toString()
        if (e.isBlank() || p.length < 6) {
            Toast.makeText(this, "Enter a valid email and password (minimum 6 characters).", Toast.LENGTH_LONG).show()
            return
        }
        val task = if (create) auth.createUserWithEmailAndPassword(e, p) else auth.signInWithEmailAndPassword(e, p)
        task.addOnCompleteListener(this) {
            if (it.isSuccessful) openMain()
            else Toast.makeText(this, it.exception?.localizedMessage ?: "Authentication failed", Toast.LENGTH_LONG).show()
        }
    }

    private fun openMain() {
        startActivity(android.content.Intent(this, MainActivity::class.java))
        finish()
    }
}
