package com.example.socialmedia

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import de.hdodenhof.circleimageview.CircleImageView
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var storageRef: StorageReference
    private lateinit var usernameTextView: TextView
    private lateinit var profileImageView: CircleImageView
    private lateinit var postsRecyclerView: RecyclerView
    private lateinit var postsAdapter: PostsAdapter
    private var selectedImageUri: Uri? = null
    private lateinit var imageViewPost: ImageView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        storageRef = FirebaseStorage.getInstance().reference
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        progressBar = view.findViewById(R.id.progressBar)
        usernameTextView = view.findViewById(R.id.textViewUsername)
        profileImageView = view.findViewById(R.id.circleImageViewProfile)
        postsRecyclerView = view.findViewById(R.id.recyclerViewPosts)

        val fab: TextView = view.findViewById(R.id.fab)
        fab.setOnClickListener {
            showAddPostDialog()
        }

        postsRecyclerView.layoutManager = LinearLayoutManager(context)
        postsAdapter = PostsAdapter()
        postsRecyclerView.adapter = postsAdapter

        loadUserProfile()
        loadPosts()
        swipeRefreshLayout.setOnRefreshListener {
            // Handle refresh action here, e.g., reload posts
            loadPosts()
        }
        return view
    }


    private fun loadPosts() {
        progressBar.visibility = View.VISIBLE

        val postsRef = database.child("posts")

        postsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val posts = mutableListOf<Post>()
                for (postSnapshot in snapshot.children) {
                    val post = postSnapshot.getValue(Post::class.java)
                    if (post != null) {
                        posts.add(post)
                    }
                }
                postsAdapter.setPosts(posts)
                progressBar.visibility = View.GONE

                // Complete the refresh animation
                swipeRefreshLayout.isRefreshing = false
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
                progressBar.visibility = View.GONE

                // Complete the refresh animation
                swipeRefreshLayout.isRefreshing = false
            }
        })
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = database.child("users").child(userId)

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val username = snapshot.child("username").getValue(String::class.java)
                val profileImageUrl = snapshot.child("profileImageUrl").getValue(String::class.java)

                usernameTextView.text = username

                if (!profileImageUrl.isNullOrEmpty() && isAdded && context != null) {
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

    private fun showAddPostDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_post, null)
        val descriptionEditText: EditText = dialogView.findViewById(R.id.editTextDescription)
        imageViewPost = dialogView.findViewById(R.id.imageViewPost)
        val uploadButton: Button = dialogView.findViewById(R.id.buttonUpload)

        imageViewPost.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, REQUEST_IMAGE)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        uploadButton.setOnClickListener {
            val description = descriptionEditText.text.toString().trim()
            if (description.isEmpty() || selectedImageUri == null) {
                Toast.makeText(context, "Please add description and image", Toast.LENGTH_SHORT).show()
            } else {
                uploadPost(description)
                dialog.dismiss()
            }
        }

        dialog.show()

        // Load the selected image into ImageView if available
        selectedImageUri?.let {
            Glide.with(this)
                .load(it)
                .into(imageViewPost)
        }
    }


    private fun uploadPost(description: String) {
        val userId = auth.currentUser?.uid ?: return
        val userRef = database.child("users").child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val username = snapshot.child("username").getValue(String::class.java)
                val email = snapshot.child("email").getValue(String::class.java)
                val profileImageUrl = snapshot.child("profileImageUrl").getValue(String::class.java)

                val postRef = storageRef.child("posts/${UUID.randomUUID()}.jpg")
                postRef.putFile(selectedImageUri!!)
                    .addOnSuccessListener {
                        postRef.downloadUrl.addOnSuccessListener { uri ->
                            val postImageUrl = uri.toString()
                            savePostToDatabase(description, email, username, profileImageUrl, postImageUrl)
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to upload post image", Toast.LENGTH_SHORT).show()
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
            }
        })
    }

    private fun savePostToDatabase(description: String, email: String?, username: String?, profileImageUrl: String?, postImageUrl: String) {
        val postId = database.child("posts").push().key ?: return
        val postMap = HashMap<String, Any>()
        postMap["description"] = description
        postMap["email"] = email ?: ""
        postMap["username"] = username ?: ""
        postMap["profileImageUrl"] = profileImageUrl ?: ""
        postMap["postImageUrl"] = postImageUrl

        database.child("posts").child(postId).setValue(postMap)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Post uploaded successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to upload post", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data
            selectedImageUri?.let { uri ->
                // Update the ImageView in the dialog with the selected image
                if (this::imageViewPost.isInitialized) {
                    imageViewPost.setImageURI(uri)
                }
            }
        }
    }

    companion object {
        private const val REQUEST_IMAGE = 100
    }
}
