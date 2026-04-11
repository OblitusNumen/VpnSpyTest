package oblitusnumen.vpnspy

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import oblitusnumen.vpnspy.impl.*
import oblitusnumen.vpnspy.ui.theme.VPNSpyTheme
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

//        val intent = Intent(this, MyForegroundService::class.java)
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            startForegroundService(intent)
//        } else {
//            startService(intent)
//        }

//        ActivityCompat.requestPermissions(
//            this,
//            arrayOf(Manifest.permission.RECORD_AUDIO),
//            100
//        )


        setContent {
            VPNSpyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LazyColumn(contentPadding = innerPadding) {
                        item {
                            val coroutineScope = rememberCoroutineScope()

                            var vpnOn by remember {
                                mutableStateOf(false)
                            }

                            Row {
                                Text("VPN: ", Modifier.padding(16.dp))

                                Switch(vpnOn, {})
                            }

                            LaunchedEffect(coroutineScope) {
                                coroutineScope.launch {
                                    val threadPoolExecutor = ThreadPoolExecutor(
                                        1, 1, 100000, TimeUnit.SECONDS,
                                        LinkedBlockingQueue()
                                    )
                                    repeat(1) {

                                        log(getVpnIpAddress())
// Использование:
//                                        val vpnIp = getVpnIpAddress()
//                                        if (vpnIp != null) {
//                                            println("VPN активен, IP: $vpnIp")
//                                        } else {
//                                            println("VPN не обнаружен")
//                                        }

                                        threadPoolExecutor.execute {
                                            getIpInfo()

                                            repeat(60) {
                                                log("ttl: ${it + 1}")
                                                log("ttl-hop", getNHopIp(it + 1))
                                            }
//                                            log("from cocoutine: ${}")
                                        }
//                                        threadPoolExecutor.getKeepAliveTime(TimeUnit.SECONDS)

                                        log("vpn on: ${isVpnActive(this@MainActivity)}")
                                        vpnOn = isVpnActive(this@MainActivity)
                                        delay(5000)
                                    }
                                }
                            }
                        }

                        item {
                            val context = LocalContext.current

                            val apkPickerLauncher =
                                rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
                                    uri?.let {
                                        installApk(context, it)
                                    }
                                }

                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Button(onClick = {
//                                        requestIgnoreBatteryOptimizations(this@MainActivity)
//                                        apkPickerLauncher.launch(arrayOf("application/vnd.android.package-archive"))
                                    startRecordingService(this@MainActivity)
                                }) {
                                    Text("Select APK")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    VPNSpyTheme {
        Greeting("Android")
    }
}