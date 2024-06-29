package com.example.socialmedia

import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class MyFeedsFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var storage: FirebaseStorage
    private lateinit var postsRecyclerView: RecyclerView
    private lateinit var postsAdapter: PostsAdapter
    private var userEmail: String? = null
    private lateinit var noFeedsTextView: TextView
    private var selectedImageUri: Uri? = null
    private lateinit var postImageViewInDialog: ImageView // Reference for ImageView in dialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_my_feeds, container, false)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        storage = FirebaseStorage.getInstance()
        noFeedsTextView = view.findViewById(R.id.textViewNoFeeds)

        postsRecyclerView = view.findViewById(R.id.recyclerViewMyPosts)
        postsRecyclerView.layoutManager = LinearLayoutManager(context)
        postsAdapter = PostsAdapter()
        postsRecyclerView.adapter = postsAdapter

        userEmail = auth.currentUser?.email

        loadUserPosts()

        return view
    }

    private fun loadUserPosts() {
        userEmail?.let { email ->
            val postsRef = database.child("posts")
            postsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val posts = mutableListOf<Post>()
                    for (postSnapshot in snapshot.children) {
                        val post = postSnapshot.getValue(Post::class.java)
                        if (post?.email == email) {
                            post?.id = postSnapshot.key
                            posts.add(post)
                        }
                    }
                    postsAdapter.setPosts(posts)
                    if (posts.isEmpty()) {
                        noFeedsTextView.visibility = View.VISIBLE
                    } else {
                        noFeedsTextView.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle database error
                }
            })
        }
    }

    inner class PostsAdapter : RecyclerView.Adapter<PostsAdapter.PostViewHolder>() {

        private var posts: List<Post> = listOf()

        fun setPosts(posts: List<Post>) {
            this.posts = posts
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post1, parent, false)
            return PostViewHolder(view)
        }

        override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
            val post = posts[position]
            holder.bind(post)
        }

        override fun getItemCount(): Int = posts.size

        inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val usernameTextView: TextView = itemView.findViewById(R.id.textViewUsername)
            private val descriptionTextView: TextView = itemView.findViewById(R.id.textViewDescription)
            private val profileImageView: ImageView = itemView.findViewById(R.id.imageViewProfile)
            private val postImageView: ImageView = itemView.findViewById(R.id.imageViewPost)
            private val editImageView: ImageView = itemView.findViewById(R.id.imageViewEdit)
            private val deleteImageView: ImageView = itemView.findViewById(R.id.imageViewDelete)

            fun bind(post: Post) {
                usernameTextView.text = post.username
                descriptionTextView.text = post.description
                Glide.with(itemView.context).load(post.profileImageUrl).into(profileImageView)
                Glide.with(itemView.context).load(post.postImageUrl).into(postImageView)

                editImageView.setOnClickListener {
                    showEditPostDialog(post)
                }

                deleteImageView.setOnClickListener {
                    deletePost(post)
                }
            }

            private fun deletePost(post: Post) {
                post.id?.let { postId ->
                    database.child("posts").child(postId).removeValue().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(context, "Post deleted successfully", Toast.LENGTH_SHORT).show()
                            loadUserPosts()
                        } else {
                            Toast.makeText(context, "Failed to delete post", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun showEditPostDialog(post: Post) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_post, null)
        val descriptionEditText: EditText = dialogView.findViewById(R.id.editTextDescription)
        val postImageView: ImageView = dialogView.findViewById(R.id.imageViewPost)
        postImageViewInDialog = postImageView // Assign the reference
        val saveButton: TextView = dialogView.findViewById(R.id.buttonSave)
        val changeImageButton: TextView = dialogView.findViewById(R.id.buttonChangeImage)

        descriptionEditText.setText(post.description)
        Glide.with(this).load(post.postImageUrl).into(postImageView)

        changeImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, REQUEST_IMAGE)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        saveButton.setOnClickListener {
            val updatedDescription = descriptionEditText.text.toString().trim()
            if (updatedDescription.isEmpty()) {
                Toast.makeText(context, "Description cannot be empty", Toast.LENGTH_SHORT).show()
            } else {
                updatePost(post, updatedDescription)
                dialog.dismiss()
            }
        }

        dialog.show()

        // Load the selected image into ImageView if available
        selectedImageUri?.let {
            Glide.with(this)
                .load(it)
                .into(postImageView)
        }
    }

    private fun updatePost(post: Post, updatedDescription: String) {
        val postId = post.id ?: return

        val postUpdates = mutableMapOf<String, Any>("description" to updatedDescription)

        if (selectedImageUri != null) {
            val postRef = storage.reference.child("posts/${UUID.randomUUID()}.jpg")
            postRef.putFile(selectedImageUri!!)
                .addOnSuccessListener {
                    postRef.downloadUrl.addOnSuccessListener { uri ->
                        postUpdates["postImageUrl"] = uri.toString()
                        savePostUpdates(postId, postUpdates)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to upload image", Toast.LENGTH_SHORT).show()
                }
        } else {
            savePostUpdates(postId, postUpdates)
        }
    }

    private fun savePostUpdates(postId: String, postUpdates: Map<String, Any>) {
        database.child("posts").child(postId).updateChildren(postUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Post updated successfully", Toast.LENGTH_SHORT).show()
                    loadUserPosts()
                } else {
                    Toast.makeText(context, "Failed to update post", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE && resultCode == RESULT_OK) {
            selectedImageUri = data?.data
            selectedImageUri?.let {
                Glide.with(this)
                    .load(it)
                    .into(postImageViewInDialog) // Update the ImageView in the dialog
            }
        }
    }

    companion object {
        private const val REQUEST_IMAGE = 100
    }
}
