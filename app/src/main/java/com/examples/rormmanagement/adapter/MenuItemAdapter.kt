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
import com.examples.rormmanagement.MenuItemInfoActivity
import com.examples.rormmanagement.databinding.CardViewMenuItemBinding
import com.examples.rormmanagement.model.Menu
import com.google.firebase.database.FirebaseDatabase

class MenuItemAdapter(
    private val context: Context,
    private val menuList: ArrayList<Menu>,
    private val itemClickLauncher: ActivityResultLauncher<Intent>
) : RecyclerView.Adapter<MenuItemAdapter.MenuItemViewHolder>() {

    var showCheckboxes: Boolean = false
    private val selectedItems = mutableSetOf<Menu>()

    inner class MenuItemViewHolder(private val binding: CardViewMenuItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(menuItem: Menu) {
            binding.apply {
                Glide.with(context)
                    .load(Uri.parse(menuItem.foodImage))
                    .into(menuItemImage)

                menuItemName.text = menuItem.foodName
                menuItemPrice.text = menuItem.foodPrice
                switchAvailability.isChecked = menuItem.available
                updateCardViewBackground(menuItem.available)

                root.setOnClickListener {
                    val intent = Intent(context, MenuItemInfoActivity::class.java).apply {
                        putExtra("menuItemId", menuItem.foodId)
                        putExtra("restaurantId", menuItem.restaurantId)
                        putExtra("foodName", menuItem.foodName)
                        putExtra("foodPrice", menuItem.foodPrice)
                        putExtra("foodDescription", menuItem.foodDescription)
                        putExtra("foodIngredients", menuItem.foodIngredients)
                        putExtra("foodImage", menuItem.foodImage)
                        putExtra("isAvailable", menuItem.available)
                    }
                    itemClickLauncher.launch(intent)
                }

                switchAvailability.setOnCheckedChangeListener { _, isChecked ->
                    menuItem.available = isChecked
                    updateCardViewBackground(isChecked)
                    updateMenuItemAvailabilityInDatabase(menuItem)
                }

                menuItemCheckbox.visibility = if (showCheckboxes) View.VISIBLE else View.GONE
                menuItemCheckbox.isChecked = selectedItems.contains(menuItem)

                menuItemCheckbox.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedItems.add(menuItem)
                    } else {
                        selectedItems.remove(menuItem)
                    }
                }
            }
        }

        private fun updateCardViewBackground(isAvailable: Boolean) {
            binding.cardViewMenu.setCardBackgroundColor(if (isAvailable) Color.WHITE else Color.GRAY)
        }

        private fun updateMenuItemAvailabilityInDatabase(menuItem: Menu) {
            val database = FirebaseDatabase.getInstance()
            val menuRef =
                database.reference.child("restaurants").child(menuItem.restaurantId).child("menu")
                    .child(menuItem.foodId)
            menuRef.child("available").setValue(menuItem.available)
                .addOnSuccessListener {
                    Log.d(
                        "MenuItemAdapter",
                        "Successfully updated availability for ${menuItem.foodName}"
                    )
                }
                .addOnFailureListener { error ->
                    Log.e("MenuItemAdapter", "Failed to update availability: ${error.message}")
                }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuItemViewHolder {
        val binding = CardViewMenuItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuItemViewHolder, position: Int) {
        holder.bind(menuList[position])
    }

    override fun getItemCount() = menuList.size

    fun getSelectedItems() = selectedItems.toList()
}
