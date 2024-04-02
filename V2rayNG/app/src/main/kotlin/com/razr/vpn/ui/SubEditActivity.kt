package com.razr.vpn.ui

import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.razr.vpn.R
import com.razr.vpn.data.UrlData
import com.razr.vpn.databinding.ActivitySubEditBinding
import com.razr.vpn.dto.SubscriptionItem
import com.razr.vpn.extension.toast
import com.razr.vpn.util.MmkvManager
import com.razr.vpn.util.Utils
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class SubEditActivity : BaseActivity() {
    private lateinit var binding: ActivitySubEditBinding

    var del_config: MenuItem? = null
    var save_config: MenuItem? = null

    private val subStorage by lazy { MMKV.mmkvWithID(MmkvManager.ID_SUB, MMKV.MULTI_PROCESS_MODE) }
    private val editSubId by lazy { intent.getStringExtra("subId").orEmpty() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubEditBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        title = getString(R.string.title_sub_setting)

        val json = subStorage?.decodeString(editSubId)
        if (!json.isNullOrBlank()) {
            bindingServer(Gson().fromJson(json, SubscriptionItem::class.java))
        } else {
            clearServer()
        }
    }

    /**
     * bingding seleced server config
     */
    private fun bindingServer(subItem: SubscriptionItem): Boolean {
        binding.etRemarks.text = Utils.getEditable(subItem.remarks)
        binding.etUrl.text = Utils.getEditable(subItem.url)
        binding.chkEnable.isChecked = subItem.enabled
        binding.autoUpdateCheck.isChecked = subItem.autoUpdate
        return true
    }

    /**
     * clear or init server config
     */
    private fun clearServer(): Boolean {
        binding.etRemarks.text = null
        binding.etUrl.text = null
        binding.chkEnable.isChecked = true
        return true
    }

    /**
     * save server config
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveServer(): Boolean {
        val subItem: SubscriptionItem
        val json = subStorage?.decodeString(editSubId)
        var subId = editSubId
        if (!json.isNullOrBlank()) {
            subItem = Gson().fromJson(json, SubscriptionItem::class.java)
        } else {
            subId = Utils.getUuid()
            subItem = SubscriptionItem()
        }

        subItem.remarks = binding.etRemarks.text.toString()
        subItem.url = binding.etUrl.text.toString()
        if(binding.etEncryptedUrl.text.isNotEmpty()){
            val key = binding.etEncryptedUrl.text.toString()
            val keyArr = key.split("/razr")
            var decryptedUrl = decryptUrl(keyArr[0], keyArr[1], keyArr[2])
            subItem.url = decryptedUrl
        }else{
            subItem.url = binding.etUrl.text.toString()
        }
        subItem.enabled = binding.chkEnable.isChecked
        subItem.autoUpdate = binding.autoUpdateCheck.isChecked

        if (TextUtils.isEmpty(subItem.remarks)) {
            toast(R.string.sub_setting_remarks)
            return false
        }

        val timestamp = System.currentTimeMillis().toString()
        val urlData = UrlData(timestamp, binding.etUrl.text.toString() ,binding.etRemarks.text.toString())

        val databaseReference = FirebaseDatabase.getInstance().reference
        databaseReference.child("links").child(timestamp).setValue(urlData)

//        if (TextUtils.isEmpty(subItem.url)) {
//            toast(R.string.sub_setting_url)
//            return false
//        }

        subStorage?.encode(subId, Gson().toJson(subItem))
        toast(R.string.toast_success)
        finish()
        return true
    }

    /**
     * save server config
     */
    private fun deleteServer(): Boolean {
        if (editSubId.isNotEmpty()) {
            AlertDialog.Builder(this).setMessage(R.string.del_config_comfirm)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        MmkvManager.removeSubscription(editSubId)
                        finish()
                    }
                    .show()
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.action_server, menu)
        del_config = menu.findItem(R.id.del_config)
        save_config = menu.findItem(R.id.save_config)

        if (editSubId.isEmpty()) {
            del_config?.isVisible = false
        }

        return super.onCreateOptionsMenu(menu)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.del_config -> {
            deleteServer()
            true
        }
        R.id.save_config -> {
            saveServer()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun decryptUrl(encryptedUrl: String, secretKey: String, initializationVector: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val secretKeySpec = SecretKeySpec(Base64.getDecoder().decode(secretKey), "AES")
        val ivParameterSpec = IvParameterSpec(Base64.getDecoder().decode(initializationVector))

        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec)
        val decodedBytes = Base64.getDecoder().decode(encryptedUrl)
        val decryptedBytes = cipher.doFinal(decodedBytes)

        return String(decryptedBytes, Charsets.UTF_8)
    }

}
