package com.example.cm2.Activity

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.util.Log
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.cm2.Adapter.Device_Adapter
import com.example.cm2.R
import android.view.View
import android.widget.ArrayAdapter
import models.BTDeviceItem
import android.widget.Button
import android.widget.ListView
import android.bluetooth.le.ScanResult
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager


class MainActivity : AppCompatActivity() {


    private lateinit var boutton_recherche: Button
    private lateinit var boutton_stop_recherche: Button
    private lateinit var tvDebug: TextView
    private lateinit var listView: ListView
    private lateinit var btActivation: Button
    private val scanPeriod: Long = 10000
    private var stateBTEnabled: Boolean = false
    private var scanningBLE: Boolean = false
    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner


    private lateinit var ArrayAdapter: ArrayAdapter<BTDeviceItem>
    private var deviceslist: ArrayList<BTDeviceItem> = ArrayList()
    private var mbr: MonBroadcastReceiver? = null

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            if (hasBluetoothConnectPermission()) {
                handleScanResult(result)
            } else {
                requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }
    }

    // scan Bt LOW ENERGY
    private fun handleScanResult(result: ScanResult) {
        try {
            // Vérification explicite de la permission BLUETOOTH_CONNECT
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                val bleDevice: BluetoothDevice = result.device
                val newDevice = BTDeviceItem(bleDevice, false, "BLE")

                // Recherche de l'appareil existant dans la liste
                val existingDevice = deviceslist.find { it.appareil.address == bleDevice.address }
                if (existingDevice == null) {
                    tvDebug.append("Appareil BLE Detecté\n")
                    deviceslist.add(newDevice)
                } else {
                    // Mise à jour du type si l'appareil existe déjà dans BT2
                    existingDevice.BTtype = "BT2 | BLE"
                }

                // Notification à l'adaptateur pour rafraîchir la liste
                ArrayAdapter.notifyDataSetChanged()
            } else {
                Log.w("BLE_SCAN", "Permission BLUETOOTH_CONNECT non accordée.")
                // Demander la permission si elle n'est pas accordée
                requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            }
        } catch (e: SecurityException) {
            Log.e("BLE_SCAN", "Permission manquante pour accéder à l'appareil : ${e.message}")
        } catch (e: Exception) {
            Log.e("BLE_SCAN", "Erreur inattendue lors du traitement du résultat BLE : ${e.message}")
        }
    }

    private fun scanLeDevice() {
        if (!scanningBLE) {
            Handler(Looper.getMainLooper()).postDelayed({
                scanningBLE = false
                handleBluetoothScan(false)
            }, scanPeriod)
            scanningBLE = true
            handleBluetoothScan(true)
        } else {
            handleBluetoothScan(false)
        }
    }

    private fun handleBluetoothScan(isScanning: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            when {
                ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.BLUETOOTH_SCAN
                ) == PackageManager.PERMISSION_GRANTED -> {
                    if (isScanning) {
                        bluetoothLeScanner.startScan(leScanCallback)
                    } else {
                        bluetoothLeScanner.stopScan(leScanCallback)
                        boutton_recherche.isEnabled = true
                    }
                }
                shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_SCAN) -> {
                    showDialog(
                        getString(R.string.rationaleBTConnectTitle),
                        getString(R.string.rationaleBTConnectDesc),
                        Manifest.permission.BLUETOOTH_SCAN,
                        "REQUEST_PERMISSION"
                    )
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_SCAN)
                }
            }
        } else {
            if (isScanning) {
                bluetoothLeScanner.startScan(leScanCallback)
            } else {
                bluetoothLeScanner.stopScan(leScanCallback)
                boutton_recherche.isEnabled = true
            }
        }
    }



    private var activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        )
        {
                result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                if (stateBTEnabled) {
                    btActivation.text = resources.getString(R.string.btActiverBT)
                    stateBTEnabled = false
                } else {
                    btActivation.text = resources.getString(R.string.btDesactiverBT)
                    stateBTEnabled = true
                }
            }
            else if (result.resultCode == RESULT_CANCELED) {
                Afficher_message(message ="Vous devez activer le Bluetooth pour faire une .")
            }
            else {
                Afficher_message(message ="Vous devez activer le Bluetooth.")
            }
        }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your app.
                Afficher_message(message ="Permission accordée")
            } else {
                // Permission is denied action
                Afficher_message(message ="Permission refusée")
            }
        }

    private fun showDialog(
        title: String, message: String, permission: String, requestCode: String
    ) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(requestCode+" "+message+" "+permission)
            .setPositiveButton("Ok", { dialog, which -> })
        builder.create().show()
    }
    // Vérification de la permission Bluetooth
    private fun hasBluetoothConnectPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }


    internal inner class MonBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

            when (action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> logDebug("Changement de statut BT")
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    logDebug("Recherche commencée")
                    handlePairedDevices()
                }
                BluetoothDevice.ACTION_FOUND -> {
                    logDebug("Appareil BT2 détecté")
                    handleDeviceFound(intent, device)
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    logDebug("Recherche BT2 terminée")
                    scanLeDevice()
                    updateButtonVisibility()
                }
            }

            device?.let { handleBondState(it, intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)) }
        }

        private fun handlePairedDevices() {
            if (hasBluetoothConnectPermission()) {
                try {
                    mBluetoothAdapter.bondedDevices.forEach { pairedDevice ->
                        if (!deviceslist.any { it.appareil.address == pairedDevice.address }) {
                            deviceslist.add(BTDeviceItem(pairedDevice, true, "Paired"))
                            ArrayAdapter.notifyDataSetChanged()
                        }
                    }
                } catch (e: SecurityException) {
                    Log.e("BLUETOOTH", e.message.toString())
                }
            } else {
                requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }

        private fun handleDeviceFound(intent: Intent, device: BluetoothDevice?) {
            try {
                // Vérification explicite de la permission
                if (ActivityCompat.checkSelfPermission(baseContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    device?.let {
                        val deviceName = it.name ?: "Appareil inconnu"
                        val deviceAddress = it.address

                        if (!deviceslist.any { it.appareil.address == deviceAddress }) {
                            deviceslist.add(BTDeviceItem(it, false, "BT2"))
                            ArrayAdapter.notifyDataSetChanged()
                            Log.d("BT_DEVICE", "$deviceName - $deviceAddress")
                        }

                        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, BluetoothAdapter.STATE_DISCONNECTED)
                        updateDeviceStatus(it, state == BluetoothAdapter.STATE_CONNECTED)
                    }
                } else {
                    // Si la permission n'est pas accordée, demander la permission
                    Log.w("BT_DEVICE", "Permission BLUETOOTH_CONNECT non accordée.")
                    requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                }
            } catch (e: SecurityException) {
                Log.e("BT_DEVICE", "Erreur de sécurité lors du traitement de l'appareil détecté : ${e.message}")
            } catch (e: Exception) {
                Log.e("BT_DEVICE", "Erreur inattendue : ${e.message}")
            }
        }

        private fun handleBondState(device: BluetoothDevice, bondState: Int) {
            when (bondState) {
                BluetoothDevice.BOND_BONDED -> {
                    deviceslist.find { it.appareil.address == device.address }?.apply {
                        remoteConfirmed = true
                        if (localConfirmed && remoteConfirmed) {
                            isconnect = true
                            paired = true
                            ArrayAdapter.notifyDataSetChanged()
                        }
                    }
                }
            }
        }

        private fun hasBluetoothConnectPermission(): Boolean {
            return ActivityCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        }

        private fun logDebug(message: String) {
            tvDebug.append("$message\n")
        }

        private fun updateButtonVisibility() {
            boutton_stop_recherche.visibility = View.GONE
            boutton_recherche.visibility = View.VISIBLE
        }
    }


    private fun updateDeviceStatus(device: BluetoothDevice, isConnected: Boolean) {
        val item = deviceslist.find { it.appareil.address == device.address }
        if (item != null) {
            item.isconnect = isConnected
            ArrayAdapter.notifyDataSetChanged()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btActivation = findViewById(R.id.btActivationBT)
        boutton_recherche = findViewById(R.id.btRechercherBT)
        listView = findViewById(R.id.listView)
        ArrayAdapter = Device_Adapter(this, R.layout.device_item, deviceslist)
        listView.adapter = ArrayAdapter

        boutton_stop_recherche = findViewById(R.id.stop_recherche)
        tvDebug = findViewById(R.id.tvDebug)

        val mBluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        mBluetoothAdapter = mBluetoothManager.adapter

        mbr = MonBroadcastReceiver()
        val filter = IntentFilter()
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        registerReceiver(mbr, filter)

        checkBluetoothState()
        boutton_stop_recherche.visibility = View.GONE

        boutton_recherche.setOnClickListener {
            if (commencerRecherche()) {
                clearDebugText()
                buttons_update(in_search = true)
            }
        }

        btActivation.setOnClickListener {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                when {
                    ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        // If Permission is Granted do THIS :
                        if (stateBTEnabled) {
                            if (mBluetoothAdapter.isDiscovering || scanningBLE ) {
                                mBluetoothAdapter.cancelDiscovery()
                                bluetoothLeScanner?.stopScan(leScanCallback)
                                scanningBLE = false
                            }
                            activityResultLauncher.launch(
                                Intent("android.bluetooth.adapter.action.REQUEST_DISABLE")
                            )
                            deleteAllDevices()
                        } else {
                            activityResultLauncher.launch(
                                Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                            )
                        }
                    }
                    (shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_CONNECT)) -> {
                        // If Permission is Denied do THIS :
                        showDialog(
                            getString(R.string.rationaleBTConnectTitle),
                            getString(R.string.rationaleBTConnectDesc),
                            Manifest.permission.BLUETOOTH_CONNECT,
                            "REQUEST_PERMISSION"
                        )
                    }
                    else -> {
                        // If Permission is Denied do THIS :
                        requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    }

                }

            }
            else {
                if (stateBTEnabled) {
                    activityResultLauncher.launch(
                        Intent("android.bluetooth.adapter.action.REQUEST_DISABLE")
                    )
                } else {
                    activityResultLauncher.launch(
                        Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    )
                }
            }
        }
        boutton_stop_recherche.setOnClickListener {
            if (mBluetoothAdapter.isDiscovering) {
                mBluetoothAdapter.cancelDiscovery()
                Afficher_message(message ="Recherche Bluetooth arrêtée")
            }

            if (scanningBLE) {
                Afficher_message(message ="Recherche arrêtée")
                bluetoothLeScanner.stopScan(leScanCallback)
                scanningBLE = false
            }
            buttons_update(in_search = false)
        }
    }

    private fun commencerRecherche(): Boolean {
        if (mBluetoothAdapter == null) {

            Afficher_message(message ="Pas de Bluetooth")
            return false
        }
        return if (!mBluetoothAdapter.isEnabled) { // Bluetooth désactivé
            Afficher_message(message = "Vous devez activer votre Bluetooth pour effectuer une recherche")
            false
        } else {

            deviceslist.clear()
            ArrayAdapter.clear()
            bluetoothLeScanner = mBluetoothAdapter.bluetoothLeScanner

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                when {
                    ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.BLUETOOTH_SCAN
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        // You can use the API that requires the permission.
                        val success:Boolean = mBluetoothAdapter.startDiscovery()
                        Afficher_message(message ="Discovery Started")
                        return success
                    }

                    (shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_SCAN)) -> {
                        // If Permission is Denied do THIS :
                        showDialog(
                            getString(R.string.rationaleBTScanTitle),
                            getString(R.string.rationaleBTScanDesc),
                            Manifest.permission.BLUETOOTH_SCAN,
                            "REQUEST_PERMISSION_BTSCAN"
                        )
                        return false
                    }

                    else -> {
                        // You can directly ask for the permission.
                        requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_SCAN)
                        return false
                    }
                }
            }
            else {
                val success:Boolean = mBluetoothAdapter.startDiscovery()
                return success
            }
        }
    }

    // Vérification de l'état du Bluetooth
    private fun checkBluetoothState() {
        if (mBluetoothAdapter == null) {
            btActivation.isEnabled = false
        } else if (mBluetoothAdapter.isEnabled) {
            btActivation.text = resources.getString(R.string.btDesactiverBT)
            stateBTEnabled = true
        } else {
            btActivation.text = resources.getString(R.string.btActiverBT)
            stateBTEnabled = false
        }
    }
    private fun deleteAllDevices() {
        deviceslist.clear()
        ArrayAdapter.notifyDataSetChanged()
    }
    // Vider le DebugText
    private fun clearDebugText() {
        tvDebug.text = ""
    }

    private fun Afficher_message(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun buttons_update(in_search: Boolean) {
        boutton_stop_recherche.isEnabled = in_search
        boutton_recherche.isEnabled = !in_search
        if (in_search) {
            boutton_stop_recherche.visibility = View.VISIBLE
            boutton_recherche.visibility = View.GONE
        } else {
            boutton_recherche.visibility = View.VISIBLE
            boutton_stop_recherche.visibility = View.GONE
        }
    }

}