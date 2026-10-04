package com.example.obd2scanner

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var obdManager: OBDManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnConnect = findViewById<Button>(R.id.btnConnect)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        btnConnect.setOnClickListener {
            // මෙහි ඔබේ ELM327 බ්ලූටූත් ඩිවයිස් එකේ MAC Address එක දෙන්න (උදා: "00:1D:A5:00:12:34")
            val deviceAddress = "XX:XX:XX:XX:XX:XX" 
            val device: BluetoothDevice? = bluetoothAdapter?.getRemoteDevice(deviceAddress)

            if (device != null) {
                obdManager = OBDManager(device)
                Thread {
                    val success = obdManager?.connect() == true
                    runOnUiThread {
                        if (success) {
                            tvStatus.text = "Connected to OBD2 Scanner!"
                        } else {
                            tvStatus.text = "Connection Failed!"
                        }
                    }
                }.start()
            }
        }
    }
}