package com.examples.rormmanagement

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.examples.rormmanagement.adapter.PromotionAdapter
import com.examples.rormmanagement.databinding.ActivityPromotionBinding
import com.examples.rormmanagement.model.Promotion
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class PromotionActivity : AppCompatActivity() {

    private lateinit var databaseReference: DatabaseReference
    private lateinit var database: FirebaseDatabase
    private var promotionItems: ArrayList<Promotion> = ArrayList()
    private lateinit var adapter: PromotionAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityPromotionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPromotionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().reference
        setupRecyclerView()
        retrievePromotionItem()

        binding.deleteButton.setOnClickListener {
            toggleCheckboxes()
        }

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.addButton.setOnClickListener {
            val intent = Intent(this, AddPromotionItemActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        retrievePromotionItem()
    }

    private fun setupRecyclerView() {
        adapter = PromotionAdapter(this, promotionItems, itemClickLauncher)
        binding.promotionRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.promotionRecyclerView.adapter = adapter
    }

    private val itemClickLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                retrievePromotionItem()
            }
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

    private fun retrievePromotionItem() {
        val userId = auth.currentUser?.uid ?: return

        database = FirebaseDatabase.getInstance()
        val restaurantsQuery =
            database.reference.child("restaurants").orderByChild("userId").equalTo(userId)

        restaurantsQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                promotionItems.clear()
                for (restaurantSnapshot in snapshot.children) {
                    val restaurantId = restaurantSnapshot.key ?: continue
                    val promotionRef: DatabaseReference =
                        database.reference.child("restaurants").child(restaurantId).child("promotion")

                    promotionRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(promotionSnapshot: DataSnapshot) {
                            for (promotionItemSnapshot in promotionSnapshot.children) {
                                val promotionItem = promotionItemSnapshot.getValue(Promotion::class.java)
                                promotionItem?.let {
                                    if (!promotionItems.contains(it)) { // Ensure no duplicates
                                        promotionItems.add(it)
                                    }
                                }
                            }
                            adapter.notifyDataSetChanged()
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.d(
                                "DatabaseError",
                                "Failed to retrieve promotion items: ${error.message}"
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

    private fun deleteSelectedItems(selectedItems: List<Promotion>) {
        val userId = auth.currentUser?.uid ?: return

        val database = FirebaseDatabase.getInstance()
        val restaurantsRef =
            database.reference.child("restaurants").orderByChild("userId").equalTo(userId)

        restaurantsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val restaurantSnapshot = snapshot.children.first()
                    val restaurantId = restaurantSnapshot.key ?: return

                    val promotionRef =
                        database.reference.child("restaurants").child(restaurantId).child("promotion")

                    selectedItems.forEach { promotionItem ->
                        promotionRef.child(promotionItem.promotionId).removeValue()
                            .addOnSuccessListener {
                                promotionItems.remove(promotionItem)
                                adapter.notifyDataSetChanged()
                                Toast.makeText(
                                    this@PromotionActivity,
                                    "${promotionItem.name} deleted",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { error ->
                                Toast.makeText(
                                    this@PromotionActivity,
                                    "Failed to delete ${promotionItem.name}: ${error.message}",
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
