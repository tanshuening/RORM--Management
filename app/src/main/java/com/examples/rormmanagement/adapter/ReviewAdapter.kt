package com.examples.rormmanagement.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.examples.rormmanagement.databinding.CardViewFeedbackBinding

class ReviewAdapter(
    private val customerNames: ArrayList<String>,
    //private val profilePictures: ArrayList<Int>,
    private val dates: ArrayList<String>,
    private val reviews: ArrayList<String>
) : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = CardViewFeedbackBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val customerName = customerNames[position]
        //val profilePicture = profilePictures[position]
        val date = dates[position]
        val review = reviews[position]
        holder.bind(customerName, date, review)
    }

    override fun getItemCount(): Int {
        return customerNames.size
    }

    class ReviewViewHolder(private val binding: CardViewFeedbackBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(customerName: String, date: String, review: String) {
            binding.customerName.text = customerName
            binding.date.text = date
            binding.review.text = review
            //binding.profilePicture.setImageResource(profilePicture)
        }
    }

}
