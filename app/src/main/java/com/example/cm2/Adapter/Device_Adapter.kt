package com.example.cm2.Adapter


import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import com.example.cm2.Activity.CaracteristicsActivity
import com.example.cm2.R
import models.BTDeviceItem

class Device_Adapter(
    context: Context,
    private val resource: Int,
    private val devices: MutableList<BTDeviceItem>

) : ArrayAdapter<BTDeviceItem>(context, resource, devices) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(resource, parent, false)
        val appareil = devices[position]
        val champ_address = view.findViewById<TextView>(R.id.deviceAddress)
        val champ_name = view.findViewById<TextView>(R.id.deviceName)
        val champ_status= view.findViewById<TextView>(R.id.deviceStatus)
        val champ_type = view.findViewById<TextView>(R.id.deviceType)
        val pairButton = view.findViewById<Button>(R.id.pairButton)
        val removeButton = view.findViewById<Button>(R.id.removeButton)

        // Vérification des permissions
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            champ_name.text = appareil.appareil.name ?: "Nom Inconnu"
            champ_address.text = appareil.appareil.address
            champ_type.text = appareil.BTtype
            champ_status.text = if (appareil.paired and appareil.isconnect) "Connected" else "Disconnected"
        } else {
            champ_name.text = "permission denied"
            champ_address.text = "permission denied"
        }

        pairButton.setOnClickListener {
            Log.d("Appairage", "Appairer avec ${appareil.appareil.name}")
            if (!appareil.paired) {
                Appairer(appareil.appareil)
                if (Appairer(appareil.appareil)) {
                    appareil.paired = true
                    appareil.isconnect = true
                    updateDevice(appareil) } } }

        removeButton.setOnClickListener {
            if (appareil.paired) {
                Desapairer(appareil.appareil)
                appareil.paired = false
                appareil.isconnect = false
                updateDevice(appareil)
            }
        }

        pairButton.visibility = View.VISIBLE
        removeButton.visibility = View.GONE
        if (appareil.paired) {
            pairButton.visibility = View.GONE
            removeButton.visibility = View.VISIBLE
        } else {
            pairButton.visibility = View.VISIBLE
            removeButton.visibility = View.GONE
        }


        val cardView: CardView = view.findViewById(R.id.cardView)
        cardView.setOnClickListener {
            val intent = Intent(context, CaracteristicsActivity::class.java)
            intent.putExtra("device_address", appareil.appareil.address)
            context.startActivity(intent)
        }
        return view
    }

    private fun Appairer(appareil: BluetoothDevice): Boolean {
        return try {
            val method = appareil.javaClass.getMethod("createBond")
            val success = method.invoke(appareil) as Boolean
            if (success) {
                showToast(message="Appairage réussi: ${appareil.name ?: "Nom Inconnu"}")
                // Met à jour l'appareil concerné
                val index = devices.indexOfFirst { it.appareil.address == appareil.address }
                if (index != -1) {
                    devices[index].paired = true
                    notifyDataSetChanged()
                }
            } else {
                showToast(message="Échec de l'appairage avec : ${appareil.address}")
            }
            success
        } catch (e: SecurityException) {
            showToast(message="Permission refusée pour appairer l'appareil")
            false
        } catch (e: Exception) {
            showToast(message="Error : ${e.message}")
            false
        }
    }

    private fun Desapairer(appareil: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            showToast(message="Permission refusée se déconnecter")
            return
        }
        try {
            val method = appareil.javaClass.getMethod("removeBond")
            val succes = method.invoke(appareil) as Boolean
            if (succes) {
                val index = devices.indexOfFirst { it.appareil.address == appareil.address }
                if (index != -1) {
                    devices[index].isconnect = false
                    devices[index].paired = false
                    notifyDataSetChanged()
                }
                showToast(message="Appairage supprimé : ${appareil.address }")
            } else {
                showToast(message="Échec lors de la suppression de l'appairage : Adresse ${appareil.address}")
            }
        } catch (e: Exception) {
            showToast(message="Error : ${e.message}")
        }
    }


    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
    fun updateDevice(updatedDevice: BTDeviceItem) {
        val index = devices.indexOfFirst { it.appareil.address == updatedDevice.appareil.address }
        if (index != -1) {
            devices[index] = updatedDevice
            notifyDataSetChanged()
        }
    }

}
