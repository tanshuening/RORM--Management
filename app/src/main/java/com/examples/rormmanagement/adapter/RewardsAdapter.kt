package com.examples.rormmanagement.adapter

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.examples.rormmanagement.databinding.CardViewRewardsBinding
import com.examples.rormmanagement.model.Rewards
import com.google.firebase.database.FirebaseDatabase

class RewardsAdapter(
    private val context: Context,
    private val rewardsList: ArrayList<Rewards>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<RewardsAdapter.RewardsViewHolder>() {

    var showCheckboxes: Boolean = false
    private val selectedItems = mutableSetOf<Rewards>()

    inner class RewardsViewHolder(private val binding: CardViewRewardsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(rewardsItem: Rewards) {
            binding.apply {
                val uriString = rewardsItem.image
                val uri = Uri.parse(uriString)

                promotionName.text = rewardsItem.name
                expiredDate.text = rewardsItem.endDate

                Glide.with(context).load(uri).into(promotionImage)

                switchAvailability.setOnCheckedChangeListener(null)
                switchAvailability.isChecked = rewardsItem.available
                updateCardViewBackground(rewardsItem.available)

                switchAvailability.setOnCheckedChangeListener { _, isChecked ->
                    rewardsItem.available = isChecked
                    updateCardViewBackground(isChecked)
                    updateMenuItemAvailabilityInDatabase(rewardsItem)
                }

                promotionCheckbox.visibility = if (showCheckboxes) View.VISIBLE else View.GONE
                promotionCheckbox.isChecked = selectedItems.contains(rewardsItem)

                promotionCheckbox.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedItems.add(rewardsItem)
                    } else {
                        selectedItems.remove(rewardsItem)
                    }
                }

                root.setOnClickListener {
                    listener.onItemClick(rewardsItem)
                }
            }
        }

        private fun updateMenuItemAvailabilityInDatabase(rewardsItem: Rewards) {
            val database = FirebaseDatabase.getInstance()
            val promotionRef = database.reference.child("rewards").child(rewardsItem.rewardsId)
            promotionRef.child("available").setValue(rewardsItem.available)
                .addOnSuccessListener {
                    Log.d(
                        "RewardsAdapter",
                        "Successfully updated availability for ${rewardsItem.name}"
                    )
                }
                .addOnFailureListener { error ->
                    Log.e("RewardsAdapter", "Failed to update availability: ${error.message}")
                }
        }

        private fun updateCardViewBackground(isAvailable: Boolean) {
            if (isAvailable) {
                binding.cardViewRewards.setCardBackgroundColor(Color.WHITE)
            } else {
                binding.cardViewRewards.setCardBackgroundColor(Color.GRAY)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RewardsViewHolder {
        val binding = CardViewRewardsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RewardsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RewardsViewHolder, position: Int) {
        val rewardsItem = rewardsList[position]
        holder.bind(rewardsItem)
    }

    override fun getItemCount(): Int {
        return rewardsList.size
    }

    fun getSelectedItems(): List<Rewards> {
        return selectedItems.toList()
    }

    interface OnItemClickListener {
        fun onItemClick(rewards: Rewards)
    }
}