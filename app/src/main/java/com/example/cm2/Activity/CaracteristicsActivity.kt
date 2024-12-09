package com.example.cm2.Activity

import android.Manifest
import android.bluetooth.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cm2.Adapter.Service_Adapter
import com.example.cm2.R

class CaracteristicsActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_BLUETOOTH_PERMISSION = 100
    }
    private lateinit var btn_disconnect: Button
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var device: BluetoothDevice
    private var bluetoothGatt: BluetoothGatt? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var serviceAdapter: Service_Adapter
    private val bluetoothServices = mutableListOf<BluetoothGattService>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_caracteristics)
        btn_disconnect = findViewById(R.id.btn_disconnect)
        bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        val btnGoToActivity3: Button = findViewById(R.id.btnGoToActivity3)

        btnGoToActivity3.setOnClickListener {
            val deviceAddress = device.address // Remplacez par la méthode pour obtenir l'adresse de l'appareil

            if (deviceAddress.isNullOrEmpty()) {
                Toast.makeText(this, "Adresse du périphérique introuvable.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, SendMessageActivity::class.java)
            intent.putExtra("device_address", deviceAddress) // Transmettez l'adresse
            startActivity(intent)
        }


        val deviceAddress = intent.getStringExtra("device_address")
        if (deviceAddress == null) {
            showToast("Adresse du périphérique non trouvée.")
            finish()
            return
        }

        device = bluetoothAdapter.getRemoteDevice(deviceAddress)

        // Configure RecyclerView
        recyclerView = findViewById(R.id.servicesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        serviceAdapter = Service_Adapter(bluetoothServices)
        recyclerView.adapter = serviceAdapter

        // Vérification des permissions et connexion au GATT
        if (checkPermissions()) {
            connectToGatt(device)
        } else {
            requestBluetoothPermissions()
        }

        btn_disconnect.setOnClickListener {
            disconnectBluetoothGatt()
        }


    }

    private fun disconnectBluetoothGatt() {
        if (hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null
            showToast("Appareil déconnecté.")
        } else {
            showToast("Permission Bluetooth CONNECT nécessaire pour déconnecter.")
        }

        // Optionally, navigate back or finish the activity
        finish()
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }

    private fun checkPermissions(): Boolean {
        return listOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
        ).all { hasPermission(it) }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestBluetoothPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            REQUEST_BLUETOOTH_PERMISSION
        )
    }

    private fun connectToGatt(device: BluetoothDevice) {
        try {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                showToast("Permission Bluetooth CONNECT attendue.")
                return
            }

            bluetoothGatt = device.connectGatt(this, false, object : BluetoothGattCallback() {
                override fun onConnectionStateChange(
                    gatt: BluetoothGatt?, status: Int, newState: Int
                ) {
                    super.onConnectionStateChange(gatt, status, newState)
                    if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothGatt.STATE_CONNECTED) {
                        runOnUiThread {
                            showToast("Connexion réussie !")
                        }
                        gatt?.discoverServices()
                    } else {
                        runOnUiThread {
                            showToast("Erreur de connexion  !")
                        }
                        gatt?.close()
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                    super.onServicesDiscovered(gatt, status)
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        gatt?.services?.let { discoveredServices ->
                            runOnUiThread {
                                bluetoothServices.clear()
                                bluetoothServices.addAll(discoveredServices)
                                serviceAdapter.notifyDataSetChanged() // Met à jour l'affichage
                            }
                            // Afficher les détails des services et caractéristiques
                            gatt?.services?.forEach { service ->
                                Affichage_service_info(service) } }
                    } else {
                        showToast("Erreur lors de la découverte des services")
                    }
                }
            })
        } catch (e: SecurityException) {
            Log.e("GATT Error", "Erreur de sécurité lors de la connexion au périphérique : ${e.message}")
            showToast("Erreur de permission lors de la connexion au périphérique.")
        }
    }

    private fun Affichage_service_info(service: BluetoothGattService) {

        service.characteristics.forEach { characteristic ->
            val properties = mutableListOf<String>()
            if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) {
                properties.add("READ")
            }
            if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) {
                properties.add("WRITE")
            }
            if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                properties.add("NOTIFY")
            }

            bluetoothServices.add(service)
            runOnUiThread {
                serviceAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                connectToGatt(device)
            } else {
                permissions.forEachIndexed { index, permission ->
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        when (permission) {
                            Manifest.permission.BLUETOOTH_CONNECT -> showToast("Permission Bluetooth CONNECT refusée.")
                            Manifest.permission.ACCESS_FINE_LOCATION -> showToast("Permission Localisation refusée.")
                            Manifest.permission.BLUETOOTH_SCAN -> showToast("Permission Bluetooth SCAN refusée.")
                        }
                    }
                }
                if (permissions.any { !shouldShowRequestPermissionRationale(it) }) {
                    showToast("Les permissions sont nécessaires. Veuillez les activer dans les paramètres.")
                } else {
                    requestBluetoothPermissions()
                }
            } } }


    override fun onDestroy() {
        super.onDestroy()

        // Check if we have Bluetooth permissions before performing any operation
        if (hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        } else {
            showToast("Permission Bluetooth CONNECT nécessaire pour fermer la connexion.")
        }
        bluetoothGatt = null
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }



}
