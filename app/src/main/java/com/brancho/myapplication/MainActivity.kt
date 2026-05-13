package com.brancho.myapplication

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID

class MainActivity : AppCompatActivity() {

    // Componentes UI
    private lateinit var txtEstadoBT: TextView
    private lateinit var txtTemperatura: TextView
    private lateinit var txtHumedad: TextView
    private lateinit var txtIconoEstado: TextView
    private lateinit var txtEstadoClima: TextView
    private lateinit var txtMensajeAlerta: TextView
    private lateinit var btnConectar: Button
    private lateinit var statusDot: View
    private lateinit var dataCard: MaterialCardView
    lateinit var mainLayout: ConstraintLayout

    // Bluetooth
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var isConnected = false
    private var readingThread: Thread? = null

    // UUID estándar para SPP (Serial Port Profile)
    private val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // Handler para actualizar UI
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val REQUEST_BLUETOOTH_PERMISSIONS = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar vistas
        txtEstadoBT = findViewById(R.id.txtEstadoBT)
        txtTemperatura = findViewById(R.id.txtTemperatura)
        txtHumedad = findViewById(R.id.txtHumedad)
        txtIconoEstado = findViewById(R.id.txtIconoEstado)
        txtEstadoClima = findViewById(R.id.txtEstadoClima)
        txtMensajeAlerta = findViewById(R.id.txtMensajeAlerta)
        btnConectar = findViewById(R.id.btnConectar)
        statusDot = findViewById(R.id.statusDot)
        dataCard = findViewById(R.id.dataCard)
        mainLayout = findViewById(R.id.mainLayout)

        btnConectar.setOnClickListener {
            if (isConnected) {
                desconectarBluetooth()
            } else {
                verificarPermisosYConectar()
            }
        }

        verificarPermisosIniciales()
    }

    private fun verificarPermisosIniciales() {
        if (bluetoothAdapter == null) {
            txtEstadoBT.text = "Error: Sin Bluetooth"
            Toast.makeText(this, "Bluetooth no soportado", Toast.LENGTH_LONG).show()
            btnConectar.isEnabled = false
        }
    }

    private fun verificarPermisosYConectar() {
        val permisos = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permisos.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permisos.add(Manifest.permission.BLUETOOTH_SCAN)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                permisos.add(Manifest.permission.BLUETOOTH)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                permisos.add(Manifest.permission.BLUETOOTH_ADMIN)
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permisos.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permisos.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permisos.toTypedArray(), REQUEST_BLUETOOTH_PERMISSIONS)
        } else {
            conectarBluetooth()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                conectarBluetooth()
            } else {
                Toast.makeText(this, "Permisos denegados", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun conectarBluetooth() {
        if (bluetoothAdapter?.isEnabled == false) {
            Toast.makeText(this, "Enciende el Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return
        }

        val pairedDevices = bluetoothAdapter?.bondedDevices
        val device = pairedDevices?.find { it.name.contains("HC-05") || it.name.contains("HC-06") }
            ?: pairedDevices?.firstOrNull()

        if (device == null) {
            txtEstadoBT.text = "No hay dispositivos emparejados"
            return
        }

        txtEstadoBT.text = "Conectando a ${device.name}..."

        Thread {
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID)
                bluetoothSocket?.connect()
                isConnected = true
                handler.post {
                    txtEstadoBT.text = "Conectado a ${device.name}"
                    statusDot.setBackgroundColor(Color.GREEN)
                    btnConectar.text = "Desconectar"
                    iniciarLectura()
                }
            } catch (e: Exception) {
                handler.post {
                    txtEstadoBT.text = "Error al conectar"
                    isConnected = false
                }
            }
        }.start()
    }

    private fun iniciarLectura() {
        readingThread = Thread {
            try {
                val reader = BufferedReader(InputStreamReader(bluetoothSocket?.inputStream))
                while (isConnected) {
                    val line = reader.readLine()
                    if (line != null) {
                        handler.post { procesarDatoRecibido(line) }
                    }
                }
            } catch (e: Exception) {
                handler.post { desconectarBluetooth() }
            }
        }
        readingThread?.start()
    }

    fun procesarDatoRecibido(dato: String) {
        try {
            val limpio = dato.trim()
            if (limpio.isEmpty()) {
                mostrarError()
                return
            }

            val partes = limpio.split(",")
            val temp = partes[0].toFloatOrNull()
            
            if (partes.size > 1) {
                val hum = partes[1].toFloatOrNull()
                if (hum != null) txtHumedad.text = String.format("%.1f%%", hum)
            }

            if (temp == null) {
                mostrarError()
                return
            }

            // REQ-01: Formato "XX°C"
            txtTemperatura.text = String.format("%.1f°C", temp)

            when {
                // REQ-03: Alerta por calor extremo (>= 35°C)
                temp >= 35 -> {
                    mainLayout.setBackgroundColor(Color.parseColor("#FF0000")) // ROJO
                    txtMensajeAlerta.text = "¡ALERTA: CALOR EXTREMO!"
                    txtIconoEstado.text = "🔥"
                    txtEstadoClima.text = "Calor Extremo"
                }
                // REQ-02: Rango normal (0-34°C)
                temp in 0.0..34.0 -> {
                    mainLayout.setBackgroundColor(Color.parseColor("#00FF00")) // VERDE
                    txtMensajeAlerta.text = ""
                    txtIconoEstado.text = "✅"
                    txtEstadoClima.text = "Normal"
                }
                else -> {
                    // Otros casos (ej. bajo cero)
                    mainLayout.setBackgroundColor(Color.CYAN)
                    txtMensajeAlerta.text = ""
                }
            }
        } catch (e: Exception) {
            mostrarError()
        }
    }

    private fun mostrarError() {
        // REQ-04: Manejo de errores
        mainLayout.setBackgroundColor(Color.parseColor("#FFFF00")) // AMARILLO
        txtMensajeAlerta.text = "Error de lectura"
        txtTemperatura.text = "Error"
        txtIconoEstado.text = "⚠️"
        txtEstadoClima.text = "Dato inválido"
    }

    private fun desconectarBluetooth() {
        isConnected = false
        try { bluetoothSocket?.close() } catch (e: Exception) {}
        txtEstadoBT.text = "Desconectado"
        statusDot.setBackgroundColor(Color.RED)
        btnConectar.text = "Conectar Bluetooth"
    }

    override fun onDestroy() {
        super.onDestroy()
        desconectarBluetooth()
    }
}
