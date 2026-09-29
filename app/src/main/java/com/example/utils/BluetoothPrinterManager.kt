package com.example.utils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.UUID

class BluetoothPrinterManager(private val context: Context) {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    @SuppressLint("MissingPermission")
    suspend fun printInvoice(invoiceText: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Bluetooth is not enabled", Toast.LENGTH_SHORT).show()
                }
                return@withContext false
            }
            
            val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
            if (pairedDevices.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "No paired Bluetooth devices found", Toast.LENGTH_SHORT).show()
                }
                return@withContext false
            }
            
            // Try to find a device that looks like a printer
            val device = pairedDevices.firstOrNull { 
                it.name?.contains("print", ignoreCase = true) == true || 
                it.name?.contains("pos", ignoreCase = true) == true || 
                it.bluetoothClass.majorDeviceClass == 1536 
            } ?: pairedDevices.first() // Fallback to first paired device

            val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard SerialPortService ID
            socket = device.createRfcommSocketToServiceRecord(uuid)
            socket?.connect()
            outputStream = socket?.outputStream

            // ESC/POS Init
            val escPosInit = byteArrayOf(0x1B, 0x40)
            outputStream?.write(escPosInit)
            
            // Print Text
            outputStream?.write(invoiceText.toByteArray())
            
            // Feed lines and cut
            outputStream?.write("\n\n\n\n".toByteArray())
            
            outputStream?.flush()
            socket?.close()
            
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Printed successfully on ${device.name}", Toast.LENGTH_SHORT).show()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            try { socket?.close() } catch (ex: Exception) {}
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Failed to print: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            false
        }
    }
}
