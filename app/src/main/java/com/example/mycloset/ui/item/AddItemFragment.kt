package com.example.mycloset.ui.item

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.mycloset.R
import com.example.mycloset.data.model.Item
import com.example.mycloset.data.repository.ItemsRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.io.File

class AddItemFragment : Fragment(R.layout.fragment_add_item) {

    private val repo = ItemsRepository()

    private var selectedImageUri: Uri? = null
    private var cameraImageUri: Uri? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedImageUri = uri
                view?.findViewById<ImageView>(R.id.imgItem)?.setImageURI(uri)
            }
        }

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) openCamera()
            else Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                selectedImageUri = cameraImageUri
                view?.findViewById<ImageView>(R.id.imgItem)?.setImageURI(cameraImageUri)
            } else {
                Toast.makeText(requireContext(), "Taking picture canceled", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_global_login)
            return
        }

        // ✅ מגיע מה-ItemListFragment
        val closetIdFromArgs = arguments?.getString("closetId").orEmpty()

        val btnCamera = view.findViewById<Button>(R.id.btnTakePhoto)
        val btnGallery = view.findViewById<Button>(R.id.btnPickImage)
        val btnSave = view.findViewById<Button>(R.id.btnSave)
        val progress = view.findViewById<ProgressBar>(R.id.progress)

        val etName = view.findViewById<EditText>(R.id.etName)
        val etType = view.findViewById<EditText>(R.id.etType)
        val etColor = view.findViewById<EditText>(R.id.etColor)
        val etSeason = view.findViewById<EditText>(R.id.etSeason)
        val etTags = view.findViewById<EditText>(R.id.etTags)

        btnCamera.setOnClickListener {
            val hasCamera = requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
            if (!hasCamera) {
                Toast.makeText(requireContext(), "No camera on this device", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val granted = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

            if (granted) openCamera()
            else requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        btnGallery.setOnClickListener { pickImageLauncher.launch("image/*") }

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

            // אם נכנסת ל-AddItem בלי ארון - נשמור ל-default (עדיף מאשר לקרוס)
            val closetId = if (closetIdFromArgs.isBlank()) "default" else closetIdFromArgs

            lifecycleScope.launch {
                try {
                    setLoading(true, progress, btnSave, btnCamera, btnGallery)

                    val imageUrl = selectedImageUri?.let { uri ->
                        repo.uploadImage(userId, uri)
                    } ?: ""

                    val item = Item(
                        ownerUid = userId,
                        closetId = closetId,
                        name = name,
                        type = type,
                        color = color,
                        season = season,
                        tags = tags,
                        imageUrl = imageUrl
                    )

                    repo.addItem(userId, item)

                    Toast.makeText(requireContext(), "Item saved successfully", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()

                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    setLoading(false, progress, btnSave, btnCamera, btnGallery)
                }
            }
        }
    }

    private fun openCamera() {
        cameraImageUri = createImageUri()
        takePictureLauncher.launch(cameraImageUri)
    }

    private fun setLoading(
        loading: Boolean,
        progress: ProgressBar,
        btnSave: Button,
        btnCamera: Button,
        btnGallery: Button
    ) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        btnSave.isEnabled = !loading
        btnCamera.isEnabled = !loading
        btnGallery.isEnabled = !loading
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
