package com.bayzed.scanner.ui.generator

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Text / URL", "Wi-Fi", "Contact")

    var textInput by remember { mutableStateOf("") }
    var wifiSsid by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }
    val wifiEncryption by remember { mutableStateOf("WPA") }

    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }

    val contentToEncode = when (selectedTab) {
        0 -> textInput
        1 -> "WIFI:S:$wifiSsid;T:$wifiEncryption;P:$wifiPassword;;"
        2 -> "BEGIN:VCARD\nVERSION:3.0\nFN:$contactName\nTEL:$contactPhone\nEMAIL:$contactEmail\nEND:VCARD"
        else -> ""
    }

    val qrBitmap = remember(contentToEncode) {
        if (contentToEncode.isNotBlank()) {
            generateQRCodeBitmap(contentToEncode, 512, 512)
        } else {
            null
        }
    }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Generate QR Code") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            when (selectedTab) {
                0 -> {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Enter text or URL") },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = false,
                        maxLines = 4
                    )
                }
                1 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = wifiSsid,
                            onValueChange = { wifiSsid = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Network SSID (Name)") },
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = wifiPassword,
                            onValueChange = { wifiPassword = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Password") },
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true
                        )
                    }
                }
                2 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = contactName,
                            onValueChange = { contactName = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Full Name") },
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = contactPhone,
                            onValueChange = { contactPhone = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Phone Number") },
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = contactEmail,
                            onValueChange = { contactEmail = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Email Address") },
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.size(260.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Generated QR Code",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                        )
                    } else {
                        Text(
                            text = "Enter content above to generate QR code",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (qrBitmap != null) {
                Button(
                    onClick = {
                        try {
                            val cachePath = File(context.cacheDir, "images")
                            cachePath.mkdirs()
                            val file = File(cachePath, "shared_qrcode.png")
                            val stream = FileOutputStream(file)
                            qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                            stream.close()

                            val uri: Uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )

                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/png"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share QR Code"))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share QR Image")
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

fun generateQRCodeBitmap(content: String, width: Int, height: Int): Bitmap? {
    return try {
        val hints = hashMapOf<EncodeHintType, Any>().apply {
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
        }
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints)
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bmp.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bmp
    } catch (e: Exception) {
        null
    }
}
