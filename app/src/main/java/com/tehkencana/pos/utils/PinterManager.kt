package com.tehkencana.pos.utils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.util.UUID

class PrinterManager(private val context: Context) {
    // UUID Standard untuk Printer Thermal (Serial Port Profile)
    private val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // Check if we have Bluetooth permission
    fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Older versions don't need runtime permission
        }
    }

    @SuppressLint("MissingPermission")
    fun printReceipt(macAddress: String, receiptText: String) {
        if (macAddress.isEmpty()) {
            Toast.makeText(context, "Printer belum disetting!", Toast.LENGTH_SHORT).show()
            return
        }

        if (!hasBluetoothPermission()) {
            Toast.makeText(context, "Izin Bluetooth diperlukan!", Toast.LENGTH_SHORT).show()
            return
        }

        Thread {
            try {
                val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val adapter = bluetoothManager.adapter
                
                if (adapter == null || !adapter.isEnabled) {
                    Toast.makeText(context, "Bluetooth tidak aktif!", Toast.LENGTH_SHORT).show()
                    return@Thread
                }
                
                val device = adapter.getRemoteDevice(macAddress)
                val socket = device.createRfcommSocketToServiceRecord(uuid)
                
                adapter.cancelDiscovery() // Hemat baterai & bandwidth
                socket.connect()
                
                val outputStream = socket.outputStream

                // Init Printer
                outputStream.write(byteArrayOf(0x1B, 0x40)) 

                // Kirim Teks
                outputStream.write(receiptText.toByteArray())
                
                // Cut Paper Command (Feed 3 lines + Cut)
                outputStream.write(byteArrayOf(0x1D, 0x56, 66, 0))

                Thread.sleep(1000)
                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
                // Show error on main thread
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
    
    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        if (!hasBluetoothPermission()) {
            return emptyList()
        }
        
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return bluetoothManager?.adapter?.bondedDevices?.toList() ?: emptyList()
    }
}