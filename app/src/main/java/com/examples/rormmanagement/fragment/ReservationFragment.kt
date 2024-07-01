package com.examples.rormmanagement.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.examples.rormmanagement.R
import com.examples.rormmanagement.adapter.ReservationAdapter
import com.examples.rormmanagement.model.Reservation
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class ReservationFragment : Fragment() {

    private lateinit var upcomingRecyclerView: RecyclerView
    private lateinit var pastRecyclerView: RecyclerView
    private lateinit var upcomingReservationAdapter: ReservationAdapter
    private lateinit var pastReservationAdapter: ReservationAdapter
    private val upcomingReservations = mutableListOf<Reservation>()
    private val pastReservations = mutableListOf<Reservation>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_reservation, container, false)

        upcomingRecyclerView = view.findViewById(R.id.upcomingRecyclerView)
        pastRecyclerView = view.findViewById(R.id.pastRecyclerView)

        upcomingRecyclerView.layoutManager = LinearLayoutManager(context)
        pastRecyclerView.layoutManager = LinearLayoutManager(context)

        upcomingReservationAdapter = ReservationAdapter(upcomingReservations)
        pastReservationAdapter = ReservationAdapter(pastReservations)

        upcomingRecyclerView.adapter = upcomingReservationAdapter
        pastRecyclerView.adapter = pastReservationAdapter

        fetchReservations()

        return view
    }

    private fun fetchReservations() {
        val database = FirebaseDatabase.getInstance()
        val reservationsRef = database.getReference("reservations")

        reservationsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                upcomingReservations.clear()
                pastReservations.clear()

                for (snapshot in dataSnapshot.children) {
                    val reservation = snapshot.getValue(Reservation::class.java)
                    reservation?.let {
                        try {
                            val currentDate = Date()
                            val reservationDate = Date(it.date)

                            if (reservationDate.before(currentDate)) {
                                pastReservations.add(it)
                            } else {
                                upcomingReservations.add(it)
                            }
                        } catch (e: Exception) {
                            Log.e("ReservationFragment", "Error processing reservation date", e)
                        }
                    }
                }

                upcomingReservationAdapter.notifyDataSetChanged()
                pastReservationAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("ReservationFragment", "loadReservations:onCancelled", databaseError.toException())
            }
        })
    }
}
