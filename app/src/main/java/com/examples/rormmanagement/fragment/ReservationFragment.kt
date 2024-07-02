package com.examples.rormmanagement.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.examples.rormmanagement.adapter.ReservationAdapter
import com.examples.rormmanagement.databinding.FragmentReservationBinding
import com.examples.rormmanagement.model.Reservation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class ReservationFragment : Fragment() {

    private var _binding: FragmentReservationBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var reservationAdapterUpcoming: ReservationAdapter
    private lateinit var reservationAdapterPast: ReservationAdapter
    private val upcomingReservations = mutableListOf<Reservation>()
    private val pastReservations = mutableListOf<Reservation>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReservationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        setupRecyclerViews()
        fetchReservations()
    }

    private fun setupRecyclerViews() {
        reservationAdapterUpcoming = ReservationAdapter(upcomingReservations)
        reservationAdapterPast = ReservationAdapter(pastReservations)

        binding.upcomingRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = reservationAdapterUpcoming
        }

        binding.pastRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = reservationAdapterPast
        }
    }

    private fun fetchReservations() {
        val currentUser = auth.currentUser ?: return

        // Get the current restaurant associated with the logged-in user
        database.child("restaurants")
            .orderByChild("userId")
            .equalTo(currentUser.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (restaurantSnapshot in snapshot.children) {
                        val restaurantId = restaurantSnapshot.child("restaurantId").value.toString()
                        getReservationsForRestaurant(restaurantId)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle possible errors
                }
            })
    }

    private fun getReservationsForRestaurant(restaurantId: String) {
        database.child("reservations")
            .orderByChild("restaurantId")
            .equalTo(restaurantId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!isAdded || _binding == null) {
                        return
                    }
                    upcomingReservations.clear()
                    pastReservations.clear()
                    for (reservationSnapshot in snapshot.children) {
                        val reservation = reservationSnapshot.getValue(Reservation::class.java)
                        reservation?.let {
                            if (isUpcoming(it.date)) {
                                upcomingReservations.add(it)
                            } else {
                                pastReservations.add(it)
                            }
                        }
                    }
                    binding.upcoming.text = upcomingReservations.size.toString()
                    binding.past.text = pastReservations.size.toString()
                    reservationAdapterUpcoming.notifyDataSetChanged()
                    reservationAdapterPast.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle possible errors
                }
            })
    }

    private fun isUpcoming(date: Long): Boolean {
        val currentDate = Calendar.getInstance().timeInMillis
        return date >= currentDate
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
