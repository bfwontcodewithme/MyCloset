package com.example.mycloset.ui.item

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.mycloset.R
import com.example.mycloset.data.model.Item
import com.example.mycloset.data.model.Item
import com.example.mycloset.data.repository.ItemsRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.io.File

class AddItemFragment : Fragment(R.layout.fragment_add_item) {

    private val repo = ItemsRepository()

    private var selectedImageUri: Uri? = null
    private var cameraImageUri: Uri? = null

    // Gallery
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedImageUri = uri
                view?.findViewById<ImageView>(R.id.imgItem)?.setImageURI(uri)
            }
        }

    // Camera
    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                selectedImageUri = cameraImageUri
                view?.findViewById<ImageView>(R.id.imgItem)?.setImageURI(cameraImageUri)
            } else {
                Toast.makeText(requireContext(), "taking picture canceled", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_global_loginFragment)
            return
        }

        val imgItem = view.findViewById<ImageView>(R.id.imgItem)

        val btnCamera = view.findViewById<Button>(R.id.btnTakePhoto)      //   camera button
        val btnGallery = view.findViewById<Button>(R.id.btnPickImage)     // gallary
        val btnSave = view.findViewById<Button>(R.id.btnSave)
        val progress = view.findViewById<ProgressBar>(R.id.progress)

        val etName = view.findViewById<EditText>(R.id.etName)
        val etType = view.findViewById<EditText>(R.id.etType)
        val etColor = view.findViewById<EditText>(R.id.etColor)
        val etSeason = view.findViewById<EditText>(R.id.etSeason)
        val etTags = view.findViewById<EditText>(R.id.etTags)

        // camera
        btnCamera.setOnClickListener {
            cameraImageUri = createImageUri()
            takePictureLauncher.launch(cameraImageUri)
        }

        // gallary
        btnGallery.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // save
        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val type = etType.text.toString().trim()
            val color = etColor.text.toString().trim()
            val season = etSeason.text.toString().trim()
            val tags = etTags.text.toString()
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            if (name.isEmpty() || type.isEmpty()) {
                Toast.makeText(requireContext(), "Name ו-Type mandatory", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    progress.visibility = View.VISIBLE
                    btnSave.isEnabled = false
                    btnCamera.isEnabled = false
                    btnGallery.isEnabled = false

                    //       if there is a picture -> storage, otherwise ""
                    val imageUrl = selectedImageUri?.let { uri ->
                        repo.uploadImage(userId, uri)   // Storage
                    } ?: ""

                    val item = Item(
                        ownerUid = userId,
                        closetId = "default",
                        name = name,
                        type = type,
                        color = color,
                        season = season,
                        tags = tags,
                        imageUrl = imageUrl
                    )

                    repo.addItem(userId, item)         // Firestore

                    Toast.makeText(requireContext(), "Item saved successfully ", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack() // return to item list

                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "error: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    progress.visibility = View.GONE
                    btnSave.isEnabled = true
                    btnCamera.isEnabled = true
                    btnGallery.isEnabled = true
                }
            }
        }
    }

    private fun createImageUri(): Uri {
        val imagesDir = File(requireContext().cacheDir, "images")
        imagesDir.mkdirs()
        val file = File(imagesDir, "camera_${System.currentTimeMillis()}.jpg")

        return FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            file
        )
    }
}