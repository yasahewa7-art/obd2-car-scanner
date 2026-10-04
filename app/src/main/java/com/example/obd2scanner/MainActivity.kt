package com.example.obd2scanner

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var statusTextView: TextView
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    
    // ELM327 බ්ලූටූත් සර්විස් සඳහා පොදු UUID එක
    private val ELM327_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        statusTextView = TextView(this).apply {
            textSize = 16f
            setPadding(32, 32, 32, 32)
        }
        setContentView(statusTextView)

        checkBluetoothStatus()
    }

    private fun checkBluetoothStatus() {
        if (bluetoothAdapter == null) {
            statusTextView.text = "මෙම උපාංගයේ බ්ලූටූත් (Bluetooth) පහසුකම නොමැත."
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            statusTextView.text = "කරුණාකර ඔබගේ දුරකථනයේ Bluetooth සක්‍රීය (Turn on) කරන්න."
        } else {
            statusTextView.text = "Bluetooth සක්‍රීයයි. යුගල කළ (Paired) ELM327 උපාංගය සෙවීම..."
            connectToELM327()
        }
    }

    private fun connectToELM327() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                statusTextView.text = "බ්ලූටූත් සම්බන්ධ වීමට අවසර (Permission) අවශ්‍යයි."
                return
            }
        }

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        val elmDevice: BluetoothDevice? = pairedDevices?.find { 
            it.name?.contains("OBD", ignoreCase = true) == true || it.name?.contains("ELM", ignoreCase = true) == true 
        }

        if (elmDevice != null) {
            statusTextView.text = "සම්බන්ධ වෙමින් පවතී: ${elmDevice.name}..."
            
            Thread {
                try {
                    val socket: BluetoothSocket = elmDevice.createRfcommSocketToServiceRecord(ELM327_UUID)
                    bluetoothAdapter?.cancelDiscovery()
                    socket.connect()
                    
                    val inputStream: InputStream = socket.inputStream
                    val outputStream: OutputStream = socket.outputStream

                    // ELM327 ආරම්භක විධාන යැවීම (Initialization)
                    sendCommand(outputStream, inputStream, "AT Z\r") // Reset
                    sendCommand(outputStream, inputStream, "AT SP 0\r") // Automatic protocol detection

                    runOnUiThread {
                        statusTextView.text = "සාර්ථකව සම්බන්ධ විය! දත්ත ලබාගනිමින්..."
                    }

                    // නිරන්තරයෙන් RPM සහ Temperature ලබාගැනීමේ ලූප් එක
                    while (socket.isConnected) {
                        // 1. Engine RPM ලබාගැනීම (PID: 01 0C)
                        val rpm = requestRPM(outputStream, inputStream)
                        
                        // 2. Coolant Temperature ලබාගැනීම (PID: 01 05)
                        val temp = requestTemperature(outputStream, inputStream)

                        runOnUiThread {
                            statusTextView.text = """
                                🚗 OBD2 Scanner Connected (${elmDevice.name})
                                
                                ⚙️ Engine RPM: $rpm RPM
                                🌡️ Coolant Temp: $temp °C
                            """.trimIndent()
                        }

                        Thread.sleep(1000) // තත්පර 1 කට වතාවක් අප්ඩේට් වේ
                    }

                    socket.close()
                } catch (e: IOException) {
                    runOnUiThread {
                        statusTextView.text = "සම්බන්ධතාවය බිඳ වැටුණි: ${e.message}"
                    }
                }
            }.start()

        } else {
            statusTextView.text = "ELM327 හෝ OBD ස්කෑනරයක් හමු නොවීය! බ්ලූටූත් සැකසුම් හරහා ස්කෑනරය Pair කරගන්න."
        }
    }

    private fun sendCommand(out: OutputStream, input: InputStream, command: String): String {
        out.write(command.toByteArray())
        out.flush()
        
        val buffer = ByteArray(1024)
        val bytes = input.read(buffer)
        return String(buffer, 0, bytes)
    }

    private fun requestRPM(out: OutputStream, input: InputStream): Int {
        try {
            val response = sendCommand(out, input, "01 0C\r")
            // ප්‍රතිචාරය උදාහරණයක් ලෙස "41 0C 1A F8" වැනි විය හැක
            val cleanResponse = response.replace(">", "").trim()
            val lines = cleanResponse.split("\n")
            for (line in lines) {
                if (line.contains("41 0C")) {
                    val parts = line.trim().split(" ")
                    if (parts.size >= 4) {
                        val A = parts[2].toInt(16)
                        val B = parts[3].toInt(16)
                        // RPM ගණනය කිරීමේ සූත්‍රය: ((A * 256) + B) / 4
                        return ((A * 256) + B) / 4
                    }
                }
            }
        } catch (e: Exception) {
            // දෝෂයක් මග හැරීමට
        }
        return 0
    }

    private fun requestTemperature(out: OutputStream, input: InputStream): Int {
        try {
            val response = sendCommand(out, input, "01 05\r")
            // ප්‍රතිචාරය උදාහරණයක් ලෙස "41 05 7B" විය හැක
            val cleanResponse = response.replace(">", "").trim()
            val lines = cleanResponse.split("\n")
            for (line in lines) {
                if (line.contains("41 05")) {
                    val parts = line.trim().split(" ")
                    if (parts.size >= 3) {
                        val A = parts[2].toInt(16)
                        // උෂ්ණත්වය ගණනය කිරීමේ සූත්‍රය: A - 40
                        return A - 40
                    }
                }
            }
        } catch (e: Exception) {
            // දෝෂයක් මග හැරීමට
        }
        return 0
    }
}
