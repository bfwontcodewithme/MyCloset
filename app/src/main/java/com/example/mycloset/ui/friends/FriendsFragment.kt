package com.example.mycloset.ui.friends

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mycloset.R
import com.example.mycloset.data.model.User
import com.example.mycloset.data.repository.UsersRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class FriendsFragment : Fragment(R.layout.fragment_friends) {

    private val repo = UsersRepository()

    private lateinit var progress: ProgressBar
    private lateinit var myFriendsAdapter: UsersAdapter
    private lateinit var resultsAdapter: UsersAdapter

    private var myFriends: MutableList<User> = mutableListOf()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val myUid = FirebaseAuth.getInstance().currentUser?.uid
        if (myUid == null) {
            Toast.makeText(requireContext(), "Please login", Toast.LENGTH_SHORT).show()
            return
        }

        val etSearch = view.findViewById<EditText>(R.id.etSearch)
        val btnSearch = view.findViewById<Button>(R.id.btnSearch)
        progress = view.findViewById(R.id.progressFriends)

        val rvMyFriends = view.findViewById<RecyclerView>(R.id.rvMyFriends)
        val rvResults = view.findViewById<RecyclerView>(R.id.rvResults)

        myFriendsAdapter = UsersAdapter(
            isFriend = { u -> myFriends.any { it.userUid == u.userUid } },
            onAdd = { /* לא קורה ברשימה הזאת */ },
            onRemove = { u -> removeFriend(myUid, u) }
        )

        resultsAdapter = UsersAdapter(
            isFriend = { u -> myFriends.any { it.userUid == u.userUid } },
            onAdd = { u -> addFriend(myUid, u) },
            onRemove = { u -> removeFriend(myUid, u) }
        )

        rvMyFriends.layoutManager = LinearLayoutManager(requireContext())
        rvMyFriends.adapter = myFriendsAdapter

        rvResults.layoutManager = LinearLayoutManager(requireContext())
        rvResults.adapter = resultsAdapter

        loadMyFriends(myUid)

        btnSearch.setOnClickListener {
            val q = etSearch.text.toString()
            searchUsers(q)
        }
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun loadMyFriends(myUid: String) {
        lifecycleScope.launch {
            try {
                setLoading(true)
                val friends = repo.getFriends(myUid)
                myFriends = friends.toMutableList()
                myFriendsAdapter.submitList(myFriends)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Friends load error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun searchUsers(query: String) {
        val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        lifecycleScope.launch {
            try {
                setLoading(true)
                val results = repo.searchUsers(query)
                    .filter { it.userUid.isNotBlank() && it.userUid != myUid } // לא להראות את עצמי

                resultsAdapter.submitList(results)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Search error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun addFriend(myUid: String, u: User) {
        lifecycleScope.launch {
            try {
                setLoading(true)
                repo.addFriend(myUid, u.userUid)

                if (myFriends.none { it.userUid == u.userUid }) myFriends.add(u)
                myFriendsAdapter.submitList(myFriends.toList())
                resultsAdapter.notifyDataSetChanged()

                Toast.makeText(requireContext(), "Added ✅", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Add friend error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun removeFriend(myUid: String, u: User) {
        lifecycleScope.launch {
            try {
                setLoading(true)
                repo.removeFriend(myUid, u.userUid)

                myFriends.removeAll { it.userUid == u.userUid }
                myFriendsAdapter.submitList(myFriends.toList())
                resultsAdapter.notifyDataSetChanged()

                Toast.makeText(requireContext(), "Removed", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Remove friend error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }
}
