package com.examples.rormmanagement.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.examples.rormmanagement.R
import com.examples.rormmanagement.model.Reservation
import com.examples.rormmanagement.model.User
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class ReservationAdapter(private val reservations: List<Reservation>) :
    RecyclerView.Adapter<ReservationAdapter.ViewHolder>() {

    private val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("users")

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val customerName: TextView = itemView.findViewById(R.id.customerName)
        val bookingNumOfPax: TextView = itemView.findViewById(R.id.bookingNumOfPax)
        val bookingDate: TextView = itemView.findViewById(R.id.bookingDate)
        val bookingTime: TextView = itemView.findViewById(R.id.bookingTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_view_reservation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val reservation = reservations[position]
        holder.bookingNumOfPax.text = reservation.numOfPax.toString()
        holder.bookingDate.text = formatDate(reservation.date)
        holder.bookingTime.text = reservation.timeSlot

        // Fetch user data from Firebase
        database.child(reservation.userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(User::class.java)
                holder.customerName.text = user?.name ?: "Unknown"
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("ReservationAdapter", "Failed to load user data", databaseError.toException())
                holder.customerName.text = "Unknown"
            }
        })
    }

    override fun getItemCount() = reservations.size

    private fun formatDate(date: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(date))
    }
}
