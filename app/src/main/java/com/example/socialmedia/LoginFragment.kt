package com.example.socialmedia

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.auth.FirebaseAuth

class LoginFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private var isPasswordVisible: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        auth = FirebaseAuth.getInstance()

        val emailEditText: EditText = view.findViewById(R.id.editTextEmail)
        val passwordEditText: EditText = view.findViewById(R.id.editTextPassword)
        val loginButton: AppCompatButton = view.findViewById(R.id.buttonLogin)
        val eyeImageView: ImageView = view.findViewById(R.id.imageViewEye)

        eyeImageView.setOnClickListener {
            // Toggle password visibility
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                passwordEditText.transformationMethod = HideReturnsTransformationMethod.getInstance()
                eyeImageView.setImageResource(R.drawable.eyeclosed) // Change icon if needed
            } else {
                passwordEditText.transformationMethod = PasswordTransformationMethod.getInstance()
                eyeImageView.setImageResource(R.drawable.ic_eye) // Change icon if needed
            }
            passwordEditText.setSelection(passwordEditText.text.length) // Move cursor to the end
        }

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(activity, "Please enter email and password", Toast.LENGTH_SHORT).show()
            } else {
                loginUser(email, password)
            }
        }

        // Check if user is already logged in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            navigateToHomepage()
        }

        return view
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(activity, "Login successful", Toast.LENGTH_SHORT).show()
                    navigateToHomepage()
                } else {
                    Toast.makeText(activity, "Login failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun navigateToHomepage() {
        // Launch HomepageActivity
        val intent = Intent(activity, HomepageActivity::class.java)
        startActivity(intent)
        activity?.finish() // Optional: Finish the current activity
    }
}
