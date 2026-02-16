package com.example.mycloset.ui.support

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.mycloset.R
import com.google.android.material.card.MaterialCardView

class SupportFragment : Fragment(R.layout.fragment_support) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cardEmail = view.findViewById<MaterialCardView>(R.id.cardSupportEmail)
        val cardWhatsApp = view.findViewById<MaterialCardView>(R.id.cardSupportWhatsApp)
        val cardFacebook = view.findViewById<MaterialCardView>(R.id.cardSupportFacebook)

        cardEmail.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("support@mycloset.app"))
                putExtra(Intent.EXTRA_SUBJECT, "MyCloset Support")
                putExtra(Intent.EXTRA_TEXT, "Hi,\n\nI need help with...\n\nThanks!")
            }
            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "No email app found", Toast.LENGTH_SHORT).show()
            }
        }

        cardWhatsApp.setOnClickListener {
            // אפשר לשנות למספר/קישור שלך
            val url = "https://wa.me/972500000000?text=" + Uri.encode("Hi MyCloset Support, I need help with...")
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "WhatsApp not found", Toast.LENGTH_SHORT).show()
            }
        }

        cardFacebook.setOnClickListener {
            // אפשר לשנות לקישור שלך
            val url = "https://www.facebook.com"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "No browser found", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
