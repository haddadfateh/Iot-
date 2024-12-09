package com.example.cm2.Adapter

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.view.LayoutInflater
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cm2.R
import android.view.View
import android.view.ViewGroup



class Service_Adapter(
    private val List_services: List<BluetoothGattService>
) : RecyclerView.Adapter<Service_Adapter.ServiceViewHolder>() {

    class ServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val service_Uuid: TextView = itemView.findViewById(R.id.service_Uuid)
        val service_Name: TextView = itemView.findViewById(R.id.service_Name)
        val characteristic: TextView = itemView.findViewById(R.id.characteristic)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.service_item, parent, false)
        return ServiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        val service = List_services[position]
        val serviceName = getServiceName(service.uuid)
        val characteristicInfo = service.characteristics.joinToString("\n") { characteristic ->
            val props = mutableListOf<String>()
            if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) props.add("Read")
            if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) props.add("Write")
            if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) props.add("Notify")
            "  ├── Characteristic UUID: ${characteristic.uuid}\n" +
                    "   ─ Properties: ${props.joinToString(", ")}"
        }

        holder.service_Uuid.text = "Service UUID: ${service.uuid}"
        holder.service_Name.text = "Name : $serviceName"
        holder.characteristic.text = characteristicInfo
    }

    private fun getServiceName(serviceUuid: java.util.UUID): String {
        return when (serviceUuid.toString()) {
            "00001800-0000-1000-8000-00805f9b34fb" -> "Général Access"
            "00001801-0000-1000-8000-00805f9b34fb" -> "Général Attribute"
            "00001802-0000-1000-8000-00805f9b34fb" -> "Immediate Alert"
            "00001803-0000-1000-8000-00805f9b34fb" -> "Link Loss"
            "00001804-0000-1000-8000-00805f9b34fb" -> "Tx Power"
            "00001805-0000-1000-8000-00805f9b34fb" -> "Current Time Service"
            "00001806-0000-1000-8000-00805f9b34fb" -> "Reference Time Update Service"
            "00001808-0000-1000-8000-00805f9b34fb" -> "Glucose"
            "0000180a-0000-1000-8000-00805f9b34fb" -> "Device Information"
            "0000180d-0000-1000-8000-00805f9b34fb" -> "Heart Rate"
            "0000180f-0000-1000-8000-00805f9b34fb" -> "Health Thermometer"
            "00001811-0000-1000-8000-00805f9b34fb" -> "Alert Notification"
            "00001812-0000-1000-8000-00805f9b34fb" -> "Blood Pressure"
            "00001814-0000-1000-8000-00805f9b34fb" -> "Automation IO"
            "00001816-0000-1000-8000-00805f9b34fb" -> "Cycling Power"
            "00001818-0000-1000-8000-00805f9b34fb" -> "Running Speed and Cadence"
            "00001819-0000-1000-8000-00805f9b34fb" -> "Cycling Speed and Cadence"
            "0000181b-0000-1000-8000-00805f9b34fb" -> "Temperature Measurement"
            "0000181e-0000-1000-8000-00805f9b34fb" -> "Environmental Sensing"
            "00001820-0000-1000-8000-00805f9b34fb" -> "Body Composition"
            "00001822-0000-1000-8000-00805f9b34fb" -> "User Data"
            "00001824-0000-1000-8000-00805f9b34fb" -> "Weight Scale"
            "00001827-0000-1000-8000-00805f9b34fb" -> "Inhaler"
            "00001828-0000-1000-8000-00805f9b34fb" -> "Respiratory"
            "00001829-0000-1000-8000-00805f9b34fb" -> "Continuous Glucose Monitoring"
            "0000182a-0000-1000-8000-00805f9b34fb" -> "Mesh"
            "0000182f-0000-1000-8000-00805f9b34fb" -> "Smartphone Service"
            "00001830-0000-1000-8000-00805f9b34fb" -> "Smart Wearables"
            else -> "Service inconnu"
        }
    }
    override fun getItemCount(): Int = List_services.size

}
