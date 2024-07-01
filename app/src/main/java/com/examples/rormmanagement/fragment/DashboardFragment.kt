package com.examples.rormmanagement.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.examples.rormmanagement.MenuActivity
import com.examples.rormmanagement.ProfileActivity
import com.examples.rormmanagement.PromotionActivity
import com.examples.rormmanagement.ReviewActivity
import com.examples.rormmanagement.databinding.FragmentDashboardBinding
import com.examples.rormmanagement.model.Restaurant
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class DashboardFragment : Fragment() {

    private lateinit var binding: FragmentDashboardBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var restaurantRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        val userId = auth.currentUser?.uid ?: return
        restaurantRef = database.getReference("restaurants")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadRestaurantData()

        val menuClickListener = View.OnClickListener {
            val intent = Intent(requireContext(), MenuActivity::class.java)
            startActivity(intent)
        }

        binding.menuLayout.setOnClickListener(menuClickListener)
        binding.menuIcon.setOnClickListener(menuClickListener)
        binding.menuText.setOnClickListener(menuClickListener)

        val feedbackClickListener = View.OnClickListener {
            val intent = Intent(requireContext(), ReviewActivity::class.java)
            startActivity(intent)
        }

        binding.feedbackLayout.setOnClickListener(feedbackClickListener)
        binding.feedbackIcon.setOnClickListener(feedbackClickListener)
        binding.feedbackText.setOnClickListener(feedbackClickListener)

        val informationClickListener = View.OnClickListener {
            val intent = Intent(requireContext(), ProfileActivity::class.java)
            startActivity(intent)
        }

        binding.informationLayout.setOnClickListener(informationClickListener)
        binding.informationIcon.setOnClickListener(informationClickListener)
        binding.informationText.setOnClickListener(informationClickListener)

        val promotionClickListener = View.OnClickListener {
            val intent = Intent(requireContext(), PromotionActivity::class.java)
            startActivity(intent)
        }

        binding.promotionsLayout.setOnClickListener(promotionClickListener)
        binding.promotionsIcon.setOnClickListener(promotionClickListener)
        binding.promotionsText.setOnClickListener(promotionClickListener)
    }

    private fun loadRestaurantData() {
        restaurantRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    Log.d("DashboardFragment", "Snapshot exists: ${snapshot.value}")
                    for (restaurantSnapshot in snapshot.children) {
                        val restaurant = restaurantSnapshot.getValue(Restaurant::class.java)
                        Log.d("DashboardFragment", "Restaurant snapshot: ${restaurantSnapshot.value}")
                        if (restaurant != null) {
                            binding.restaurantNameText.text = restaurant.name
                            Log.d("DashboardFragment", "Restaurant Name: ${restaurant.name}")
                            break
                        } else {
                            Log.e("DashboardFragment", "Restaurant data is null")
                        }
                    }
                } else {
                    Log.e("DashboardFragment", "Snapshot does not exist")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load restaurant data.", Toast.LENGTH_SHORT).show()
                Log.e("DashboardFragment", "DatabaseError: ${error.message}")
            }
        })
    }
}
