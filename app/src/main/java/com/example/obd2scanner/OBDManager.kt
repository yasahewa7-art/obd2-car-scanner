package com.example.obd2scanner

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class OBDManager(private val device: BluetoothDevice) {
    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    // සාමාන්‍ය Bluetooth Serial Port Profile (SPP) UUID එක
    private val UUID_SPP: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun connect(): Boolean {
        try {
            socket = device.createRfcommSocketToServiceRecord(UUID_SPP)
            socket?.connect()
            
            inputStream = socket?.inputStream
            outputStream = socket?.outputStream
            
            // ELM327 ආරම්භක විධාන (Initialization commands) යැවීම
            sendCommand("AT Z")    // Reset
            sendCommand("AT E0")   // Echo off
            sendCommand("AT L0")   // Linefeeds off
            sendCommand("AT SP 0") // Auto protocol
            
            return true
        } catch (e: IOException) {
            Log.e("OBDManager", "Connection failed", e)
            try {
                socket?.close()
            } catch (closeException: IOException) {
                Log.e("OBDManager", "Could not close socket", closeException)
            }
            return false
        }
    }

    fun sendCommand(command: String): String {
        try {
            val cmd = "$command\r"
            outputStream?.write(cmd.toByteArray())
            outputStream?.flush()

            val buffer = ByteArray(1024)
            val bytes = inputStream?.read(buffer)
            if (bytes != null && bytes > 0) {
                return String(buffer, 0, bytes)
            }
        } catch (e: IOException) {
            Log.e("OBDManager", "Failed to send command", e)
        }
        return ""
    }

    fun disconnect() {
        try {
            socket?.close()
        } catch (e: IOException) {
            Log.e("OBDManager", "Failed to close socket", e)
        }
    }
}
