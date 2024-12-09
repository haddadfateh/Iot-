package com.example.cm2.Activity

import android.Manifest
import android.bluetooth.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cm2.R
import java.util.UUID

class SendMessageActivity : AppCompatActivity() {

    private val REQUEST_PERMISSION_CODE = 1001

    private lateinit var champ_Message: EditText
    private lateinit var btn_envoi: Button
    private lateinit var btn_retour: Button

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothManager: BluetoothManager
    private var bluetoothGatt: BluetoothGatt? = null
    private lateinit var appareil: BluetoothDevice

    companion object {
        const val SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb"
        const val CHARACTERISTIC_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb"
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_communication)
        champ_Message = findViewById(R.id.ChampMessage)
        btn_envoi = findViewById(R.id.boutton_envoyer)
        btn_retour = findViewById(R.id.boutton_retour)
        bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        // Récupération de l'adresse
        val Address = intent.getStringExtra("device_address")
        if (Address != null) {
            appareil = bluetoothAdapter.getRemoteDevice(Address)
            connectToGatt(appareil)
        } else {
            showToast("périphérique introuvable.")
            finish()
        }
        btn_envoi.setOnClickListener {
            val message = champ_Message.text.toString()
            if (message.isNotEmpty()) {
                sendMessageToHM10(message)
            } else {
                showToast("Message vide !")
            }
        }
        btn_retour.setOnClickListener {
            finish()
        }
    }

    private fun connectToGatt(device: BluetoothDevice) {
        if (checkBluetoothPermissions()) {
            try {
                bluetoothGatt = device.connectGatt(this, false, object : BluetoothGattCallback() {
                    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                        super.onConnectionStateChange(gatt, status, newState)
                        if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothGatt.STATE_CONNECTED) {
                            runOnUiThread {
                                showToast("Connexion réussie à l'appareil.")
                            }
                            gatt?.discoverServices()
                        } else if (status == BluetoothGatt.GATT_FAILURE || newState == BluetoothGatt.STATE_DISCONNECTED) {
                            runOnUiThread {
                                showToast("Erreur de connexion.")
                            }
                            gatt?.close()
                        }
                    }

                    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                        super.onServicesDiscovered(gatt, status)
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            val service = gatt?.getService(UUID.fromString(SERVICE_UUID))
                            if (service == null) {
                                runOnUiThread {
                                    showToast("Service non trouvé.")
                                }
                            } else {
                                val characteristic = service.getCharacteristic(UUID.fromString(
                                    CHARACTERISTIC_UUID
                                ))
                                if (characteristic != null) {
                                    // à remplir
                                } else {
                                    runOnUiThread {
                                        showToast("Caractéristique non trouvée.")
                                    } } }
                        } else {
                            runOnUiThread {
                                showToast("Erreur lors de la découverte des services.")
                            }
                        }
                    }

                    override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
                        super.onCharacteristicWrite(gatt, characteristic, status)
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            runOnUiThread {
                                showToast("Message envoyé avec succès.")
                            }
                        } else {
                            runOnUiThread {
                                showToast("Erreur lors de l'envoi du message.")
                            }
                        }
                    }

                    override fun onCharacteristicChanged(
                        gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?
                    ) {
                        super.onCharacteristicChanged(gatt, characteristic)
                        val value = characteristic?.value
                        if (value != null) {
                            val message = String(value)
                            runOnUiThread {
                                showToast("Réponse de l'Arduino: $message")
                            }
                        }
                    }
                })
            } catch (e: SecurityException) {
                showToast("Erreur de permission : ${e.message}")
            }
        } else {
            requestPermissions()
        }
    }

    private fun sendMessageToHM10(message: String) {
        if (checkBluetoothPermissions()) {
            try {
                bluetoothGatt?.let { gatt ->
                    val service = gatt.getService(UUID.fromString(SERVICE_UUID))
                    val characteristic = service?.getCharacteristic(UUID.fromString(
                        CHARACTERISTIC_UUID
                    ))
                    if (characteristic != null) {
                        val byteMessage = message.toByteArray()

                        characteristic.value = byteMessage

                        val success = gatt.writeCharacteristic(characteristic)
                        if (!success) {
                            showToast("Erreur lors de l'envoi du message.")
                        }
                    } else {
                        showToast("Caractéristique non trouvée.")
                    }
                } ?: run {
                    showToast("Bluetooth non connecté.")
                }
            } catch (e: SecurityException) {
                showToast("Erreur de permission : ${e.message}")
            }
        } else {
            requestPermissions()
        }
    }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun checkBluetoothPermissions(): Boolean {
        val permissionLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val permissionBluetooth = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
        return permissionBluetooth == PackageManager.PERMISSION_GRANTED && permissionLocation == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                showToast("Permissions accordée.")
            } else {
                showToast("Permissions non accordée.")
            }
        }
    }
}
