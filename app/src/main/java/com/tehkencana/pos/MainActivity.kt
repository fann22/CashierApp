package com.tehkencana.pos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.tehkencana.pos.ui.MainViewModel
import com.tehkencana.pos.ui.screens.AdminScreen
import com.tehkencana.pos.ui.screens.PosScreen

class MainActivity : ComponentActivity() {
    
    // Permission launcher
    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Toast.makeText(this, "Izin Bluetooth diberikan", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Izin Bluetooth ditolak. Fitur printer tidak akan berfungsi.", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request Bluetooth permissions for Android 12+
        requestBluetoothPermissions()
        
        // Inisialisasi ViewModel
        val viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        
        setContent {
            // State untuk pindah layar (POS <-> Admin)
            var isAdminMode by remember { mutableStateOf(false) }
            
            // Setup Tema Warna (Oren Kencana)
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFFD97706),       // Amber 600
                    primaryContainer = Color(0xFFFEF3C7), // Amber 100
                    secondary = Color(0xFFB45309)      // Amber 700
                )
            ) {
                // Logika Navigasi Sederhana
                if (isAdminMode) {
                    AdminScreen(
                        viewModel = viewModel,
                        onClose = { isAdminMode = false }
                    )
                } else {
                    PosScreen(
                        viewModel = viewModel,
                        onAdminTrigger = { isAdminMode = true }
                    )
                }
            }
        }
    }
    
    private fun requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissionsNeeded = mutableListOf<String>()
            
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            
            if (permissionsNeeded.isNotEmpty()) {
                bluetoothPermissionLauncher.launch(permissionsNeeded.toTypedArray())
            }
        }
    }
}