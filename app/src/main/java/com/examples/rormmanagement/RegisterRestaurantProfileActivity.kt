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
import com.examples.rormmanagement.databinding.ActivityRegisterRestaurantProfileBinding
import com.examples.rormmanagement.model.Restaurant
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class RegisterRestaurantProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterRestaurantProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private var startTime: Calendar? = null
    private var endTime: Calendar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterRestaurantProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        setupSpinners()

        // Retrieve the restaurant ID passed from the previous activity
        val restaurantId = intent.getStringExtra("restaurantId") ?: run {
            Toast.makeText(this, "Restaurant ID is missing.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadRestaurantData(restaurantId)

        binding.startTime.setOnClickListener {
            showTimePickerDialog(true)
        }

        binding.endTime.setOnClickListener {
            showTimePickerDialog(false)
        }

        binding.saveButton.setOnClickListener {
            val addressText = binding.address.text.toString()
            val priceText = binding.price.selectedItem.toString()
            val cuisineText = binding.cuisine.getSelectedItems().joinToString(", ")
            val paymentText = binding.payment.getSelectedItems().joinToString(", ")
            val parkingText = binding.parking.selectedItem.toString()
            val dressCodeText = binding.dressCode.selectedItem.toString()
            val descriptionText = binding.description.text.toString()
            val startTimeText = binding.startTime.text.toString()
            val endTimeText = binding.endTime.text.toString()

            if (addressText.isNotEmpty() && priceText.isNotEmpty() && cuisineText.isNotEmpty()
                && paymentText.isNotEmpty() && parkingText.isNotEmpty() && dressCodeText.isNotEmpty()
                && descriptionText.isNotEmpty() && startTimeText.isNotEmpty() && endTimeText.isNotEmpty()) {

                if (endTime != null && startTime != null && endTime!!.before(startTime)) {
                    Toast.makeText(this, "End time cannot be earlier than start time.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val userId = auth.currentUser?.uid ?: run {
                    Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show()
                    Log.e("RegisterRestaurantProfile", "User not authenticated")
                    return@setOnClickListener
                }

                val restaurantUpdate = mapOf(
                    "address" to addressText,
                    "price" to priceText,
                    "cuisine" to cuisineText,
                    "payment" to paymentText,
                    "parking" to parkingText,
                    "dressCode" to dressCodeText,
                    "description" to descriptionText,
                    "startTime" to startTimeText,
                    "endTime" to endTimeText
                )

                database.reference.child("restaurants").child(restaurantId).updateChildren(restaurantUpdate)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                        } else {
                            Toast.makeText(this, "Failed to save data.", Toast.LENGTH_SHORT).show()
                            Log.e("RegisterRestaurantProfile", "Failed to save data", task.exception)
                        }
                    }
            } else {
                Toast.makeText(this, "Please fill all fields.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.backButton.setOnClickListener {
            finish()
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

    private fun loadRestaurantData(restaurantId: String) {
        database.reference.child("restaurants").child(restaurantId).get()
            .addOnSuccessListener { dataSnapshot ->
                val restaurant = dataSnapshot.getValue(Restaurant::class.java)
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
                    binding.startTime.setText(it.businessStartTime)
                    binding.endTime.setText(it.businessEndTime)
                    startTime = parseTime(it.businessStartTime)
                    endTime = parseTime(it.businessEndTime)
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to load restaurant data.", Toast.LENGTH_SHORT).show()
                Log.e("RegisterRestaurantProfile", "Failed to load restaurant data", exception)
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
