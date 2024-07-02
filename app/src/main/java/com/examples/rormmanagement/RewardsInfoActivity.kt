package com.examples.rormmanagement

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.examples.rormmanagement.databinding.ActivityRewardsBinding
import com.examples.rormmanagement.databinding.ActivityRewardsInfoBinding
import com.examples.rormmanagement.model.Rewards
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class RewardsInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRewardsInfoBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var rewardsItemRef: DatabaseReference
    private var rewardsItemId: String? = null
    private var restaurantId: String? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var imageUrl: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRewardsInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()

        rewardsItemId = intent.getStringExtra(REWARDS_ITEM_ID)
        restaurantId = intent.getStringExtra(RESTAURANT_ID)

        if (rewardsItemId != null && restaurantId != null) {
            rewardsItemRef = database.getReference("restaurants")
                .child(restaurantId!!)
                .child("rewards")
                .child(rewardsItemId!!)
            retrieveAndDisplayRewardsItem()
        } else {
            Log.e("RewardsInfoActivity", "RewardsItemId or restaurantId is null")
            finish()
        }

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.editButton.setOnClickListener {
            val intent = Intent(this, EditRewardsActivity::class.java).apply {
                putExtra(REWARDS_ITEM_ID, rewardsItemId)
                putExtra(RESTAURANT_ID, restaurantId)
                putExtra("promotionName", binding.promotionNameTextView.text.toString())
                putExtra("promotionDescription", binding.promotionDescriptionTextView.text.toString())
                putExtra("promotionTnc", binding.promotionTncTextView.text.toString())
                putExtra("promotionPoints", binding.promotionPointsTextView.text.toString())
                putExtra("promotionStartDate", binding.promotionStartDateTextView.text.toString())
                putExtra("promotionEndDate", binding.promotionEndDateTextView.text.toString())
                putExtra("image", imageUrl) // Pass the correct image URL
            }
            startActivityForResult(intent, EDIT_REWARDS_ITEM_REQUEST_CODE)
        }
    }

    private fun retrieveAndDisplayRewardsItem() {
        rewardsItemRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val rewardsItem = snapshot.getValue(Rewards::class.java)
                    if (rewardsItem != null) {
                        displayRewardsItem(rewardsItem)
                        Log.d("RewardsInfoActivity", "Reward retrieved successfully: $rewardsItem")
                    } else {
                        Log.e("RewardsInfoActivity", "Reward is null")
                    }
                } else {
                    Log.e("RewardsInfoActivity", "Reward does not exist")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("RewardsInfoActivity", "Failed to retrieve rewards", error.toException())
            }
        })
    }

    private fun displayRewardsItem(rewardsItem: Rewards) {
        with(binding) {
            promotionNameTextView.setText(rewardsItem.name)
            promotionDescriptionTextView.setText(rewardsItem.description)
            promotionPointsTextView.setText(rewardsItem.points)
            promotionTncTextView.setText(rewardsItem.termsAndConditions)
            promotionStartDateTextView.setText(rewardsItem.startDate)
            promotionEndDateTextView.setText(rewardsItem.endDate)
            imageUrl = rewardsItem.image
            Glide.with(this@RewardsInfoActivity).load(Uri.parse(imageUrl))
                .into(promotionImageView)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EDIT_REWARDS_ITEM_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            retrieveAndDisplayRewardsItem()
            setResult(Activity.RESULT_OK)
        }
    }

    companion object {
        const val REWARDS_ITEM_ID = "rewardsItemId"
        const val RESTAURANT_ID = "restaurantId"
        const val EDIT_REWARDS_ITEM_REQUEST_CODE = 1
    }
}
