package com.example.obd2scanner

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var obdManager: OBDManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // කෝඩ් එකෙන්ම Layout එක හැදීම (XML ෆයිල් අවශ්‍ය නොවේ)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
            setBackgroundColor(Color.parseColor("#121212")) // Dark background
        }

        val tvStatus = TextView(this).apply {
            text = "Status: Not Connected"
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }

        val btnConnect = Button(this).apply {
            text = "Connect to OBD2"
            textSize = 16f
            setBackgroundColor(Color.parseColor("#3F51B5"))
            setTextColor(Color.WHITE)
        }

        layout.addView(tvStatus)
        layout.addView(btnConnect)
        setContentView(layout)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        btnConnect.setOnClickListener {
            // ඔබේ ELM327 බ්ලූටූත් ඩිවයිස් එකේ MAC Address එක මෙතැනට දෙන්න (උදා: "00:1D:A5:00:12:34")
            val deviceAddress = "XX:XX:XX:XX:XX:XX" 
            val device: BluetoothDevice? = bluetoothAdapter?.getRemoteDevice(deviceAddress)

            if (device != null) {
                obdManager = OBDManager(device)
                Thread {
                    val success = obdManager?.connect() == true
                    runOnUiThread {
                        if (success) {
                            tvStatus.text = "Status: Connected to OBD2 Scanner!"
                            tvStatus.setTextColor(Color.GREEN)
                        } else {
                            tvStatus.text = "Status: Connection Failed!"
                            tvStatus.setTextColor(Color.RED)
                        }
                    }
                }.start()
            } else {
                tvStatus.text = "Status: Bluetooth Device not found!"
                tvStatus.setTextColor(Color.YELLOW)
            }
        }
    }
}