package com.examples.rormmanagement

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.examples.rormmanagement.databinding.ActivityRestaurantProfileBinding
import com.examples.rormmanagement.model.Restaurant
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class RestaurantProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRestaurantProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var currentRestaurant: Restaurant? = null

    private var startTime: Calendar? = null
    private var endTime: Calendar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRestaurantProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        setupSpinners()
        loadRestaurantData()

        binding.saveButton.setOnClickListener {
            saveRestaurantData()
        }

        binding.backButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupSpinners() {
        // Price Spinner
        ArrayAdapter.createFromResource(
            this,
            R.array.price_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.price.adapter = adapter
        }

        // Cuisine Spinner (MultiSelect)
        val cuisines = resources.getStringArray(R.array.cuisine_options)
        binding.cuisine.setItems(cuisines)

        // Payment Spinner (MultiSelect)
        val payments = resources.getStringArray(R.array.payment_options)
        binding.payment.setItems(payments)

        // Parking Spinner
        ArrayAdapter.createFromResource(
            this,
            R.array.parking_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.parking.adapter = adapter
        }

        // Dress Code Spinner
        ArrayAdapter.createFromResource(
            this,
            R.array.dress_code_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.dressCode.adapter = adapter
        }
    }

    private fun loadRestaurantData() {
        val userId = auth.currentUser?.uid ?: return
        database.reference.child("restaurants").orderByChild("userId").equalTo(userId).get()
            .addOnSuccessListener { dataSnapshot ->
                val restaurant = dataSnapshot.children.firstOrNull()?.getValue(Restaurant::class.java)
                currentRestaurant = restaurant
                restaurant?.let {
                    binding.address.setText(it.address)
                    binding.price.setSelection(getIndex(binding.price, it.price))
                    it.cuisine?.split(", ")?.let { cuisines ->
                        binding.cuisine.setSelectedItems(cuisines)
                    }
                    it.payment?.split(", ")?.let { payments ->
                        binding.payment.setSelectedItems(payments)
                    }
                    binding.parking.setSelection(getIndex(binding.parking, it.parking))
                    binding.dressCode.setSelection(getIndex(binding.dressCode, it.dressCode))
                    binding.description.setText(it.description)

                    // Use parseTime to set start and end times
                    parseTime(it.businessStartTime)?.let { startTime ->
                        this.startTime = startTime
                        binding.startTime.setText(DateFormat.format("hh:mm a", startTime))
                    }
                    parseTime(it.businessEndTime)?.let { endTime ->
                        this.endTime = endTime
                        binding.endTime.setText(DateFormat.format("hh:mm a", endTime))
                    }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to load restaurant data", Toast.LENGTH_SHORT).show()
                Log.e("RestaurantProfile", "Failed to load restaurant data", exception)
            }
    }


    private fun saveRestaurantData() {
        val userId = auth.currentUser?.uid ?: return
        database.reference.child("restaurants").orderByChild("userId").equalTo(userId).get()
            .addOnSuccessListener { dataSnapshot ->
                val restaurantId = dataSnapshot.children.firstOrNull()?.key
                if (restaurantId != null) {
                    val restaurant = Restaurant(
                        restaurantId = restaurantId,
                        userId = userId,
                        address = binding.address.text.toString(),
                        price = binding.price.selectedItem.toString(),
                        cuisine = binding.cuisine.getSelectedItems().joinToString(", "),
                        payment = binding.payment.getSelectedItems().joinToString(", "),
                        parking = binding.parking.selectedItem.toString(),
                        dressCode = binding.dressCode.selectedItem.toString(),
                        description = binding.description.text.toString(),
                        businessStartTime = binding.startTime.text.toString(),
                        businessEndTime = binding.endTime.text.toString()
                    )

                    database.reference.child("restaurants").child(restaurantId).setValue(restaurant)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Restaurant data saved", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(this, "Failed to save restaurant data", Toast.LENGTH_SHORT).show()
                            Log.e("RestaurantProfile", "Failed to save restaurant data", exception)
                        }
                } else {
                    Toast.makeText(this, "Restaurant not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to load restaurant data", Toast.LENGTH_SHORT).show()
                Log.e("RestaurantProfile", "Failed to load restaurant data", exception)
            }
    }


    private fun getIndex(spinner: Spinner, value: String?): Int {
        for (i in 0 until spinner.count) {
            if (spinner.getItemAtPosition(i) == value) {
                return i
            }
        }
        return 0
    }

    private fun showTimePickerDialog(isStartTime: Boolean) {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kuala_Lumpur"), Locale.getDefault())
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timeSetListener = TimePickerDialog.OnTimeSetListener { _, selectedHour, selectedMinute ->
            calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
            calendar.set(Calendar.MINUTE, selectedMinute)

            val formattedTime = DateFormat.format("hh:mm a", calendar).toString()

            if (isStartTime) {
                startTime = calendar
                binding.startTime.setText(formattedTime)
            } else {
                endTime = calendar
                binding.endTime.setText(formattedTime)
            }
        }

        TimePickerDialog(
            this,
            timeSetListener,
            hour,
            minute,
            false
        ).show()
    }

    private fun parseTime(time: String?): Calendar? {
        return try {
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kuala_Lumpur"), Locale.getDefault())
            val dateFormat = DateFormat.getTimeFormat(this)
            calendar.time = dateFormat.parse(time)
            calendar
        } catch (e: Exception) {
            null
        }
    }

}
