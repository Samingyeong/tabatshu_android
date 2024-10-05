package com.example.tabatshu_android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class HomeActivity : AppCompatActivity() {
    private val CAMERA_REQUEST_CODE = 101 // 원하는 숫자로 설정할 수 있습니다.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        // 상태바 색상 변경
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = Color.parseColor("#FC9332") // 원하는 색상으로 변경
        }

        // 시스템 UI 플래그 설정 (아이콘 색상 조정)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR // 아이콘을 어두운 색으로 설정
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 자전거 찾기 버튼 참조 추가
        val findBikeButton = findViewById<ImageButton>(R.id.find_bike)
        findBikeButton.setOnClickListener {
            val intent = Intent(this, FindBikeActivity::class.java)
            startActivity(intent)
        }

        // 스캔 버튼 참조 추가
        val findrentbike = findViewById<ImageButton>(R.id.rent_bike)
        findrentbike.setOnClickListener {
            checkCameraPermission() // 권한 확인 후 QR 스캔 시작
        }
    }

    // onActivityResult 메서드 정의
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data) // super 호출
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Scanned: " + result.contents, Toast.LENGTH_LONG).show()
                // QR 코드 내용(result.contents)을 이용한 추가 작업 수행
                val scannedUrl = result.contents
                checkUrlInServer(scannedUrl) // 서버에서 URL 확인
            }
        }
    }

    // 서버에서 URL 확인하기
    private fun checkUrlInServer(url: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = performUrlCheck(url)
            withContext(Dispatchers.Main) {
                when (result) {
                    "success" -> {
                        Toast.makeText(this@HomeActivity, "url 정상", Toast.LENGTH_LONG).show()
                    }
                    "error" -> {
                        Toast.makeText(this@HomeActivity, "url 비정상", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Toast.makeText(this@HomeActivity, "Error checking URL.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // URL을 확인하는 비동기 작업
    private suspend fun performUrlCheck(url: String): String? {
        val serverUrl = "http://192.168.1.115:5000/check_url" // 실제 서버 URL로 변경하세요
        val connection = URL(serverUrl).openConnection() as HttpURLConnection
        val jsonObject = JSONObject()
        jsonObject.put("url", url)

        return try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.write(jsonObject.toString().toByteArray())

            // 응답 코드 체크
            if (connection.responseCode == 200) {
                "success"
            } else {
                "error"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // QR 코드 스캐너 시작
    private fun startQRCodeScanner() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setOrientationLocked(false) // 방향 잠금을 해제하여 수동 조작 가능
        integrator.setBeepEnabled(false) // 비프음 끄기
        integrator.setPrompt("QR코드를 스캔하세요")
        integrator.setCameraId(0)  // 후면 카메라 사용
        integrator.setBarcodeImageEnabled(true)
        integrator.initiateScan()
    }

    // 카메라 권한 확인
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
        } else {
            startQRCodeScanner() // 권한이 있을 경우 QR 스캐너 실행
        }
    }

    // 권한 요청 결과 처리
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == CAMERA_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                startQRCodeScanner()
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
