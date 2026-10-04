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
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var statusTextView: TextView
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    
    // ELM327 බ්ලූටූත් සර්විස් සඳහා පොදු UUID එක
    private val ELM327_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        statusTextView = TextView(this).apply {
            textSize = 18f
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
        // Android 12 සහ ඉහළ සංස්කරණ සඳහා BLUETOOTH_CONNECT අවසරය පරීක්ෂා කිරීම
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                statusTextView.text = "බ්ලූටූත් සම්බන්ධ වීමට අවසර (Permission) අවශ්‍යයි."
                return
            }
        }

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        val elmDevice: BluetoothDevice? = pairedDevices?.find { it.name?.contains("OBD", ignoreCase = true) == true || it.name?.contains("ELM", ignoreCase = true) == true }

        if (elmDevice != null) {
            statusTextView.text = "සම්බන්ධ වෙමින් පවතී: ${elmDevice.name}..."
            
            // වෙනම Thread එකක් හරහා බ්ලූටූත් සම්බන්ධතාවය ඇති කිරීම
            Thread {
                try {
                    val socket: BluetoothSocket = elmDevice.createRfcommSocketToServiceRecord(ELM327_UUID)
                    bluetoothAdapter?.cancelDiscovery()
                    socket.connect()
                    
                    runOnUiThread {
                        statusTextView.text = "සාර්ථකව සම්බන්ධ විය! (${elmDevice.name})"
                    }
                    
                    // මෙහිදී OBD2 විමසුම් (Commands) යැවීමට කෝඩ් එක ලිවිය හැක
                    
                } catch (e: IOException) {
                    runOnUiThread {
                        statusTextView.text = "සම්බන්ධ වීම අසාර්ථකයි: ${e.message}"
                    }
                }
            }.start()

        } else {
            statusTextView.text = "ELM327 හෝ OBD ස්කෑනරයක් හමු නොවීය! කරුණාකර දුරකථන බ්ලූටූත් සැකසුම් හරහා ස්කෑනරය Pair කරගන්න."
        }
    }
}
