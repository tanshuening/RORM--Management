package com.examples.rormmanagement.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.examples.rormmanagement.databinding.CardViewReservationBinding
import com.examples.rormmanagement.model.Reservation
import com.examples.rormmanagement.model.User
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class ReservationAdapter(private val reservations: List<Reservation>) :
    RecyclerView.Adapter<ReservationAdapter.ViewHolder>() {

    private val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("users")

    inner class ViewHolder(val binding: CardViewReservationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = CardViewReservationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val reservation = reservations[position]
        holder.binding.bookingNumOfPax.text = reservation.numOfPax.toString()
        holder.binding.bookingDate.text = formatDate(reservation.date)
        holder.binding.bookingTime.text = reservation.timeSlot

        // Fetch user data from Firebase
        database.child(reservation.userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(User::class.java)
                holder.binding.customerName.text = user?.name ?: "Unknown"
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("ReservationAdapter", "Failed to load user data", databaseError.toException())
                holder.binding.customerName.text = "Unknown"
            }
        })
    }

    override fun getItemCount() = reservations.size

    private fun formatDate(date: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("ms", "MY"))
        return sdf.format(Date(date))
    }
}
