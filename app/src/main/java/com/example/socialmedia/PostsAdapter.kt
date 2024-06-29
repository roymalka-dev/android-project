package com.example.socialmedia

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView

class PostsAdapter : RecyclerView.Adapter<PostsAdapter.PostViewHolder>() {

    private var posts = mutableListOf<Post>()

    fun setPosts(posts: List<Post>) {
        this.posts = posts.toMutableList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        holder.bind(post)
    }

    override fun getItemCount(): Int {
        return posts.size
    }

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImageView: CircleImageView = itemView.findViewById(R.id.imageViewProfile)
        private val usernameTextView: TextView = itemView.findViewById(R.id.textViewUsername)
        private val postImageView: ImageView = itemView.findViewById(R.id.imageViewPost)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.textViewDescription)

        fun bind(post: Post) {
            usernameTextView.text = post.username
            descriptionTextView.text = post.description

            Glide.with(itemView.context)
                .load(post.profileImageUrl)
                .into(profileImageView)

            Glide.with(itemView.context)
                .load(post.postImageUrl)
                .into(postImageView)
        }
    }
}
