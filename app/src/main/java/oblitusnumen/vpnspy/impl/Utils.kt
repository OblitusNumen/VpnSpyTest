package oblitusnumen.vpnspy.impl

import android.Manifest
import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.FileProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Proxy

fun log(o: Any?) {
    log("calendar", o)
}

fun log(tag: String?, o: Any?) {
    Log.v(tag, o.toString())
}

@RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
fun isVpnActive(context: Context): Boolean {
    val interfaces = NetworkInterface.getNetworkInterfaces()
    for (intf in interfaces) {
        if (!intf.isUp || intf.interfaceAddresses.isEmpty()) continue
        if (intf.name == "tun0" || intf.name == "ppp0") {
            return true
        }
    }
    return false
    val cm = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false

    return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
}
//fun getIpInfo(): String? {
//    val client = OkHttpClient()
//
//    val request = DownloadManager.Request.Builder()
//        .url("https://2ip.io/json")
//        .build()
//
//    return try {
//        client.newCall(request).execute().use { response ->
//            response.body?.string()
//        }
//    } catch (e: Exception) {
//        e.printStackTrace()
//        null
//    }
//}
fun getIpInfo() {
//    val url = URL("https://2ip.io").
//        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
//        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
//        .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
//        .header("Accept-Encoding", "gzip, deflate, br")
//        .header("Connection", "keep-alive")
//        .build()
//
//    val connection = url.openConnection() as HttpURLConnection
//
//    return try {
//        connection.requestMethod = "GET"
//        connection.connectTimeout = 5000
//        connection.readTimeout = 5000
//
//        val reader = BufferedReader(InputStreamReader(connection.inputStream))
//        val response = reader.readText()
//        reader.close()
//
//        response
//    } catch (e: Exception) {
//        e.printStackTrace()
//        null
//    } finally {
//        connection.disconnect()
//    }
//    val client = OkHttpClient()
//
//    val request = Request.Builder()
//        .url("https://2ip.io/")
//        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
//        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
//        .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
//        .header("Accept-Encoding", "gzip, deflate, br")
//        .header("Connection", "keep-alive")
//        .build()
//
//    client.newCall(request).execute().use { response ->
//        log(response.code)
//        log(response.body?.string())
//    }
    val client = OkHttpClient.Builder()
        .proxy(Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", 10808)))
        .build()
//    val client = OkHttpClient()

    val request = Request.Builder()
        .url("https://2ip.io")
        .header("User-Agent", "curl/8.7.1")
        .build()

    log( client.newCall(request).execute().use { response ->
//        log(response.protocol)
//        log(response.headers)
        response.body?.string()?.trim() ?: "unknown"
    })
}



fun installApk(context: Context, apkFile: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        apkFile
    )

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
    }

    context.startActivity(intent)
}

fun installApk(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_ACTIVITY_NEW_TASK
    }

    context.startActivity(intent)
}

fun requestIgnoreBatteryOptimizations(context: Context) {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    context.startActivity(intent)
}


fun getVpnIpAddress(): String? {
    try {
        val networkInterfaces = NetworkInterface.getNetworkInterfaces() ?: return null
        for (ni in networkInterfaces) {
            // VPN interfaces typically have names like tun0, ppp0, pptp0
            if (ni.isUp && (ni.name.startsWith("tun") || ni.name.startsWith("ppp") || ni.name.startsWith("pptp"))) {
                for (addr in ni.inetAddresses) {
                    if (!addr.isLoopbackAddress && addr.hostAddress?.contains(':') == false) {
                        return addr.hostAddress // IPv4
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}


fun getNHopIp(ttl: Int): String? {
    try {
//        val socket = DatagramSocket()
//        socket.soTimeout = 3000
//
//        // TTL = 1: пакет умрёт на первом хопе,
//        // и тот вернёт ICMP Time Exceeded со своим IP
//        socket.setTrafficClass(0)
//        val target = InetAddress.getByName("8.8.8.8")
//
//        // Через рефлексию ставим TTL на UDP-сокет
//        val fd = socket.javaClass.getDeclaredField("impl").apply { isAccessible = true }
//        val impl = fd.get(socket)
//        val setOption = impl.javaClass.getDeclaredMethod(
//            "setOption", Int::class.java, Any::class.java
//        ).apply { isAccessible = true }
//        setOption.invoke(impl, 3 /* IP_TTL */, 1)
//
//        val data = ByteArray(1)
//        socket.send(DatagramPacket(data, data.size, target, 33434))
//
//        // Ждём ICMP-ответ — но DatagramSocket его не получит напрямую.
//        // Поэтому лучше ping:
//        socket.close()

        // Fallback: ping с TTL=1
        val process = Runtime.getRuntime().exec("ping -t $ttl -c 1 -W 2 46.228.120.90")
        val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
        val output = reader.readText()
        reader.close()

        // "From 185.x.x.x icmp_seq=1 Time to live exceeded"
        return output
        val match = Regex("From (\\d+\\.\\d+\\.\\d+\\.\\d+)").find(output)
        return match?.groupValues?.get(1)
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}
