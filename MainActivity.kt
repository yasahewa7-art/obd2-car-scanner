package com.example.obd2carscanner

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val REQUEST_BLUETOOTH_PERMISSIONS = 1
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private lateinit var statusTextView: TextView
    private lateinit var connectButton: Button
    private var obdManager: OBDManager? = null

    override fun onCreate(savedInstanceState: Bundle  ?) {
        super.onCreate(savedInstanceState)
        
        // මූලික UI සැකසුම (Programmatic Layout)
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        statusTextView = TextView(this).apply {
            text = "OBD2 Scanner Status: Disconnected"
            textSize = 16f
            setPadding(0, 0, 0, 20)
        }

        connectButton = Button(this).apply {
            text = "Connect to ELM327"
            setOnClickListener {
                checkPermissionsAndConnect()
            }
        }

        layout.addView(statusTextView)
        layout.addView(connectButton)
        setContentView(layout)
    }

    private fun checkPermissionsAndConnect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN),
                    REQUEST_BLUETOOTH_PERMISSIONS
                )
                return
            }
        }
        
        connectToOBD()
    }

    private fun connectToOBD() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_SHORT).show()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Please turn on Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        // යුගල කරන ලද (Paired) ඩිවයිස් අතරින් ELM327 සොයාගැනීම
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
            val elmDevice = pairedDevices?.find { it.name != null && (it.name.contains("OBD", ignoreCase = true) || it.name.contains("ELM", ignoreCase = true)) }

            if (elmDevice != null) {
                statusTextView.text = "Connecting to ${elmDevice.name}..."
                
                // Thread එකක් හරහා පසුබිමේ සම්බන්ධ වීම (Background Connection)
                Thread {
                    obdManager = OBDManager(elmDevice)
                    val success = obdManager?.connect() ?: false
                    
                    runOnUiThread {
                        if (success) {
                            statusTextView.text = "Connected Successfully!"
                            Toast.makeText(this, "OBD2 Scanner Connected!", Toast.LENGTH_SHORT).show()
                        } else {
                            statusTextView.text = "Connection Failed!"
                            Toast.makeText(this, "Failed to connect to scanner.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }.start()
            } else {
                Toast.makeText(this, "No paired ELM327/OBD device found. Please pair it in Bluetooth settings first.", Toast.LENGTH_LONG).show()
                statusTextView.text = "ELM327 device not found in paired list."
            }
        }
    }
}