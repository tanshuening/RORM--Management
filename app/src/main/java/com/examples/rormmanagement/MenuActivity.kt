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
import com.examples.rormmanagement.adapter.MenuItemAdapter
import com.examples.rormmanagement.databinding.ActivityMenuBinding
import com.examples.rormmanagement.model.Menu
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MenuActivity : AppCompatActivity() {

    private lateinit var databaseReference: DatabaseReference
    private lateinit var database: FirebaseDatabase
    private var menuItems: ArrayList<Menu> = ArrayList()
    private lateinit var adapter: MenuItemAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().reference
        setupRecyclerView()
        retrieveMenuItem()

        binding.deleteButton.setOnClickListener {
            toggleCheckboxes()
        }

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.addButton.setOnClickListener {
            val intent = Intent(this, AddMenuItemActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        retrieveMenuItem()
    }

    private fun setupRecyclerView() {
        adapter = MenuItemAdapter(this, menuItems, itemClickLauncher)
        binding.menuItemRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.menuItemRecyclerView.adapter = adapter
    }

    private val itemClickLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                retrieveMenuItem()
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

    private fun retrieveMenuItem() {
        val userId = auth.currentUser?.uid ?: return

        database = FirebaseDatabase.getInstance()
        val restaurantsQuery =
            database.reference.child("restaurants").orderByChild("userId").equalTo(userId)

        restaurantsQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                menuItems.clear()
                for (restaurantSnapshot in snapshot.children) {
                    val restaurantId = restaurantSnapshot.key ?: continue
                    val menuRef: DatabaseReference =
                        database.reference.child("restaurants").child(restaurantId).child("menu")

                    menuRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(menuSnapshot: DataSnapshot) {
                            for (menuItemSnapshot in menuSnapshot.children) {
                                val menuItem = menuItemSnapshot.getValue(Menu::class.java)
                                menuItem?.let {
                                    if (!menuItems.contains(it)) { // Ensure no duplicates
                                        menuItems.add(it)
                                    }
                                }
                            }
                            adapter.notifyDataSetChanged()
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.d(
                                "DatabaseError",
                                "Failed to retrieve menu items: ${error.message}"
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

    private fun deleteSelectedItems(selectedItems: List<Menu>) {
        val userId = auth.currentUser?.uid ?: return

        val database = FirebaseDatabase.getInstance()
        val restaurantsRef =
            database.reference.child("restaurants").orderByChild("userId").equalTo(userId)

        restaurantsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val restaurantSnapshot = snapshot.children.first()
                    val restaurantId = restaurantSnapshot.key ?: return

                    val menuRef =
                        database.reference.child("restaurants").child(restaurantId).child("menu")

                    selectedItems.forEach { menuItem ->
                        menuRef.child(menuItem.foodId).removeValue()
                            .addOnSuccessListener {
                                menuItems.remove(menuItem)
                                adapter.notifyDataSetChanged()
                                Toast.makeText(
                                    this@MenuActivity,
                                    "${menuItem.foodName} deleted",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { error ->
                                Toast.makeText(
                                    this@MenuActivity,
                                    "Failed to delete ${menuItem.foodName}: ${error.message}",
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
