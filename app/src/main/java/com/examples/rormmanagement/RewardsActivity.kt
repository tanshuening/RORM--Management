package com.examples.rormmanagement

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.examples.rormmanagement.adapter.RewardsAdapter
import com.examples.rormmanagement.databinding.ActivityRewardsBinding
import com.examples.rormmanagement.model.Rewards
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class RewardsActivity : AppCompatActivity(), RewardsAdapter.OnItemClickListener {

    private lateinit var databaseReference: DatabaseReference
    private lateinit var database: FirebaseDatabase
    private var rewardsItems: ArrayList<Rewards> = ArrayList()
    private lateinit var adapter: RewardsAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityRewardsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRewardsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().reference
        setupRecyclerView()
        retrieveRewardsItem()

        binding.deleteButton.setOnClickListener {
            toggleCheckboxes()
        }

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.addButton.setOnClickListener {
            val intent = Intent(this, AddRewardsActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        retrieveRewardsItem()
    }

    private fun setupRecyclerView() {
        adapter = RewardsAdapter(this, rewardsItems, this)
        binding.rewardsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.rewardsRecyclerView.adapter = adapter
    }

    override fun onItemClick(rewards: Rewards) {
        val intent = Intent(this, RewardsInfoActivity::class.java).apply {
            putExtra("rewardsItemId", rewards.rewardsId)
            putExtra("restaurantId", rewards.restaurantId)
        }
        startActivity(intent)
    }

    private fun toggleCheckboxes() {
        adapter.showCheckboxes = !adapter.showCheckboxes
        adapter.notifyDataSetChanged()

        if (adapter.showCheckboxes) {
            binding.deleteButton.setImageResource(R.drawable.delete)
            binding.deleteButton.setOnClickListener {
                showDeleteConfirmationDialog()
            }
        } else {
            binding.deleteButton.setImageResource(R.drawable.delete_red)
            binding.deleteButton.setOnClickListener {
                toggleCheckboxes()
            }
        }
    }

    private fun showDeleteConfirmationDialog() {
        val selectedItems = adapter.getSelectedItems()
        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "No items selected", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Delete Items")
            .setMessage("Are you sure you want to delete the selected items?")
            .setPositiveButton("Yes") { _, _ ->
                deleteSelectedItems(selectedItems)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun retrieveRewardsItem() {
        val userId = auth.currentUser?.uid ?: return

        database = FirebaseDatabase.getInstance()
        val restaurantsQuery =
            database.reference.child("restaurants").orderByChild("userId").equalTo(userId)

        restaurantsQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                rewardsItems.clear()
                for (restaurantSnapshot in snapshot.children) {
                    val restaurantId = restaurantSnapshot.key ?: continue
                    val rewardsRef: DatabaseReference =
                        database.reference.child("restaurants").child(restaurantId).child("rewards")

                    rewardsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(rewardsSnapshot: DataSnapshot) {
                            for (rewardsItemSnapshot in rewardsSnapshot.children) {
                                val rewardsItem = rewardsItemSnapshot.getValue(Rewards::class.java)
                                rewardsItem?.let {
                                    if (!rewardsItems.contains(it)) { // Ensure no duplicates
                                        rewardsItems.add(it)
                                    }
                                }
                            }
                            adapter.notifyDataSetChanged()
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.d(
                                "DatabaseError",
                                "Failed to retrieve reward items: ${error.message}"
                            )
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("DatabaseError", "Failed to retrieve restaurants: ${error.message}")
            }
        })
    }

    private fun deleteSelectedItems(selectedItems: List<Rewards>) {
        val userId = auth.currentUser?.uid ?: return

        val database = FirebaseDatabase.getInstance()
        val restaurantsRef =
            database.reference.child("restaurants").orderByChild("userId").equalTo(userId)

        restaurantsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val restaurantSnapshot = snapshot.children.first()
                    val restaurantId = restaurantSnapshot.key ?: return

                    val rewardsRef =
                        database.reference.child("restaurants").child(restaurantId).child("rewards")

                    selectedItems.forEach { rewardsItem ->
                        rewardsRef.child(rewardsItem.rewardsId).removeValue()
                            .addOnSuccessListener {
                                rewardsItems.remove(rewardsItem)
                                adapter.notifyDataSetChanged()
                                Toast.makeText(
                                    this@RewardsActivity,
                                    "${rewardsItem.name} deleted",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { error ->
                                Toast.makeText(
                                    this@RewardsActivity,
                                    "Failed to delete ${rewardsItem.name}: ${error.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                    toggleCheckboxes()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("DatabaseError", "Failed to retrieve restaurant: ${error.message}")
            }
        })
    }
}
