package com.examples.rormmanagement

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.examples.rormmanagement.databinding.ActivityEditRewardsBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class EditRewardsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditRewardsBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var rewardsItemRef: DatabaseReference
    private var rewardsItemId: String? = null
    private var restaurantId: String? = null
    private var image: Uri? = null
    private lateinit var initialImageUri: String

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            binding.selectedImage.setImageURI(uri)
            image = uri
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditRewardsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance()
        rewardsItemId = intent.getStringExtra(REWARDS_ITEM_ID)
        restaurantId = intent.getStringExtra(RESTAURANT_ID)

        if (rewardsItemId != null && restaurantId != null) {
            rewardsItemRef = database.getReference("restaurants")
                .child(restaurantId!!)
                .child("rewards")
                .child(rewardsItemId!!)
        } else {
            Toast.makeText(this, "rewardsItemId or restaurantId is null", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val promotionName = intent.getStringExtra("promotionName")
        val promotionDescription = intent.getStringExtra("promotionDescription")
        val promotionTnc = intent.getStringExtra("promotionTnc")
        val promotionPoints = intent.getStringExtra("promotionPoints")
        val promotionStartDate = intent.getStringExtra("promotionStartDate")
        val promotionEndDate = intent.getStringExtra("promotionEndDate")
        initialImageUri = intent.getStringExtra("image") ?: ""

        binding.promotionName.setText(promotionName)
        binding.promotionDescription.setText(promotionDescription)
        binding.promotionTnc.setText(promotionTnc)
        binding.promotionPoints.setText(promotionPoints)
        binding.startDate.setText(promotionStartDate)
        binding.endDate.setText(promotionEndDate)

        // Load initial image using Glide
        if (initialImageUri.isNotEmpty()) {
            Glide.with(this).load(initialImageUri).into(binding.selectedImage)
        }

        binding.startDate.setOnClickListener { showDatePickerDialog(binding.startDate) }
        binding.endDate.setOnClickListener { showDatePickerDialog(binding.endDate) }

        binding.selectImage.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.saveButton.setOnClickListener {
            if (validateInputs()) {
                saveData()
            }
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
        val points = binding.promotionPoints.text.toString().toDoubleOrNull()

        if (!isEndDateValid()) {
            Toast.makeText(this, "End date cannot be earlier than start date", Toast.LENGTH_SHORT)
                .show()
            return false
        }

        if (points == null || points < 0) {
            Toast.makeText(this, "Please enter valid points", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun isEndDateValid(): Boolean {
        val startDateParts = binding.startDate.text.toString().split("/").map { it.toInt() }
        val endDateParts = binding.endDate.text.toString().split("/").map { it.toInt() }

        val startCalendar = Calendar.getInstance().apply {
            set(startDateParts[2], startDateParts[1] - 1, startDateParts[0])
        }

        val endCalendar = Calendar.getInstance().apply {
            set(endDateParts[2], endDateParts[1] - 1, endDateParts[0])
        }

        return !endCalendar.before(startCalendar)
    }

    private fun saveData() {
        val updatedData = mutableMapOf(
            "name" to binding.promotionName.text.toString(),
            "description" to binding.promotionDescription.text.toString(),
            "termsAndConditions" to binding.promotionTnc.text.toString(),
            "points" to binding.promotionPoints.text.toString(),
            "startDate" to binding.startDate.text.toString(),
            "endDate" to binding.endDate.text.toString(),
            "image" to initialImageUri
        )

        if (image != null) {
            val storageRef = FirebaseStorage.getInstance().reference
            val imageRef = storageRef.child("rewards_images/$rewardsItemId.jpg")
            val uploadTask = imageRef.putFile(image!!)

            uploadTask.addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    updatedData["image"] = downloadUri.toString()
                    updateDatabase(updatedData)
                }.addOnFailureListener {
                    Toast.makeText(this, "Failed to get image URL", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show()
            }
        } else {
            updateDatabase(updatedData)
        }
    }

    private fun updateDatabase(updatedData: Map<String, Any>) {
        rewardsItemRef.updateChildren(updatedData)
            .addOnSuccessListener {
                val updatedIntent = Intent().apply {
                    putExtra(REWARDS_ITEM_ID, rewardsItemId)
                    putExtra("promotionName", updatedData["name"].toString())
                    putExtra("promotionDescription", updatedData["description"].toString())
                    putExtra("promotionTnc", updatedData["termsAndConditions"].toString())
                    putExtra("promotionPoints", updatedData["points"].toString())
                    putExtra("promotionStartDate", updatedData["startDate"].toString())
                    putExtra("promotionEndDate", updatedData["endDate"].toString())
                    putExtra("image", updatedData["image"].toString())
                }
                setResult(Activity.RESULT_OK, updatedIntent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update reward: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    companion object {
        const val REWARDS_ITEM_ID = "rewardsItemId"
        const val RESTAURANT_ID = "restaurantId"
    }
}
