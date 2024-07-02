package com.examples.rormmanagement

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.examples.rormmanagement.databinding.ActivityPromotionInfoBinding
import com.examples.rormmanagement.model.Promotion
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class PromotionInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPromotionInfoBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var promotionItemRef: DatabaseReference
    private var promotionItemId: String? = null
    private var restaurantId: String? = null
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPromotionInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()

        promotionItemId = intent.getStringExtra(PROMOTION_ITEM_ID)
        restaurantId = intent.getStringExtra(RESTAURANT_ID)

        if (promotionItemId != null && restaurantId != null) {
            promotionItemRef = database.getReference("restaurants")
                .child(restaurantId!!)
                .child("promotion")
                .child(promotionItemId!!)
            retrieveAndDisplayPromotionItem()
        } else {
            Log.e("PromotionInfoActivity", "PromotionItemId or restaurantId is null")
            finish()
        }

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.editButton.setOnClickListener {
            val intent = Intent(this, EditPromotionInfoActivity::class.java).apply {
                putExtra(PROMOTION_ITEM_ID, promotionItemId)
                putExtra(RESTAURANT_ID, restaurantId)
                putExtra("promotionName", binding.promotionNameTextView.text.toString())
                putExtra("promotionDescription", binding.promotionDescriptionTextView.text.toString())
                putExtra("promotionTnc", binding.promotionTncTextView.text.toString())
                putExtra("promotionDiscount", binding.promotionDiscountTextView.text.toString())
                putExtra("promotionStartDate", binding.promotionStartDateTextView.text.toString())
                putExtra("promotionEndDate", binding.promotionEndDateTextView.text.toString())
                putExtra("image", intent.getStringExtra("image"))
            }
            startActivityForResult(intent, EDIT_PROMOTION_ITEM_REQUEST_CODE)
        }
    }

    private fun retrieveAndDisplayPromotionItem() {
        promotionItemRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val promotionItem = snapshot.getValue(Promotion::class.java)
                    if (promotionItem != null) {
                        displayPromotionItem(promotionItem)
                        Log.d("PromotionInfoActivity", "Promotion retrieved successfully: $promotionItem")
                    } else {
                        Log.e("PromotionInfoActivity", "Promotion is null")
                    }
                } else {
                    Log.e("PromotionInfoActivity", "Promotion does not exist")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("PromotionInfoActivity", "Failed to retrieve promotion", error.toException())
            }
        })
    }

    private fun displayPromotionItem(promotionItem: Promotion) {
        with(binding) {
            promotionNameTextView.setText(promotionItem.name)
            promotionDescriptionTextView.setText(promotionItem.description)
            promotionDiscountTextView.setText(promotionItem.discount)
            promotionTncTextView.setText(promotionItem.termsAndConditions)
            promotionStartDateTextView.setText(promotionItem.startDate)
            promotionEndDateTextView.setText(promotionItem.endDate)
            Glide.with(this@PromotionInfoActivity).load(Uri.parse(promotionItem.image))
                .into(promotionImageView)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EDIT_PROMOTION_ITEM_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            retrieveAndDisplayPromotionItem()
            setResult(Activity.RESULT_OK)
        }
    }

    companion object {
        const val PROMOTION_ITEM_ID = "promotionItemId"
        const val RESTAURANT_ID = "restaurantId"
        const val EDIT_PROMOTION_ITEM_REQUEST_CODE = 1
    }
}
