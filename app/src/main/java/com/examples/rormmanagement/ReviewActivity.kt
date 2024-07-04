package com.examples.rormmanagement

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.examples.rormmanagement.adapter.ReviewAdapter
import com.examples.rormmanagement.databinding.ActivityReviewBinding
import com.examples.rormmanagement.model.Feedback
import com.google.firebase.database.*

class ReviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReviewBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var feedbackRef: DatabaseReference
    private lateinit var userRef: DatabaseReference
    private lateinit var reviewAdapter: ReviewAdapter
    private val feedbackList = mutableListOf<Feedback>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance()
        feedbackRef = database.getReference("feedbacks")
        userRef = database.getReference("users")

        setupRecyclerView()
        loadFeedbackData()
    }

    private fun setupRecyclerView() {
        reviewAdapter = ReviewAdapter(feedbackList, userRef)
        binding.reviewRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ReviewActivity)
            adapter = reviewAdapter
        }
    }

    private fun loadFeedbackData() {
        feedbackRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                feedbackList.clear()
                for (feedbackSnapshot in snapshot.children) {
                    val feedback = feedbackSnapshot.getValue(Feedback::class.java)
                    if (feedback != null) {
                        // Format the timestamp to dd/MM/yyyy
                        feedback.formattedDate = DateUtils.formatTimestamp(feedback.timestamp)
                        feedbackList.add(feedback)
                        Log.d("ReviewActivity", "Loaded feedback: ${feedback.review}")
                    } else {
                        Log.e("ReviewActivity", "Feedback data is null")
                    }
                }
                reviewAdapter.notifyDataSetChanged()
                Log.d("ReviewActivity", "Feedback list updated with ${feedbackList.size} items")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ReviewActivity", "Failed to load feedback data.", error.toException())
            }
        })
    }
}
