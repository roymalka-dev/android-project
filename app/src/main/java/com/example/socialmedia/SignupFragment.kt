package com.example.socialmedia

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import de.hdodenhof.circleimageview.CircleImageView
import java.util.*

class SignupFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var storageRef: StorageReference
    private var selectedImageUri: Uri? = null
    private var isPasswordVisible: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_signup, container, false)

        auth = FirebaseAuth.getInstance()
        storageRef = FirebaseStorage.getInstance().reference

        val usernameEditText: EditText = view.findViewById(R.id.editTextUsername)
        val emailEditText: EditText = view.findViewById(R.id.editTextEmail)
        val passwordEditText: EditText = view.findViewById(R.id.editTextPassword)
        val eyeImageView: ImageView = view.findViewById(R.id.imageViewEye)
        val imageViewProfile: CircleImageView = view.findViewById(R.id.imageViewProfile)
        val signupButton: Button = view.findViewById(R.id.imageViewSignup)

        eyeImageView.setOnClickListener {
            // Toggle password visibility
            passwordEditText.transformationMethod =
                if (passwordEditText.transformationMethod == null) null else PasswordTransformationMethod.getInstance()
        }

        imageViewProfile.setOnClickListener {
            // Open gallery to select profile image
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, REQUEST_IMAGE)
        }
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
        signupButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(activity, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                registerUser(username, email, password)
            }
        }

        return view
    }

    private fun registerUser(username: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let { uploadImageToFirebase(it, username) }
                } else {
                    Toast.makeText(activity, "Registration failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun uploadImageToFirebase(user: FirebaseUser, username: String) {
        selectedImageUri?.let { uri ->
            val imagesRef = storageRef.child("profile_images/${UUID.randomUUID()}.jpg")

            imagesRef.putFile(uri)
                .addOnSuccessListener {
                    imagesRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        updateUserProfile(user, username, downloadUri.toString())
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(activity, "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } ?: run {
            updateUserProfile(user, username, null)
        }
    }

    private fun updateUserProfile(user: FirebaseUser, username: String, profileImageUrl: String?) {
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(username)
            .setPhotoUri(profileImageUrl?.let { Uri.parse(it) })
            .build()

        user.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    saveUserInfoToDatabase(user.uid, username, user.email, profileImageUrl)
                } else {
                    Toast.makeText(activity, "Failed to update profile", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveUserInfoToDatabase(userId: String, username: String, email: String?, profileImageUrl: String?) {
        val database = FirebaseDatabase.getInstance().reference.child("users").child(userId)
        val userMap = HashMap<String, Any>()
        userMap["username"] = username
        email?.let { userMap["email"] = it }
        profileImageUrl?.let { userMap["profileImageUrl"] = it }

        database.setValue(userMap)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(activity, "Registration successful", Toast.LENGTH_SHORT).show()
                    // Navigate to your desired screen
                } else {
                    Toast.makeText(activity, "Failed to save user info", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data
            selectedImageUri?.let { uri ->
                val imageViewProfile: CircleImageView = view!!.findViewById(R.id.imageViewProfile)
                imageViewProfile.setImageURI(uri)
            }
        }
    }

    companion object {
        private const val REQUEST_IMAGE = 100
    }
}
