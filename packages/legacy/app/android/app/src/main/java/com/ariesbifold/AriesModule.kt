package com.ariesbifold

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableNativeMap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.hyperledger.ariesframework.agent.Agent
import org.hyperledger.ariesframework.agent.AgentConfig
import org.hyperledger.ariesframework.agent.MediatorPickupStrategy
import org.hyperledger.ariesframework.credentials.models.AutoAcceptCredential
import org.hyperledger.ariesframework.proofs.models.AutoAcceptProof
import org.hyperledger.ariesframework.wallet.PREFERENCE_NAME
import org.hyperledger.ariesframework.wallet.Wallet
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class AriesModule @Inject constructor(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {
    @Inject
    @ApplicationContext
    lateinit var applicationContext: Context

    lateinit var agent: Agent
    var walletOpened: Boolean = false
    private val pref by lazy { applicationContext.getSharedPreferences(PREFERENCE_NAME, 0) }

    private fun copyResourceFile(resource: String, inputStream: InputStream) {
        val file = File(applicationContext.filesDir.absolutePath, resource)
        if (!file.exists()) {
            file.outputStream().use { inputStream.copyTo(it) }
        }
    }

    override fun getName() = "Aries"

    @ReactMethod
    @SuppressLint("ApplySharedPref")
    fun config(config: ReadableMap, promise: Promise) {
        val genesisTransactions = config.getString("genesisTransactions")!!
        val label = config.getString("label")!!
        val mediatorUrl = config.getString("mediatorUrl")!!

        GlobalScope.launch(Dispatchers.IO) {
            val genesisPath = "genesis.txn"
            val id = "walletId"
            var key = pref.getString("walletKey", null)

            if (key == null) {
                key = Wallet.generateKey()
                pref.edit().putString("walletKey", key).apply()
                val inputStream = ByteArrayInputStream(genesisTransactions.toByteArray(Charsets.UTF_8))
                copyResourceFile(genesisPath, inputStream)
            }

            if (!walletOpened) {
                openWallet(id, key, genesisPath, mediatorUrl, label)
            }
            promise.resolve(WritableNativeMap().apply {
                putString("id", id)
                putString("key", key)
            })
        }
    }

    suspend fun openWallet(id: String, key: String, genesisPath: String, invitationUrl: String, label: String) {
        val config = AgentConfig(
            walletId = id,
            walletKey = key,
            genesisPath = File(applicationContext.filesDir.absolutePath, genesisPath).absolutePath,
            mediatorConnectionsInvite = invitationUrl,
            mediatorPickupStrategy = MediatorPickupStrategy.Implicit,
            label = label,
            autoAcceptCredential = AutoAcceptCredential.Always,
            autoAcceptProof = AutoAcceptProof.Always,
        )
        agent = Agent(applicationContext, config)
        agent.initialize()

        walletOpened = true
        Log.i(TAG, "Agent initialized")
    }

    companion object {
        var TAG = "Willkomo|AriesModule"
    }
}
