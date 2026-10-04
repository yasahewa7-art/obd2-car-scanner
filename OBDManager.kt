import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class OBDManager(private val device: BluetoothDevice) {
    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun connect(): Boolean {
        return try {
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket?.connect()
            outputStream = socket?.outputStream
            inputStream = socket?.inputStream
            sendInitCommands()
            true
        } catch (e: Exception) {
            Log.e("OBDManager", "Connection failed", e)
            false
        }
    }

    private fun sendInitCommands() {
        sendCommand("AT Z")
        Thread.sleep(500)
        sendCommand("AT SP 0")
        Thread.sleep(500)
    }

    fun sendCommand(command: String) {
        try {
            val fullCommand = "$command\r"
            outputStream?.write(fullCommand.toByteArray())
            outputStream?.flush()
        } catch (e: Exception) {
            Log.e("OBDManager", "Failed to send command", e)
        }
    }

    fun readResponse(): String {
        return try {
            val buffer = ByteArray(1024)
            val bytes = inputStream?.read(buffer) ?: 0
            String(buffer, 0, bytes)
        } catch (e: Exception) {
            Log.e("OBDManager", "Failed to read response", e)
            ""
        }
    }

    fun disconnect() {
        try {
            socket?.close()
        } catch (e: Exception) {
            Log.e("OBDManager", "Failed to close socket", e)
        }
    }
}