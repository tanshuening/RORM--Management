package com.examples.rormmanagement

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.examples.rormmanagement.databinding.ActivityReservationInfoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReservationInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReservationInfoBinding
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReservationInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val reservationId = intent.getStringExtra("reservationId") ?: ""
        val databaseReference =
            FirebaseDatabase.getInstance().getReference("reservations").child(reservationId)

        binding.backButton.setOnClickListener {
            finish()
        }

        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userId = snapshot.child("userId").getValue(String::class.java) ?: ""
                    val numOfPax = snapshot.child("numOfPax").getValue(Int::class.java) ?: 0
                    val date = snapshot.child("date").getValue(Long::class.java) ?: 0L
                    val timeSlot = snapshot.child("timeSlot").getValue(String::class.java) ?: ""
                    val specialRequest =
                        snapshot.child("specialRequest").getValue(String::class.java) ?: ""
                    val occasion =
                        snapshot.child("bookingOccasion").getValue(String::class.java) ?: ""
                    val phone = snapshot.child("bookingPhone").getValue(String::class.java) ?: ""

                    // Retrieve user details from users node
                    val usersReference =
                        FirebaseDatabase.getInstance().getReference("users").child(userId)
                    usersReference.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(userSnapshot: DataSnapshot) {
                            if (userSnapshot.exists()) {
                                val username =
                                    userSnapshot.child("name").getValue(String::class.java) ?: ""

                                binding.customerName.text = username
                                binding.bookingNumberOfPax.text = numOfPax.toString()
                                binding.bookingDate.text = formatDate(date)
                                binding.bookingTime.text = timeSlot
                                binding.bookingSpecialRequest.text = specialRequest
                                binding.occasion.text = occasion
                                binding.bookingPhone.text = phone
                            } else {
                                // Handle case where user data doesn't exist
                                Toast.makeText(
                                    this@ReservationInfoActivity,
                                    "User data not found",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Handle database error
                            Toast.makeText(
                                this@ReservationInfoActivity,
                                "Database error: ${error.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
                } else {
                    // Handle case where reservation data doesn't exist
                    Toast.makeText(
                        this@ReservationInfoActivity,
                        "Reservation data not found",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
                Toast.makeText(
                    this@ReservationInfoActivity,
                    "Database error: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun formatDate(date: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("ms", "MY"))
        return sdf.format(Date(date))
    }
}
