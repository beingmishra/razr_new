package com.razr.vpn.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import com.razr.vpn.R
import android.os.Bundle
import com.razr.vpn.databinding.ActivitySubSettingBinding
import com.razr.vpn.dto.SubscriptionItem
import com.razr.vpn.util.MmkvManager

class SubSettingActivity : BaseActivity(), OnItemClickListener {
    private lateinit var binding: ActivitySubSettingBinding

    var subscriptions:List<Pair<String, SubscriptionItem>> = listOf()
    private val adapter by lazy { SubSettingRecyclerAdapter(this, this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubSettingBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        title = getString(R.string.title_sub_setting)

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onItemClick(subId: String) {
        AlertDialog.Builder(this).setMessage(R.string.del_config_comfirm)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                MmkvManager.removeSubscription(subId)
                subscriptions = MmkvManager.decodeSubscriptions()
                adapter.notifyDataSetChanged()
            }
            .show()

    }

    override fun onResume() {
        super.onResume()
        subscriptions = MmkvManager.decodeSubscriptions()
        adapter.notifyDataSetChanged()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.action_sub_setting, menu)
        menu.findItem(R.id.del_config)?.isVisible = false
        menu.findItem(R.id.save_config)?.isVisible = false

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.add_config -> {
            startActivity(Intent(this, SubEditActivity::class.java))
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}


interface OnItemClickListener {
    fun onItemClick(subId: String)
}
