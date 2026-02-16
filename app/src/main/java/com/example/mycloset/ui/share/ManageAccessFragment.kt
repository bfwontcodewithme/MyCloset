package com.example.mycloset.ui.share

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mycloset.R
import com.example.mycloset.data.model.AccessGrant
import com.example.mycloset.data.repository.GrantsRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ManageAccessFragment : Fragment(R.layout.fragment_manage_access) {

    private val repo = GrantsRepository()
    private lateinit var adapter: AccessListAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rvAccessList)
        val progress = view.findViewById<ProgressBar>(R.id.progressAccess)

        val ownerUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val resourceType = arguments?.getString("resourceType").orEmpty()
        val resourceId = arguments?.getString("resourceId").orEmpty()

        adapter = AccessListAdapter { grant ->
            lifecycleScope.launch {
                try {
                    progress.visibility = View.VISIBLE
                    repo.deleteGrant(
                        ownerUid,
                        grant.granteeUid,
                        grant.resourceType,
                        grant.resourceId
                    )
                    loadData(ownerUid, resourceType, resourceId)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
                } finally {
                    progress.visibility = View.GONE
                }
            }
        }

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        loadData(ownerUid, resourceType, resourceId)
    }

    private fun loadData(ownerUid: String, resourceType: String, resourceId: String) {
        lifecycleScope.launch {
            val grants = repo.getSharedOutfitGrantsForMe(ownerUid)
                .filter { it.resourceId == resourceId }

            adapter.submitList(grants)
        }
    }
}
