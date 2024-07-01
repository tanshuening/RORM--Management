package com.examples.rormmanagement

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.examples.rormmanagement.adapter.ReviewAdapter
import com.examples.rormmanagement.databinding.ActivityReviewBinding

class ReviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReviewBinding
    private lateinit var reviewAdapter: ReviewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            onBackPressed()
        }

        val customerName = arrayListOf("Tan Shue Ning")
        val date = arrayListOf("01/07/2024")
        val review = arrayListOf("Review 1...")
        //val profilePicture = arrayListOf(R.drawable.profile_pic, R.drawable.profile_pic, R.drawable.profile_pic)
        reviewAdapter = ReviewAdapter(customerName, date, review)

        binding.reviewRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.reviewRecyclerView.adapter = reviewAdapter

    }
}