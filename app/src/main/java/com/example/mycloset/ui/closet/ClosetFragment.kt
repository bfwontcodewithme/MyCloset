package com.example.mycloset.ui.closet

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mycloset.R

class ClosetFragment : Fragment(R.layout.fragment_closet) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.btnAddGarment).setOnClickListener {
            // יעד שקיים אצלך ב-nav_graph.xml
            findNavController().navigate(R.id.addItemFragment)
        }

        view.findViewById<Button>(R.id.btnCreateOutfit).setOnClickListener {
            // יעד שקיים אצלך ב-nav_graph.xml
            findNavController().navigate(R.id.createOutfitFragment)
        }
    }
}