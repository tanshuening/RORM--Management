package com.examples.rormmanagement

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.examples.rormmanagement.databinding.ActivityMenuItemInfoBinding
import com.examples.rormmanagement.model.Menu
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MenuItemInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuItemInfoBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var menuItemRef: DatabaseReference
    private var menuItemId: String? = null
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuItemInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()

        menuItemId = intent.getStringExtra("menuItemId")
        val restaurantId = intent.getStringExtra("restaurantId") // Assuming restaurantId is passed via intent

        if (menuItemId != null && restaurantId != null) {
            menuItemRef = database.getReference("restaurants").child(restaurantId).child("menu").child(menuItemId!!)
            retrieveAndDisplayMenuItem()
        } else {
            Log.e("MenuItemInfoActivity", "MenuItemId or restaurantId is null")
            finish()
        }

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.editButton.setOnClickListener {
            val intent = Intent(this, EditMenuItemInfoActivity::class.java).apply {
                putExtra("menuItemId", menuItemId)
                putExtra("restaurantId", restaurantId)
                putExtra("foodName", binding.menuItemNameTextView.text.toString())
                putExtra("foodPrice", binding.menuItemPriceTextView.text.toString())
                putExtra("foodDescription", binding.menuItemDescriptionTextView.text.toString())
                putExtra("foodIngredients", binding.menuItemIngredientsTextView.text.toString())
                putExtra("foodImage", intent.getStringExtra("foodImage"))
            }
            startActivityForResult(intent, EDIT_MENU_ITEM_REQUEST_CODE)
        }
    }

    private fun retrieveAndDisplayMenuItem() {
        menuItemRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val menuItem = snapshot.getValue(Menu::class.java)
                    if (menuItem != null) {
                        displayMenuItem(menuItem)
                        Log.d("MenuItemInfoActivity", "Menu item retrieved successfully: $menuItem")
                    } else {
                        Log.e("MenuItemInfoActivity", "Menu item is null")
                    }
                } else {
                    Log.e("MenuItemInfoActivity", "Menu item does not exist")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MenuItemInfoActivity", "Failed to retrieve menu item", error.toException())
            }
        })
    }

    private fun displayMenuItem(menuItem: Menu) {
        with(binding) {
            menuItemNameTextView.setText(menuItem.foodName)
            menuItemPriceTextView.setText(menuItem.foodPrice)
            menuItemDescriptionTextView.setText(menuItem.foodDescription)
            menuItemIngredientsTextView.setText(menuItem.foodIngredients)
            Glide.with(this@MenuItemInfoActivity).load(Uri.parse(menuItem.foodImage))
                .into(menuItemImageView)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EDIT_MENU_ITEM_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            retrieveAndDisplayMenuItem()
            setResult(Activity.RESULT_OK)
        }
    }

    companion object {
        const val EDIT_MENU_ITEM_REQUEST_CODE = 1
    }
}
