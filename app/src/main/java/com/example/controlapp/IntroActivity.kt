package com.example.controlapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class IntroActivity : AppCompatActivity() {
    private val splashTimeout: Long = 4000 // 4초

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_intro)

        // 3초 후에 메인 액티비티로 이동
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // 인트로 액티비티를 종료하여 뒤로 가기 버튼으로 돌아갈 수 없게 합니다.
        }, splashTimeout)
    }
}