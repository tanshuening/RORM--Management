package com.examples.rormmanagement

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.examples.rormmanagement.databinding.ActivityRestaurantProfileBinding
import com.examples.rormmanagement.model.Restaurant
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class RestaurantProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRestaurantProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private var restaurantImage: Uri? = null
    private var currentRestaurant: Restaurant? = null

    private var startTime: Calendar? = null
    private var endTime: Calendar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRestaurantProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        setupLocationSpinner()
        setupSpinners()
        loadRestaurantData()

        val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                binding.selectedImage.setImageURI(uri)
                restaurantImage = uri
            }
        }

        binding.selectImage.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.saveButton.setOnClickListener {
            saveRestaurantData()
        }

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.startTime.setOnClickListener {
            showTimePicker { time ->
                startTime = time
                binding.startTime.setText(DateFormat.format("hh:mm a", time))
            }
        }

        binding.endTime.setOnClickListener {
            showTimePicker { time ->
                endTime = time
                binding.endTime.setText(DateFormat.format("hh:mm a", time))
            }
        }
    }

    private fun setupLocationSpinner() {
        val locationOptions = resources.getStringArray(R.array.malaysia_states)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, locationOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.listOfLocation.setAdapter(adapter)
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
        database.reference.child("restaurants").orderByChild("userId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val restaurant = dataSnapshot.children.firstOrNull()?.getValue(Restaurant::class.java)
                    currentRestaurant = restaurant
                    restaurant?.let {
                        Log.d("RestaurantProfile", "Restaurant data: $it")

                        binding.restaurantName.setText(it.name)
                        binding.address.setText(it.address ?: "")

                        binding.price.setSelection(getIndex(binding.price, it.price ?: ""))

                        it.cuisine?.split(", ")?.let { cuisines ->
                            binding.cuisine.setSelectedItems(cuisines)
                        }

                        it.payment?.split(", ")?.let { payments ->
                            binding.payment.setSelectedItems(payments)
                        }

                        binding.parking.setSelection(getIndex(binding.parking, it.parking ?: ""))

                        binding.dressCode.setSelection(getIndex(binding.dressCode, it.dressCode ?: ""))

                        binding.description.setText(it.description ?: "")

                        it.startTime?.let { startTimeString ->
                            parseTime(startTimeString)?.let { startTime ->
                                this@RestaurantProfileActivity.startTime = startTime
                                binding.startTime.setText(DateFormat.format("hh:mm a", startTime))
                            }
                        }

                        it.endTime?.let { endTimeString ->
                            parseTime(endTimeString)?.let { endTime ->
                                this@RestaurantProfileActivity.endTime = endTime
                                binding.endTime.setText(DateFormat.format("hh:mm a", endTime))
                            }
                        }

                        // Load image if exists
                        if (!it.images.isNullOrEmpty()) {
                            Glide.with(this@RestaurantProfileActivity).load(it.images.first()).into(binding.selectedImage)
                        }

                        val locationArray = resources.getStringArray(R.array.malaysia_states)
                        val locationPosition = locationArray.indexOf(restaurant.location)
                        if (locationPosition != -1) {
                            binding.listOfLocation.setText(locationArray[locationPosition], false)
                        } else {
                            binding.listOfLocation.setText("", false)
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(this@RestaurantProfileActivity, "Failed to load restaurant data", Toast.LENGTH_SHORT).show()
                    Log.e("RestaurantProfile", "Failed to load restaurant data", databaseError.toException())
                }
            })
    }

    private fun getIndex(spinner: Spinner, value: String?): Int {
        for (i in 0 until spinner.count) {
            if (spinner.getItemAtPosition(i) == value) {
                return i
            }
        }
        return 0
    }

    private fun saveRestaurantData() {
        val userId = auth.currentUser?.uid ?: return
        database.reference.child("restaurants").orderByChild("userId").equalTo(userId).get()
            .addOnSuccessListener { dataSnapshot ->
                val restaurantId = dataSnapshot.children.firstOrNull()?.key
                if (restaurantId != null) {
                    if (restaurantImage != null) {
                        uploadImageToStorage(restaurantImage!!) { imageUrl ->
                            saveRestaurantToDatabase(userId, restaurantId, imageUrl)
                        }
                    } else {
                        val existingImage = currentRestaurant?.images?.firstOrNull() ?: ""
                        saveRestaurantToDatabase(userId, restaurantId, existingImage)
                    }
                }
            }
    }

    private fun saveRestaurantToDatabase(userId: String, restaurantId: String, imageUrl: String) {
        val restaurant = Restaurant(
            restaurantId = restaurantId,
            userId = userId,
            name = binding.restaurantName.text.toString(),
            address = binding.address.text.toString(),
            price = binding.price.selectedItem.toString(),
            cuisine = binding.cuisine.getSelectedItems().joinToString(", "),
            payment = binding.payment.getSelectedItems().joinToString(", "),
            parking = binding.parking.selectedItem.toString(),
            dressCode = binding.dressCode.selectedItem.toString(),
            description = binding.description.text.toString(),
            startTime = formatTime(startTime),
            endTime = formatTime(endTime),
            location = binding.listOfLocation.text.toString(),
            images = listOf(imageUrl)
        )

        database.reference.child("restaurants").child(restaurantId).setValue(restaurant)
            .addOnSuccessListener {
                Toast.makeText(this, "Restaurant data saved", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to save restaurant data", Toast.LENGTH_SHORT).show()
                Log.e("RestaurantProfile", "Failed to save restaurant data", exception)
            }
    }

    private fun showTimePicker(onTimeSelected: (Calendar) -> Unit) {
        val now = Calendar.getInstance()
        val timePicker = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val selectedTime = Calendar.getInstance()
                selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedTime.set(Calendar.MINUTE, minute)
                onTimeSelected(selectedTime)
            },
            now.get(Calendar.HOUR_OF_DAY),
            now.get(Calendar.MINUTE),
            false
        )
        timePicker.show()
    }

    private fun formatTime(calendar: Calendar?): String? {
        return calendar?.let {
            DateFormat.format("HH:mm", it).toString()
        }
    }

    private fun parseTime(timeString: String?): Calendar? {
        return timeString?.let {
            try {
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                val date = sdf.parse(it)
                val calendar = Calendar.getInstance()
                calendar.time = date
                calendar
            } catch (e: ParseException) {
                null
            }
        }
    }

    private fun uploadImageToStorage(uri: Uri, onSuccess: (String) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val storageRef: StorageReference = storage.reference.child("restaurant_images/$userId/${uri.lastPathSegment}")
        val uploadTask = storageRef.putFile(uri)

        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let { throw it }
            }
            storageRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                onSuccess(downloadUri.toString())
            } else {
                Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show()
                Log.e("RestaurantProfile", "Image upload failed", task.exception)
            }
        }
    }
}
