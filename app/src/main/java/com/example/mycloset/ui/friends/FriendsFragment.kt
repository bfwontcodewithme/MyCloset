package com.example.mycloset.ui.friends

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mycloset.R
import com.example.mycloset.data.model.FriendRequest
import com.example.mycloset.data.model.User
import com.example.mycloset.data.repository.FriendsRequestsRepository
import com.example.mycloset.data.repository.UsersRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class FriendsFragment : Fragment(R.layout.fragment_friends) {

    private val usersRepo = UsersRepository()
    private val reqRepo = FriendsRequestsRepository()

    private lateinit var progress: ProgressBar

    private lateinit var myFriendsAdapter: UsersAdapter
    private lateinit var resultsAdapter: UsersAdapter
    private lateinit var requestsAdapter: FriendRequestsAdapter

    private lateinit var tvMyFriendsEmpty: TextView
    private lateinit var tvRequestsEmpty: TextView
    private lateinit var tvResultsEmpty: TextView

    private var myFriends: MutableList<User> = mutableListOf()
    private var incoming: List<FriendRequest> = emptyList()

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
        val rvRequests = view.findViewById<RecyclerView>(R.id.rvRequests)

        // ✅ Empty labels (must exist in XML)
        tvMyFriendsEmpty = view.findViewById(R.id.tvMyFriendsEmpty)
        tvRequestsEmpty = view.findViewById(R.id.tvRequestsEmpty)
        tvResultsEmpty = view.findViewById(R.id.tvResultsEmpty)

        myFriendsAdapter = UsersAdapter(
            isFriend = { u -> myFriends.any { it.userUid == u.userUid } },
            onAdd = { /* no */ },
            onRemove = { u -> removeFriend(myUid, u) }
        )

        resultsAdapter = UsersAdapter(
            isFriend = { u -> myFriends.any { it.userUid == u.userUid } },
            onAdd = { u -> sendRequest(myUid, u) },
            onRemove = { u -> removeFriend(myUid, u) }
        )

        requestsAdapter = FriendRequestsAdapter(
            onAccept = { r -> acceptRequest(myUid, r) },
            onDecline = { r -> declineRequest(r) }
        )

        rvMyFriends.layoutManager = LinearLayoutManager(requireContext())
        rvMyFriends.adapter = myFriendsAdapter

        rvResults.layoutManager = LinearLayoutManager(requireContext())
        rvResults.adapter = resultsAdapter

        rvRequests.layoutManager = LinearLayoutManager(requireContext())
        rvRequests.adapter = requestsAdapter

        // initial state
        tvRequestsEmpty.visibility = View.GONE
        tvMyFriendsEmpty.visibility = View.GONE
        tvResultsEmpty.visibility = View.GONE

        loadAll(myUid)

        btnSearch.setOnClickListener {
            val q = etSearch.text.toString().trim()
            searchUsers(q)
        }
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun loadAll(myUid: String) {
        loadMyFriends(myUid)
        loadIncoming(myUid)
        // Results empty state is only relevant after a search
        tvResultsEmpty.visibility = View.GONE
    }

    private fun loadMyFriends(myUid: String) {
        lifecycleScope.launch {
            try {
                setLoading(true)
                val friends = usersRepo.getFriends(myUid)
                myFriends = friends.toMutableList()
                myFriendsAdapter.submitList(myFriends.toList())
                resultsAdapter.notifyDataSetChanged()

                tvMyFriendsEmpty.visibility = if (myFriends.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Friends load error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun loadIncoming(myUid: String) {
        lifecycleScope.launch {
            try {
                setLoading(true)
                incoming = reqRepo.getIncoming(myUid)
                requestsAdapter.submitList(incoming)

                tvRequestsEmpty.visibility = if (incoming.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Requests load error: ${e.message}", Toast.LENGTH_LONG).show()
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

                if (query.isBlank()) {
                    resultsAdapter.submitList(emptyList())
                    tvResultsEmpty.text = "Type a name or email to search"
                    tvResultsEmpty.visibility = View.VISIBLE
                    return@launch
                }

                val results = usersRepo.searchUsers(query)
                    .filter { it.userUid.isNotBlank() && it.userUid != myUid }

                resultsAdapter.submitList(results)

                tvResultsEmpty.text = "No results"
                tvResultsEmpty.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Search error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun sendRequest(myUid: String, u: User) {
        lifecycleScope.launch {
            try {
                setLoading(true)

                val me = usersRepo.getMyUserDoc(myUid)
                val myEmail = me?.userEmail ?: ""
                val senderDisplayName = me?.userName ?: me?.userEmail ?: "A user"
                reqRepo.sendRequest(
                    fromUid = myUid,
                    fromEmail = myEmail,
                    fromName = senderDisplayName,
                    toUid = u.userUid,
                    toEmail = u.userEmail
                )

                Toast.makeText(requireContext(), "Request sent ✅", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Send request error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun acceptRequest(myUid: String, r: FriendRequest) {
        lifecycleScope.launch {
            try {
                setLoading(true)

                reqRepo.accept(r.requestId)
                usersRepo.addFriend(myUid, r.fromUid)
                usersRepo.addFriend(r.fromUid, myUid)

                Toast.makeText(requireContext(), "Friend added ✅", Toast.LENGTH_SHORT).show()
                loadAll(myUid)

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Accept error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun declineRequest(r: FriendRequest) {
        val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        lifecycleScope.launch {
            try {
                setLoading(true)
                reqRepo.decline(r.requestId)
                Toast.makeText(requireContext(), "Declined", Toast.LENGTH_SHORT).show()
                loadIncoming(myUid)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Decline error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun removeFriend(myUid: String, u: User) {
        lifecycleScope.launch {
            try {
                setLoading(true)
                usersRepo.removeFriend(myUid, u.userUid)
                usersRepo.removeFriend(u.userUid, myUid)

                myFriends.removeAll { it.userUid == u.userUid }
                myFriendsAdapter.submitList(myFriends.toList())
                resultsAdapter.notifyDataSetChanged()

                tvMyFriendsEmpty.visibility = if (myFriends.isEmpty()) View.VISIBLE else View.GONE

                Toast.makeText(requireContext(), "Removed", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Remove friend error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }
}
