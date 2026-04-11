package oblitusnumen.vpnspy.impl

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.VpnService
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.MutableSharedFlow
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

//class SpyService : Service() {
//    private val binder = LocalBinder()
//
//    inner class LocalBinder : Binder() {
//        fun getService(): WebSocketService = this@WebSocketService
//    }
//
//    override fun onCreate() {
//        log("startService")
//        super.onCreate()
//        startForeground(1, createNotification(this)) // Keeps the service alive
//        mediaPlayer = MediaPlayer.create(this, R.raw.beep)
//        initWebSocket()
//    }
//
//    override fun onBind(p0: Intent?): IBinder? {
//        return null
//    }
//}


class MyForegroundService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val logRunnable = object : Runnable {
        override fun run() {
            log("MyForegroundService", "Service is running...")
            handler.postDelayed(this, 1000) // repeat every 1 second
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        handler.post(logRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(logRunnable)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundService() {
        val channelId = "foreground_service_channel"

        // Create notification channel (required for Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Foreground Service")
            .setContentText("Running...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        startForeground(1, notification)
    }
}




class AudioRecordingService : Service() {

    private var recorder: MediaRecorder? = null
    private var outputFile: String? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        startRecording()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundService() {
        val channelId = "audio_record_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Audio Recording",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Recording audio")
            .setContentText("Microphone is active")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()

        startForeground(1, notification)
    }

    private fun startRecording() {
        val file = File(filesDir, "recording_${System.currentTimeMillis()}.m4a")
        outputFile = file.absolutePath

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(outputFile)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

            prepare()
            start()
        }
    }

    private fun stopRecording() {
        recorder?.apply {
            try {
                stop()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            release()
        }
        recorder = null
    }
}

fun startRecordingService(context: Context) {
    val intent = Intent(context, AudioRecordingService::class.java)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}


//class PacketMonitorService : VpnService() {
//    private var vpnInterface: ParcelFileDescriptor? = null
//    private var isRunning = false
//    private val logFlow = MutableSharedFlow<String>()
//
//    companion object {
//        private const val VPN_MTU = 1500
//        private const val VIRTUAL_IP = "10.0.0.2"
//        private const val VIRTUAL_IP_PREFIX = 32
//    }
//
//    override fun onCreate() {
//        super.onCreate()
//        startMonitoring()
//    }
//
//    private fun startMonitoring() {
//        // Step 1: Prepare the VPN (user authorization)
//        val intent = prepare(this)
//        if (intent != null) {
//            // Start activity to get user permission
//            startActivityForResult(intent, REQUEST_VPN_PERMISSION)
//        } else {
//            // Already authorized, proceed
//            establishVpnConnection()
//        }
//    }
//
//    private fun establishVpnConnection() {
//        try {
//            val builder = Builder()
//
//            // Configure the virtual network interface
//            builder.setMtu(VPN_MTU)
//            builder.addAddress(VIRTUAL_IP, VIRTUAL_IP_PREFIX)
//
//            // Capture ALL traffic (IPv4 and IPv6)
//            builder.addRoute("0.0.0.0", 0)  // All IPv4 traffic
//            builder.addRoute("::", 0)       // All IPv6 traffic
//
//            // Add DNS servers
//            builder.addDnsServer("8.8.8.8")
//            builder.addDnsServer("8.8.4.4")
//
//            // Set to blocking mode for better performance
//            builder.setBlocking(true)
//
//            // Establish the VPN interface
//            vpnInterface = builder.establish()
//
//            isRunning = true
//
//            // Start packet processing
//            processPackets()
//
//        } catch (e: Exception) {
//            Log.e(TAG, "Failed to establish VPN", e)
//        }
//    }
//
//    private fun processPackets() {
//        val inputStream = FileInputStream(vpnInterface?.fileDescriptor)
//        val outputStream = FileOutputStream(vpnInterface?.fileDescriptor)
//        val packetBuffer = ByteArray(VPN_MTU)
//
//        while (isRunning) {
//            try {
//                // Read a packet from the TUN interface
//                val length = inputStream.read(packetBuffer)
//                if (length > 0) {
//                    // Extract and log packet information
//                    val packetInfo = extractPacketInfo(packetBuffer, length)
//
//                    // Log the information
//                    logPacketInfo(packetInfo)
//
//                    // Forward the packet to its destination
//                    forwardPacket(packetBuffer, length, packetInfo)
//                }
//            } catch (e: IOException) {
//                Log.e(TAG, "Error processing packet", e)
//                break
//            }
//        }
//    }
//
//    private fun extractPacketInfo(packet: ByteArray, length: Int): PacketInfo {
//        // Parse IP header (first byte tells us IP version)
//        val version = (packet[0].toInt() and 0xF0) shr 4
//
//        return when (version) {
//            4 -> parseIPv4Packet(packet, length)
//            6 -> parseIPv6Packet(packet, length)
//            else -> PacketInfo(protocol = "UNKNOWN", destination = "Unknown")
//        }
//    }
//
//    private fun parseIPv4Packet(packet: ByteArray, length: Int): PacketInfo {
//        // IP header structure:
//        // Byte 0: Version and header length
//        // Bytes 12-15: Source IP
//        // Bytes 16-19: Destination IP
//        // Byte 9: Protocol (6=TCP, 17=UDP, 1=ICMP)
//
//        val headerLength = (packet[0].toInt() and 0x0F) * 4
//        val protocol = packet[9].toInt()
//
//        // Extract destination IP
//        val destIp = String.format(
//            "%d.%d.%d.%d",
//            packet[16].toInt() and 0xFF,
//            packet[17].toInt() and 0xFF,
//            packet[18].toInt() and 0xFF,
//            packet[19].toInt() and 0xFF
//        )
//
//        val protocolName = when (protocol) {
//            6 -> "TCP"
//            17 -> "UDP"
//            1 -> "ICMP"
//            else -> "PROTOCOL_$protocol"
//        }
//
//        // If TCP/UDP, extract port from transport layer
//        var port = ""
//        if (protocol == 6 || protocol == 17) {
//            val transportStart = headerLength
//            if (transportStart + 4 <= length) {
//                val sourcePort = ((packet[transportStart].toInt() and 0xFF) shl 8) or
//                        (packet[transportStart + 1].toInt() and 0xFF)
//                val destPort = ((packet[transportStart + 2].toInt() and 0xFF) shl 8) or
//                        (packet[transportStart + 3].toInt() and 0xFF)
//                port = ":$destPort"
//            }
//        }
//
//        return PacketInfo(
//            protocol = protocolName,
//            destination = "$destIp$port",
//            timestamp = System.currentTimeMillis(),
//            packetSize = length
//        )
//    }
//
//    private fun parseIPv6Packet(packet: ByteArray, length: Int): PacketInfo {
//        // IPv6 header structure is different
//        // Next header is at byte 6
//        val nextHeader = packet[6].toInt()
//        val protocolName = when (nextHeader) {
//            6 -> "TCP"
//            17 -> "UDP"
//            58 -> "ICMPv6"
//            else -> "PROTOCOL_$nextHeader"
//        }
//
//        // Destination IP is at bytes 24-39
//        val destIp = StringBuilder()
//        for (i in 24 until 40 step 2) {
//            val part = ((packet[i].toInt() and 0xFF) shl 8) or (packet[i + 1].toInt() and 0xFF)
//            destIp.append(part.toString(16)).append(":")
//        }
//
//        return PacketInfo(
//            protocol = protocolName,
//            destination = destIp.toString().trimEnd(':'),
//            timestamp = System.currentTimeMillis(),
//            packetSize = length
//        )
//    }
//
//    private fun forwardPacket(packet: ByteArray, length: Int, packetInfo: PacketInfo) {
//        // Create a socket to forward the packet
//        // The protect() method prevents this socket from being routed back to our VPN
//        val socket = DatagramSocket()
//        protect(socket)  // CRITICAL: Prevents infinite loop
//
//        try {
//            // Parse destination from packetInfo
//            val destParts = packetInfo.destination.split(":")
//            val destAddress = InetAddress.getByName(destParts[0])
//            val destPort = if (destParts.size > 1) destParts[1].toInt() else 0
//
//            val packetData = packet.copyOf(length)
//
//            when (packetInfo.protocol) {
//                "UDP" -> {
//                    val packetSocket = DatagramSocket()
//                    protect(packetSocket)
//                    val datagram = DatagramPacket(packetData, length, destAddress, destPort)
//                    packetSocket.send(datagram)
//                }
//                "TCP" -> {
//                    // TCP requires connection establishment
//                    // For simplicity, this example focuses on UDP
//                    // Full TCP handling requires managing connections
//                }
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "Failed to forward packet", e)
//        } finally {
//            socket.close()
//        }
//    }
//
//    private fun logPacketInfo(info: PacketInfo) {
//        // Write to log file
//        val logEntry = "${info.timestamp} | ${info.protocol} | ${info.destination} | ${info.packetSize} bytes\n"
//
//        // Append to file
//        try {
//            val file = File(getExternalFilesDir(null), "packet_log.csv")
//            val writer = BufferedWriter(FileWriter(file, true))
//            writer.write(logEntry)
//            writer.close()
//        } catch (e: IOException) {
//            Log.e(TAG, "Failed to write log", e)
//        }
//
//        // Also log to console for debugging
//        Log.d(TAG, "PACKET: $logEntry")
//    }
//
//    override fun onRevoke() {
//        // User disabled the VPN
//        stopMonitoring()
//        super.onRevoke()
//    }
//
//    private fun stopMonitoring() {
//        isRunning = false
//        try {
//            vpnInterface?.close()
//        } catch (e: IOException) {
//            Log.e(TAG, "Error closing VPN interface", e)
//        }
//        vpnInterface = null
//    }
//
//    override fun onDestroy() {
//        stopMonitoring()
//        super.onDestroy()
//    }
//
//    data class PacketInfo(
//        val protocol: String,
//        val destination: String,
//        val timestamp: Long = System.currentTimeMillis(),
//        val packetSize: Int = 0
//    )
//
//    private fun startActivityForResult(intent: Intent, requestCode: Int) {
//        // Implementation depends on your Activity context
//        // Use a PendingIntent or callback to your Activity
//    }
//
//    companion object {
//        private const val TAG = "PacketMonitorService"
//        private const val REQUEST_VPN_PERMISSION = 1000
//    }
//}

fun getVpnEndpointIp(): String? {
    try {
        val process = Runtime.getRuntime().exec("ip route")
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val routes = reader.readLines()
        reader.close()

        // Ищем VPN tun-интерфейс в маршрутах
        val hasTun = routes.any { it.contains("tun") || it.contains("ppp") }
        if (!hasTun) return null

        // Endpoint VPN-сервера — это хост-маршрут (/32),
        // идущий через реальный интерфейс (не tun/ppp)
        for (route in routes) {
            if (route.contains("/32") &&
                !route.contains("tun") &&
                !route.contains("ppp")
            ) {
                // строка вида: "185.x.x.x/32 via 192.168.1.1 dev wlan0"
                return route.split("\\s+".toRegex()).firstOrNull()?.replace("/32", "")
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}
