package com.examples.rormmanagement.adapter

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.examples.rormmanagement.PromotionInfoActivity
import com.examples.rormmanagement.databinding.CardViewPromotionBinding
import com.examples.rormmanagement.model.Promotion
import com.google.firebase.database.FirebaseDatabase

class PromotionAdapter(
    private val context: Context,
    private val promotionList: ArrayList<Promotion>,
    private val itemClickLauncher: ActivityResultLauncher<Intent>
) : RecyclerView.Adapter<PromotionAdapter.PromotionViewHolder>() {

    var showCheckboxes: Boolean = false
    private val selectedItems = mutableSetOf<Promotion>()

    inner class PromotionViewHolder(private val binding: CardViewPromotionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(promotionItem: Promotion) {
            binding.apply {
                val uriString = promotionItem.image
                val uri = Uri.parse(uriString)

                promotionName.text = promotionItem.name
                expiredDate.text = promotionItem.endDate

                Glide.with(context).load(uri).into(promotionImage)

                switchAvailability.setOnCheckedChangeListener(null)
                switchAvailability.isChecked = promotionItem.available
                updateCardViewBackground(promotionItem.available)

                switchAvailability.setOnCheckedChangeListener { _, isChecked ->
                    promotionItem.available = isChecked
                    updateCardViewBackground(isChecked)
                    updateMenuItemAvailabilityInDatabase(promotionItem)
                }

                promotionCheckbox.visibility = if (showCheckboxes) View.VISIBLE else View.GONE
                promotionCheckbox.isChecked = selectedItems.contains(promotionItem)

                promotionCheckbox.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedItems.add(promotionItem)
                    } else {
                        selectedItems.remove(promotionItem)
                    }
                }

                root.setOnClickListener {
                    val intent = Intent(context, PromotionInfoActivity::class.java).apply {
                        putExtra("promotionItemId", promotionItem.promotionId)
                        putExtra("name", promotionItem.name)
                        putExtra("description", promotionItem.description)
                        putExtra("termsAndConditions", promotionItem.termsAndConditions)
                        putExtra("discount", promotionItem.discount)
                        putExtra("startDate", promotionItem.startDate)
                        putExtra("endDate", promotionItem.endDate)
                        putExtra("image", promotionItem.image)
                        putExtra("isAvailable", promotionItem.available)
                    }
                    itemClickLauncher.launch(intent)
                }
            }
        }

        private fun updateMenuItemAvailabilityInDatabase(promotionItem: Promotion) {
            val database = FirebaseDatabase.getInstance()
            val promotionRef = database.reference.child("promotions").child(promotionItem.promotionId)
            promotionRef.child("available").setValue(promotionItem.available)
                .addOnSuccessListener {
                    Log.d(
                        "PromotionAdapter",
                        "Successfully updated availability for ${promotionItem.name}"
                    )
                }
                .addOnFailureListener { error ->
                    Log.e("PromotionAdapter", "Failed to update availability: ${error.message}")
                }
        }

        private fun updateCardViewBackground(isAvailable: Boolean) {
            if (isAvailable) {
                binding.cardViewPromotion.setCardBackgroundColor(Color.WHITE)
            } else {
                binding.cardViewPromotion.setCardBackgroundColor(Color.GRAY)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PromotionViewHolder {
        val binding = CardViewPromotionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PromotionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PromotionViewHolder, position: Int) {
        val promotionItem = promotionList[position]
        holder.bind(promotionItem)
    }

    override fun getItemCount(): Int {
        return promotionList.size
    }

    fun getSelectedItems(): List<Promotion> {
        return selectedItems.toList()
    }
}
