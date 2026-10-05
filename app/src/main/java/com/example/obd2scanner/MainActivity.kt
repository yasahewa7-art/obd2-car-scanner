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
                    sendCommand(outputStream, inputStream, "AT Z\r")
                    sendCommand(outputStream, inputStream, "AT SP 0\r")

                    runOnUiThread {
                        statusTextView.text = "සාර්ථකව සම්බන්ධ විය! දත්ත ලබාගනිමින්..."
                    }

                    while (socket.isConnected) {
                        val rpm = requestRPM(outputStream, inputStream)
                        val temp = requestTemperature(outputStream, inputStream)

                        runOnUiThread {
                            statusTextView.text = """
                                🚗 OBD2 Scanner Connected (${elmDevice.name})
                                
                                ⚙️ Engine RPM: $rpm RPM
                                🌡️ Coolant Temp: $temp °C
                            """.trimIndent()
                        }

                        Thread.sleep(1000)
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
        try {
            out.write(command.toByteArray())
            out.flush()
            
            val buffer = ByteArray(1024)
            val bytes = input.read(buffer)
            if (bytes > 0) {
                return String(buffer, 0, bytes)
            }
        } catch (e: Exception) {
            // දෝෂ මඟහරවා ගැනීම සඳහා
        }
        return ""
    }

    private fun requestRPM(out: OutputStream, input: InputStream): Int {
        try {
            val response = sendCommand(out, input, "01 0C\r")
            if (response.isBlank()) return 0
            
            val cleanResponse = response.replace(">", "").trim()
            val lines = cleanResponse.split("\n")
            for (line in lines) {
                if (line.contains("41 0C")) {
                    val parts = line.trim().split("\\s+".toRegex())
                    if (parts.size >= 4) {
                        val A = parts[2].toIntOrNull(16) ?: 0
                        val B = parts[3].toIntOrNull(16) ?: 0
                        return ((A * 256) + B) / 4
                    }
                }
            }
        } catch (e: Exception) {
            // දෝෂ මඟහරවා ගැනීම සඳහා
        }
        return 0
    }

    private fun requestTemperature(out: OutputStream, input: InputStream): Int {
        try {
            val response = sendCommand(out, input, "01 05\r")
            if (response.isBlank()) return 0
            
            val cleanResponse = response.replace(">", "").trim()
            val lines = cleanResponse.split("\n")
            for (line in lines) {
                if (line.contains("41 05")) {
                    val parts = line.trim().split("\\s+".toRegex())
                    if (parts.size >= 3) {
                        val A = parts[2].toIntOrNull(16) ?: 0
                        return A - 40
                    }
                }
            }
        } catch (e: Exception) {
            // දෝෂ මඟහරවා ගැනීම සඳහා
        }
        return 0
    }
}
