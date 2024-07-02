package com.examples.rormmanagement

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.examples.rormmanagement.databinding.ActivityAddPromotionItemBinding
import com.examples.rormmanagement.databinding.ActivityAddRewardsBinding
import com.examples.rormmanagement.model.Menu
import com.examples.rormmanagement.model.Promotion
import com.examples.rormmanagement.model.Rewards
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class AddRewardsActivity : AppCompatActivity() {

    private lateinit var promotionName: String
    private lateinit var promotionDescription: String
    private lateinit var promotionTerms: String
    private lateinit var promotionPoints: String
    private lateinit var promotionStartDate: String
    private lateinit var promotionEndDate: String
    private var image: Uri? = null

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var binding: ActivityAddRewardsBinding

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            binding.selectedImage.setImageURI(uri)
            image = uri
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddRewardsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        binding.startDate.setOnClickListener { showDatePickerDialog(binding.startDate) }
        binding.endDate.setOnClickListener { showDatePickerDialog(binding.endDate) }

        binding.addButton.setOnClickListener {
            promotionName = binding.promotionName.text.toString().trim()
            promotionDescription = binding.promotionDescription.text.toString().trim()
            promotionTerms = binding.promotionTnc.text.toString().trim()
            promotionPoints = binding.promotionPoints.text.toString().trim()
            promotionStartDate = binding.startDate.text.toString().trim()
            promotionEndDate = binding.endDate.text.toString().trim()

            if (!(promotionName.isBlank() || promotionDescription.isBlank() || promotionTerms.isBlank() || promotionPoints.isBlank() || promotionStartDate.isBlank() || promotionEndDate.isBlank())) {
                if (validateInputs()) {
                    uploadData()
                }
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        binding.selectImage.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun showDatePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kuala_Lumpur"))
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, monthOfYear, dayOfMonth ->
                val selectedDate = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kuala_Lumpur"))
                selectedDate.set(year, monthOfYear, dayOfMonth)
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("Asia/Kuala_Lumpur")
                editText.setText(sdf.format(selectedDate.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun validateInputs(): Boolean {
        val points = promotionPoints.toDoubleOrNull()

        if (!isEndDateValid()) {
            Toast.makeText(this, "End date cannot be earlier than start date", Toast.LENGTH_SHORT)
                .show()
            return false
        }

/*        if (discount == null || discount < 0 || discount > 100) {
            Toast.makeText(
                this,
                "Please enter a valid discount between 0 and 100",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }*/

        return true
    }

    private fun isEndDateValid(): Boolean {
        val startDateParts = promotionStartDate.split("/").map { it.toInt() }
        val endDateParts = promotionEndDate.split("/").map { it.toInt() }

        val startCalendar = Calendar.getInstance().apply {
            set(startDateParts[2], startDateParts[1] - 1, startDateParts[0])
        }

        val endCalendar = Calendar.getInstance().apply {
            set(endDateParts[2], endDateParts[1] - 1, endDateParts[0])
        }

        return !endCalendar.before(startCalendar)
    }

    private fun uploadData() {

        val userId = auth.currentUser?.uid ?: return
        val restaurantsRef =
            database.getReference("restaurants").orderByChild("userId").equalTo(userId)

        restaurantsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Assuming the user has only one restaurant
                    val restaurantSnapshot = snapshot.children.first()
                    val restaurantId = restaurantSnapshot.key ?: return

                    val promotionRef =
                        database.getReference("restaurants").child(restaurantId).child("rewards")
                    val newItemKey = promotionRef.push().key

                    if (image != null) {
                        val storageRef = FirebaseStorage.getInstance().reference
                        val imageRef = storageRef.child("rewards_images/$newItemKey.jpg")
                        val uploadTask = imageRef.putFile(image!!)

                        uploadTask.addOnSuccessListener {
                            imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                                val rewardsPointsInt = promotionPoints.toIntOrNull() ?: 0

                                val newItem = Rewards(
                                    rewardsId = newItemKey ?: "",
                                    name = promotionName,
                                    description = promotionDescription,
                                    termsAndConditions = promotionTerms,
                                    points = promotionPoints,
                                    startDate = promotionStartDate,
                                    endDate = promotionEndDate,
                                    image = downloadUri.toString(),
                                    userId = userId,
                                    available = true,
                                    restaurantId = restaurantId
                                )
                                newItemKey?.let { key ->
                                    promotionRef.child(key).setValue(newItem).addOnSuccessListener {
                                        Toast.makeText(
                                            this@AddRewardsActivity,
                                            "Voucher added successfully",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        finish()
                                    }.addOnFailureListener {
                                        Toast.makeText(
                                            this@AddRewardsActivity,
                                            "Voucher upload failed",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }.addOnFailureListener {
                                Toast.makeText(
                                    this@AddRewardsActivity,
                                    "Failed to get image URL",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }.addOnFailureListener {
                            Toast.makeText(
                                this@AddRewardsActivity,
                                "Image upload failed",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            this@AddRewardsActivity,
                            "Please select an image",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("DatabaseError", "Failed to retrieve restaurant: ${error.message}")
            }
        })
    }
}