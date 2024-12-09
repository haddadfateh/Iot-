package models

import android.bluetooth.BluetoothDevice

data class BTDeviceItem(
    val appareil: BluetoothDevice,
    var paired: Boolean,
    var BTtype : String,
    var isconnect: Boolean = false,
    var localConfirmed: Boolean = false,
    var remoteConfirmed: Boolean = false
)

{
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BTDeviceItem) return false
        return appareil.address == other.appareil.address
    }
    override fun hashCode(): Int {
        return appareil.address.hashCode()
    }
}