package com.example.socialmedia

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var storageRef: StorageReference
    private lateinit var emailTextView: TextView
    private lateinit var usernameTextView: TextView
    private lateinit var profileImageView: ImageView
    private lateinit var editUsernameButton: Button
    private lateinit var changeProfileImageButton: Button
    private lateinit var logoutButton: Button
    private var selectedImageUri: Uri? = null

    companion object {
        private const val REQUEST_IMAGE = 100
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        storageRef = FirebaseStorage.getInstance().reference

        emailTextView = view.findViewById(R.id.textViewEmail)
        usernameTextView = view.findViewById(R.id.textViewUsername)
        profileImageView = view.findViewById(R.id.imageViewProfile)
        editUsernameButton = view.findViewById(R.id.buttonEditUsername)
        changeProfileImageButton = view.findViewById(R.id.buttonChangeProfileImage)
        logoutButton = view.findViewById(R.id.buttonLogout)

        loadUserProfile()

        editUsernameButton.setOnClickListener { showEditUsernameDialog() }
        changeProfileImageButton.setOnClickListener { changeProfileImage() }
        logoutButton.setOnClickListener { logout() }

        return view
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = database.child("users").child(userId)

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val email = snapshot.child("email").getValue(String::class.java)
                val username = snapshot.child("username").getValue(String::class.java)
                val profileImageUrl = snapshot.child("profileImageUrl").getValue(String::class.java)

                emailTextView.text = email
                usernameTextView.text = username

                // Check if the Fragment is attached before loading the image
                if (isAdded && context != null && !profileImageUrl.isNullOrEmpty()) {
                    Glide.with(requireContext())
                        .load(profileImageUrl)
                        .into(profileImageView)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
            }
        })
    }


    private fun showEditUsernameDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_username, null)
        val editTextUsername: EditText = dialogView.findViewById(R.id.editTextUsername)

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setTitle("Edit Username")
            .setPositiveButton("Save") { _, _ ->
                val newUsername = editTextUsername.text.toString().trim()
                if (newUsername.isNotEmpty()) {
                    updateUsername(newUsername)
                } else {
                    Toast.makeText(context, "Username cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun updateUsername(newUsername: String) {
        val userId = auth.currentUser?.uid ?: return
        val userRef = database.child("users").child(userId)

        userRef.child("username").setValue(newUsername).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(context, "Username updated successfully", Toast.LENGTH_SHORT).show()
                updateUsernameInPosts(userId, newUsername)
                loadUserProfile()
            } else {
                Toast.makeText(context, "Failed to update username", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUsernameInPosts(userId: String, newUsername: String) {
        val postsRef = database.child("posts")

        postsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (postSnapshot in snapshot.children) {
                    val post = postSnapshot.getValue(Post::class.java)
                    if (post != null && post.email == auth.currentUser?.email) {
                        postSnapshot.ref.child("username").setValue(newUsername)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
            }
        })
    }


    private fun changeProfileImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_IMAGE)
    }

    private fun uploadProfileImage(uri: Uri) {
        val userId = auth.currentUser?.uid ?: return
        val profileImageRef = storageRef.child("profileImages/$userId.jpg")

        profileImageRef.putFile(uri).addOnSuccessListener {
            profileImageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                updateProfileImageUrl(downloadUri.toString())
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Failed to upload profile image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateProfileImageUrl(profileImageUrl: String) {
        val userId = auth.currentUser?.uid ?: return
        val userRef = database.child("users").child(userId)

        userRef.child("profileImageUrl").setValue(profileImageUrl).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(context, "Profile image updated successfully", Toast.LENGTH_SHORT).show()
                updateProfileImageUrlInPosts(userId, profileImageUrl)
                loadUserProfile()
            } else {
                Toast.makeText(context, "Failed to update profile image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateProfileImageUrlInPosts(userId: String, profileImageUrl: String) {
        val postsRef = database.child("posts")

        postsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (postSnapshot in snapshot.children) {
                    val post = postSnapshot.getValue(Post::class.java)
                    if (post != null && post.email == auth.currentUser?.email) {
                        postSnapshot.ref.child("profileImageUrl").setValue(profileImageUrl)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
            }
        })
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data
            selectedImageUri?.let { uri ->
                uploadProfileImage(uri)
            }
        }
    }

    private fun logout() {
        auth.signOut()
        // Navigate to login activity or handle logout accordingly
        val intent = Intent(activity, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
