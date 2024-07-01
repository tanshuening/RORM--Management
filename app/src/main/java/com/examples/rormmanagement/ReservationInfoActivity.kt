package com.examples.rormmanagement

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.examples.rormmanagement.model.Reservation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReservationInfoActivity : AppCompatActivity() {
/*
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reservation_info)

        val reservationId = intent.getStringExtra("reservationId")
        // Fetch the reservation details from the database using reservationId and display them
        // Assume you have a method getReservationById to fetch reservation details
        val reservation = reservationId?.let { getReservationById(it) }

        findViewById<TextView>(R.id.customerName).text = reservation.userId // Assuming userId is the customer name
        findViewById<TextView>(R.id.bookingNumberOfPax).text = reservation.numOfPax.toString()
        findViewById<TextView>(R.id.bookingDate).text = formatDate(reservation.date)
        findViewById<TextView>(R.id.bookingTime).text = reservation.timeSlot
        findViewById<TextView>(R.id.bookingSpecialRequest).text = reservation.specialRequest
        findViewById<TextView>(R.id.bookingPhone).text = reservation.bookingPhone
    }

    fun formatDate(date: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(date))
    }

    private fun getReservationById(reservationId: String): Reservation {
        // Fetch the reservation details from your database
        // Placeholder implementation, replace with your actual database fetching code
        return Reservation()
    }*/
}
