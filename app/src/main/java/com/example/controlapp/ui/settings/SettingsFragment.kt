package com.example.controlapp.ui.settings

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.RadioButton
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import com.example.controlapp.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    private val permissionsRequestCode = 123 // 원하는 요청 코드로 변경 가능
    private lateinit var notificationManager: NotificationManagerCompat
    private val channelid = "PPL" // 알림 채널 ID
    private val notificationid = 1 // 알림 ID
    private var isSwitchEnabled = false
    private val conditionStatusMap = mutableMapOf<String, Boolean>()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_preferences, rootKey)
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        // 버튼 역할을 하는 Preference에 클릭 리스너 추가
        val buttonPreference = findPreference<Preference>("button_preference")
        buttonPreference?.setOnPreferenceClickListener {
            // 사용자 지정 다이얼로그 표시
            showWifiCredentialsDialog()

            // 초기 상태 설정
            conditionStatusMap["dust_condition"] = false
            conditionStatusMap["weather_condition"] = false
            conditionStatusMap["humidity_condition"] = false
            conditionStatusMap["occupancy_condition"] = false

            true
        }

        // 알림 매니저 초기화
        notificationManager = NotificationManagerCompat.from(requireContext())

        // notification_set SwitchPreference 값 변경 감지
        val switchPreference = findPreference<SwitchPreferenceCompat>("notification_set")
        switchPreference?.setOnPreferenceChangeListener { _, newValue ->
            val isChecked = newValue as Boolean
            isSwitchEnabled = isChecked
            if (isChecked) {
                // 스위치가 켜진 경우, 알림을 보냅니다.
                checkHumanCountAndSendNotification()
            }
            true // 값을 항상 변경 허용
        }

        // Notification Channel 생성 (이미 생성되어 있다면 무시됨)
        createNotificationChannel()

        val dustConditionPreference = findPreference<Preference>("dust_condition")
        dustConditionPreference?.setOnPreferenceClickListener {
            // 미세먼지 조건을 설정하는 팝업 표시
            showDustConditionDialog()
            true
        }
        val weatherConditionPreference = findPreference<Preference>("weather_condition")
        weatherConditionPreference?.setOnPreferenceClickListener {
            // 날씨 조건을 설정하는 팝업 표시
            showWeatherConditionDialog()
            true
        }
        val humidityConditionPreference = findPreference<Preference>("humidity_condition")
        humidityConditionPreference?.setOnPreferenceClickListener {
            // 습도 조건을 설정하는 팝업 표시
            showHumidityConditionDialog()
            true
        }
        val occupancyConditionPreference = findPreference<Preference>("occupancy_condition")
        occupancyConditionPreference?.setOnPreferenceClickListener {
            // 인원 조건을 설정하는 팝업 표시
            showOccupancyConditionDialog()
            true
        }

    }
    private fun showDustConditionDialog() {
        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.dialog_dust_condition, null)

        val goodRadioButton = dialogView.findViewById<RadioButton>(R.id.good)
        val normalRadioButton = dialogView.findViewById<RadioButton>(R.id.normal)
        val badRadioButton = dialogView.findViewById<RadioButton>(R.id.bad)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val isGoodChecked = sharedPreferences.getBoolean("isGoodChecked", false)
        val isNormalChecked = sharedPreferences.getBoolean("isNormalChecked", false)
        val isBadChecked = sharedPreferences.getBoolean("isBadChecked", false)

        goodRadioButton.isChecked = isGoodChecked
        normalRadioButton.isChecked = isNormalChecked
        badRadioButton.isChecked = isBadChecked

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("미세먼지 조건 설정")
        builder.setView(dialogView)
        builder.setPositiveButton("확인") { dialog, _ ->
            val selectedCondition = when {
                goodRadioButton.isChecked -> "미세먼지 좋음"
                normalRadioButton.isChecked -> "미세먼지 보통"
                badRadioButton.isChecked -> "미세먼지 나쁨"
                else -> "미세먼지 조건 미설정"
            }

            // 반환값으로 선택한 미세먼지 조건을 처리
            handleDustCondition(selectedCondition)
            val editor = sharedPreferences.edit()
            editor.putBoolean("isGoodChecked", goodRadioButton.isChecked)
            editor.putBoolean("isNormalChecked", normalRadioButton.isChecked)
            editor.putBoolean("isBadChecked", badRadioButton.isChecked)
            editor.apply()
            dialog.dismiss()
        }
        builder.setNegativeButton("취소") { dialog, _ ->
            dialog.dismiss()
        }

        val alertDialog = builder.create()
        alertDialog.show()
    }


    private fun handleDustCondition(selectedCondition: String) {
        val database = FirebaseDatabase.getInstance()
        val windowControlRef = database.reference.child("data").child("window_control")
        val fineDustRef = database.reference.child("data").child("sesnor_state").child("finedust")
        val autoWindowRef = database.reference.child("data").child("window_control").child("autowindow")

        // data/window_control/autowindow 값 확인
        autoWindowRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(autoWindowSnapshot: DataSnapshot) {
                val autoWindowValue = autoWindowSnapshot.getValue(Int::class.java)

                if (autoWindowValue == 1) {
                    fineDustRef.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(finedustSnapshot: DataSnapshot) {
                            val finedustValue = finedustSnapshot.getValue(String::class.java)?.toInt()

                            val openerValue = when (selectedCondition) {
                                "미세먼지 나쁨" -> if (finedustValue != null && finedustValue >= 101) 11 else 0
                                "미세먼지 보통" -> if (finedustValue != null && finedustValue in 16..100) 11 else 0
                                "미세먼지 좋음" -> if (finedustValue != null && finedustValue in 0..15) 11 else 0
                                else -> 0
                            }

                            // opener 값을 변경
                            windowControlRef.child("opener").setValue(openerValue)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("SettingsFragment", "Firebase onCancelled: $error")
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SettingsFragment", "Firebase onCancelled: $error")
            }
        })
    }

    private fun showWeatherConditionDialog() {
        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.dialog_weather_condition, null)

        val lowTemperatureRadioButton = dialogView.findViewById<RadioButton>(R.id.good)
        val normalTemperatureRadioButton = dialogView.findViewById<RadioButton>(R.id.normal)
        val highTemperatureRadioButton = dialogView.findViewById<RadioButton>(R.id.bad)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val isLowTemperatureChecked = sharedPreferences.getBoolean("isLowTemperatureChecked", false)
        val isNormalTemperatureChecked = sharedPreferences.getBoolean("isNormalTemperatureChecked", false)
        val isHighTemperatureChecked = sharedPreferences.getBoolean("isHighTemperatureChecked", false)

        lowTemperatureRadioButton.isChecked = isLowTemperatureChecked
        normalTemperatureRadioButton.isChecked = isNormalTemperatureChecked
        highTemperatureRadioButton.isChecked = isHighTemperatureChecked

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("온도 조건 설정")
        builder.setView(dialogView)
        builder.setPositiveButton("확인") { dialog, _ ->
            val selectedCondition = when {
                lowTemperatureRadioButton.isChecked -> "온도 낮음"
                normalTemperatureRadioButton.isChecked -> "온도 보통"
                highTemperatureRadioButton.isChecked -> "온도 높음"
                else -> "온도 조건 미설정"
            }

            // 반환값으로 선택한 날씨 조건을 처리
            handleWeatherCondition(selectedCondition)
            val editor = sharedPreferences.edit()
            editor.putBoolean("isLowTemperatureChecked", lowTemperatureRadioButton.isChecked)
            editor.putBoolean("isNormalTemperatureChecked", normalTemperatureRadioButton.isChecked)
            editor.putBoolean("isHighTemperatureChecked", highTemperatureRadioButton.isChecked)
            editor.apply()

            dialog.dismiss()
        }
        builder.setNegativeButton("취소") { dialog, _ ->
            dialog.dismiss()
        }

        val alertDialog = builder.create()
        alertDialog.show()
    }

    private fun handleWeatherCondition(selectedCondition: String) {
        val database = FirebaseDatabase.getInstance()
        val windowControlRef = database.reference.child("data").child("window_control")
        val temperatureRef = database.reference.child("data").child("sesnor_state").child("temperature")
        val autoWindowRef = database.reference.child("data").child("window_control").child("autowindow")

        // data/window_control/autowindow 값 확인
        autoWindowRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(autoWindowSnapshot: DataSnapshot) {
                val autoWindowValue = autoWindowSnapshot.getValue(Int::class.java)

                if (autoWindowValue == 1) {
                    temperatureRef.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(temperatureSnapshot: DataSnapshot) {
                            val temperatureValue = temperatureSnapshot.getValue(String::class.java)?.toInt()

                            val openerValue = when (selectedCondition) {
                                "온도 낮음" -> if (temperatureValue != null && temperatureValue <= 18) 11 else 0
                                "온도 보통" -> if (temperatureValue != null && temperatureValue in 19..28) 11 else 0
                                "온도 높음" -> if (temperatureValue != null && temperatureValue >= 29) 11 else 0
                                else -> 0
                            }

                            // opener 값을 변경금
                            windowControlRef.child("opener").setValue(openerValue)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("SettingsFragment", "Firebase onCancelled: $error")
                        }
                    })
                }
            }


            override fun onCancelled(error: DatabaseError) {
                // 데이터 가져오기가 실패한 경우 처리
                Log.e("SettingsFragment", "Firebase onCancelled: $error")
            }
        })
    }

    private fun showHumidityConditionDialog() {
        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.dialog_humidity_condition, null)

        val lowHumidityRadioButton = dialogView.findViewById<RadioButton>(R.id.good)
        val normalHumidityRadioButton = dialogView.findViewById<RadioButton>(R.id.normal)
        val highHumidityRadioButton = dialogView.findViewById<RadioButton>(R.id.bad)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val isLowHumidityChecked = sharedPreferences.getBoolean("isLowHumidityChecked", false)
        val isNormalHumidityChecked = sharedPreferences.getBoolean("isNormalHumidityChecked", false)
        val isHighHumidityChecked = sharedPreferences.getBoolean("isHighHumidityChecked", false)

        lowHumidityRadioButton.isChecked = isLowHumidityChecked
        normalHumidityRadioButton.isChecked = isNormalHumidityChecked
        highHumidityRadioButton.isChecked = isHighHumidityChecked

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("습도 조건 설정")
        builder.setView(dialogView)
        builder.setPositiveButton("확인") { dialog, _ ->
            val selectedCondition = when {
                lowHumidityRadioButton.isChecked -> "습도 낮음"
                normalHumidityRadioButton.isChecked -> "습도 보통"
                highHumidityRadioButton.isChecked -> "습도 높음"
                else -> "습도 조건 미설정"
            }

            // 반환값으로 선택한 날씨 조건을 처리
            handleHumidityCondition(selectedCondition)
            val editor = sharedPreferences.edit()
            editor.putBoolean("isLowHumidityChecked", lowHumidityRadioButton.isChecked)
            editor.putBoolean("isNormalHumidityChecked", normalHumidityRadioButton.isChecked)
            editor.putBoolean("isHighHumidityChecked", highHumidityRadioButton.isChecked)
            editor.apply()

            dialog.dismiss()
        }
        builder.setNegativeButton("취소") { dialog, _ ->
            dialog.dismiss()
        }

        val alertDialog = builder.create()
        alertDialog.show()
    }

    private fun handleHumidityCondition(selectedCondition: String) {
        val database = FirebaseDatabase.getInstance()
        val windowControlRef = database.reference.child("data").child("window_control")
        val humidityRef = database.reference.child("data").child("sesnor_state").child("humidity")
        val autoWindowRef = database.reference.child("data").child("window_control").child("autowindow")

        // data/window_control/autowindow 값 확인
        autoWindowRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(autoWindowSnapshot: DataSnapshot) {
                val autoWindowValue = autoWindowSnapshot.getValue(Int::class.java)

                if (autoWindowValue == 1) {
                    humidityRef.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(humiditySnapshot: DataSnapshot) {
                            val humidityValue = humiditySnapshot.getValue(String::class.java)?.toInt()

                            val openerValue = when (selectedCondition) {
                                "습도 낮음" -> if (humidityValue != null && humidityValue <= 30) 11 else 0
                                "습도 보통" -> if (humidityValue != null && humidityValue in 31..60) 11 else 0
                                "습도 높음" -> if (humidityValue != null && humidityValue >= 60) 11 else 0
                                else -> 0
                            }

                            // opener 값을 변경
                            windowControlRef.child("opener").setValue(openerValue)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("SettingsFragment", "Firebase onCancelled: $error")
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // 데이터 가져오기가 실패한 경우 처리
                Log.e("SettingsFragment", "Firebase onCancelled: $error")
            }
        })
    }

    private fun showOccupancyConditionDialog() {
        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.dialog_occupancy_condition, null)

        val numberPicker = dialogView.findViewById<NumberPicker>(R.id.npker)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val savedValue = sharedPreferences.getInt("occupancyValue", 0)
        numberPicker.wrapSelectorWheel = false
        numberPicker.minValue = 0 // 최소값 설정
        numberPicker.maxValue = 50 // 최대값 설정
        numberPicker.value = savedValue
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("인원 조건 설정")
        builder.setView(dialogView)
        builder.setPositiveButton("확인") { dialog, _ ->
            val selectedCondition = "재실 인원: ${numberPicker.value}"

            // 반환값으로 선택한 인원 조건을 처리
            handleOccupancyCondition(selectedCondition)
            val editor = sharedPreferences.edit()
            editor.putInt("occupancyValue", numberPicker.value)
            editor.apply()
            dialog.dismiss()
        }
        builder.setNegativeButton("취소") { dialog, _ ->
            dialog.dismiss()
        }

        val alertDialog = builder.create()
        alertDialog.show()
    }

    private fun handleOccupancyCondition(condition: String) {
        val database = FirebaseDatabase.getInstance()
        val windowControlRef = database.reference.child("data").child("window_control")
        val humanCountRef = database.reference.child("data").child("sesnor_state").child("human_count")
        val autoWindowRef = database.reference.child("data").child("window_control").child("autowindow")

        // data/window_control/autowindow 값 확인
        autoWindowRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(autoWindowSnapshot: DataSnapshot) {
                val autoWindowValue = autoWindowSnapshot.getValue(Int::class.java)

                // 값이 1이고, 인원 조건을 적용
                if (autoWindowValue == 1) {
                    humanCountRef.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(humanCountSnapshot: DataSnapshot) {
                            val humanCountValue = humanCountSnapshot.getValue(Int::class.java)

                            // 선택된 인원 조건을 파싱하여 숫자만 추출
                            val selectedOccupancy = condition.split(" ").lastOrNull()?.toIntOrNull()

                            // 인원 조건과 비교하여 opener 값을 설정
                            val openerValue = if (humanCountValue != null && selectedOccupancy != null && humanCountValue < selectedOccupancy) {
                                // 넘버픽커로 설정된 숫자보다 작을 때 opener 값을 11로 설정
                                11
                            } else {
                                0
                            }

                            // opener 값을 변경
                            windowControlRef.child("opener").setValue(openerValue)
                        }


                        override fun onCancelled(error: DatabaseError) {
                            // 데이터 가져오기가 실패한 경우 처리
                            Log.e("SettingsFragment", "Firebase onCancelled: $error")
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // 데이터 가져오기가 실패한 경우 처리
                Log.e("SettingsFragment", "Firebase onCancelled: $error")
            }
        })
    }

    private fun showWifiCredentialsDialog() {
        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.dialog_wifi_credentials, null)
        val database = FirebaseDatabase.getInstance()
        val editTextSSID = dialogView.findViewById<EditText>(R.id.editTextSSID)
        val editTextPassword = dialogView.findViewById<EditText>(R.id.editTextPassword)
        val ssidRef = database.reference.child("data").child("module_enable_state").child("wifi_ssid")
        val passwordRef = database.reference.child("data").child("module_enable_state").child("wifi_password")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Wi-Fi Credentials")
        builder.setView(dialogView)
        builder.setPositiveButton("확인") { dialog, _ ->
            val ssid = editTextSSID.text.toString()
            val password = editTextPassword.text.toString()
            ssidRef.setValue(ssid)
            passwordRef.setValue(password)
            // button_preference의 summary 업데이트
            val buttonPreference = findPreference<Preference>("button_preference")
            buttonPreference?.summary = "SSID: $ssid\nPassword: $password"

            dialog.dismiss()
        }
        builder.setNegativeButton("취소") { dialog, _ ->
            dialog.dismiss()
        }

        val alertDialog = builder.create()
        alertDialog.show()
    }

    private fun checkHumanCountAndSendNotification() {
        val database = FirebaseDatabase.getInstance()
        val humanRef = database.reference.child("data").child("sesnor_state").child("human_count")

        // human_count 값 변경을 감지하는 ValueEventListener
        humanRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val humanCount = dataSnapshot.getValue(Int::class.java)

                if (isSwitchEnabled && humanCount != null && humanCount > 0) {
                    // 스위치가 켜져 있고, human_count 값이 1 이상인 경우에만 알림 권한 확인 후 알림을 보냅니다.
                    checkNotificationPermissionAndSendNotification()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // 데이터 가져오기가 실패한 경우 처리
                Log.e("SettingsFragment", "Firebase onCancelled: $error")
            }
        })
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelid,
                "PPL",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = requireContext().getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun checkNotificationPermissionAndSendNotification() {
        // 알림 권한 확인
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // 알림 메시지 설정
            val notification = NotificationCompat.Builder(requireContext(), channelid)
                .setSmallIcon(R.drawable.windowcloseimg)
                .setContentTitle("침입자 감지!")
                .setContentText("침입자가 감지되었습니다.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .build()

            // 알림을 보냅니다.
            notificationManager.notify(notificationid, notification)
        } else {
            // 알림 권한이 없는 경우 권한 요청
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                permissionsRequestCode
            )
        }
    }

    // 권한 요청 결과 처리
    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionsRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 승인된 경우 알림을 보냅니다.
                checkNotificationPermissionAndSendNotification()
            } else {
                // 권한이 거부된 경우 사용자에게 메시지를 표시하거나 처리할 로직을 추가합니다.
            }
        }
    }
    override fun onResume() {
        super.onResume()
        // 현재 상태에 따라 활성화 또는 비활성화
        updatePreferences()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == "auto_lock_conditions") {
            // auto_lock_conditions가 변경되었을 때, 현재 상태 업데이트
            updatePreferences()
        }
    }

    private fun updatePreferences() {
        val autoLockConditions =
            preferenceManager.sharedPreferences.getStringSet("auto_lock_conditions", emptySet())

        // 모든 조건 비활성화
        conditionStatusMap.keys.forEach { condition ->
            conditionStatusMap[condition] = false
        }

        // 선택된 조건 활성화
        autoLockConditions?.forEach { condition ->
            conditionStatusMap[condition] = true
        }

        // 각 조건에 따라 활성화 또는 비활성화
        findPreference<Preference>("dust_condition")?.isEnabled = conditionStatusMap["dust_condition"] == true
        findPreference<Preference>("weather_condition")?.isEnabled = conditionStatusMap["weather_condition"] == true
        findPreference<Preference>("humidity_condition")?.isEnabled = conditionStatusMap["humidity_condition"] == true
        findPreference<Preference>("occupancy_condition")?.isEnabled = conditionStatusMap["occupancy_condition"] == true
        }
    }
