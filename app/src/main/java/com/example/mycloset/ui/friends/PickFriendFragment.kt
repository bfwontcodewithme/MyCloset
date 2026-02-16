package com.example.mycloset.ui.friends

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mycloset.R
import com.example.mycloset.data.model.User
import com.example.mycloset.data.repository.UsersRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class PickFriendFragment : Fragment(R.layout.fragment_pick_friend) {

    private val repo = UsersRepository()

    private lateinit var progress: ProgressBar
    private lateinit var adapter: PickFriendAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val myUid = FirebaseAuth.getInstance().currentUser?.uid
        if (myUid == null) {
            Toast.makeText(requireContext(), "Please login", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_global_login)
            return
        }

        progress = view.findViewById(R.id.progressPickFriend)
        val rv = view.findViewById<RecyclerView>(R.id.rvPickFriend)

        adapter = PickFriendAdapter { u ->
            // ✅ מחזירים ל-ShareAccessFragment דרך savedStateHandle
            findNavController().previousBackStackEntry?.savedStateHandle?.set("pickedFriendUid", u.userUid)
            findNavController().previousBackStackEntry?.savedStateHandle?.set(
                "pickedFriendLabel",
                u.userEmail.ifBlank { u.userName.ifBlank { u.userUid } }
            )
            findNavController().navigateUp()
        }

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        loadFriends(myUid)
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun loadFriends(myUid: String) {
        lifecycleScope.launch {
            try {
                setLoading(true)
                val friends = repo.getFriends(myUid)
                adapter.submitList(friends)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Load friends error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }
}
