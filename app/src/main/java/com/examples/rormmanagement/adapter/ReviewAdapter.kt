package com.examples.rormmanagement.adapter

import com.examples.rormmanagement.R
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.examples.rormmanagement.databinding.CardViewFeedbackBinding
import com.examples.rormmanagement.model.Feedback
import com.examples.rormmanagement.model.User
import com.google.firebase.database.*

class ReviewAdapter(
    private val feedbackList: List<Feedback>,
    private val userRef: DatabaseReference
) : RecyclerView.Adapter<ReviewAdapter.FeedbackViewHolder>() {

    inner class FeedbackViewHolder(val binding: CardViewFeedbackBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedbackViewHolder {
        val binding = CardViewFeedbackBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FeedbackViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeedbackViewHolder, position: Int) {
        val feedback = feedbackList[position]
        holder.binding.apply {
            review.text = feedback.review
            date.text = feedback.formattedDate
            Log.d("ReviewAdapter", "Review: ${feedback.review}, Date: ${feedback.formattedDate}")
            loadUserData(feedback.userId, holder)
            setRatingStars(feedback.rating, holder)
        }
    }

    override fun getItemCount(): Int = feedbackList.size

    private fun loadUserData(userId: String?, holder: FeedbackViewHolder) {
        if (userId != null) {
            userRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    if (user != null) {
                        holder.binding.customerName.text = user.name
                        Log.d("ReviewAdapter", "Customer Name: ${user.name}")
                        if (!user.profileImageUrl.isNullOrEmpty()) {
                            Glide.with(holder.itemView.context)
                                .load(user.profileImageUrl)
                                .into(holder.binding.profilePicture)
                            Log.d("ReviewAdapter", "Profile Image URL: ${user.profileImageUrl}")
                        }
                    } else {
                        Log.e("ReviewAdapter", "User data is null for userId: $userId")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ReviewAdapter", "Failed to load user data.", error.toException())
                }
            })
        }
    }

    private fun setRatingStars(rating: Int, holder: FeedbackViewHolder) {
        val stars = listOf(
            holder.binding.ratingStar1,
            holder.binding.ratingStar2,
            holder.binding.ratingStar3,
            holder.binding.ratingStar4,
            holder.binding.ratingStar5
        )

        for (i in stars.indices) {
            if (i < rating) {
                stars[i].setImageResource(R.drawable.star_filled)
                Log.d("ReviewAdapter", "Star $i set to filled for rating $rating")
            } else {
                stars[i].setImageResource(R.drawable.star_default)
                Log.d("ReviewAdapter", "Star $i set to default for rating $rating")
            }
        }
    }
}
