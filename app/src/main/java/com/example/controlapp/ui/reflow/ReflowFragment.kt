package com.example.controlapp.ui.reflow


import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.controlapp.R
import com.example.controlapp.databinding.FragmentReflowBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.UUID


class ReflowFragment : Fragment() {
    private var _binding: FragmentReflowBinding? = null
    private val binding get() = _binding!!
    private lateinit var temperatureTextView: TextView
    private lateinit var humidityTextView: TextView
    private lateinit var finedustTextView: TextView
    private lateinit var myRef: DatabaseReference


    // 블루투스 연결
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothGatt: BluetoothGatt? = null
    private var isConnected = false
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                requireActivity().finish()
            }
        }

    // 데이터 전송
    private var dataToSend = "hi"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentReflowBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val stateRef = FirebaseDatabase.getInstance().getReference("data/module_enable_state")
        val database = FirebaseDatabase.getInstance()
        myRef = database.reference.child("data").child("sesnor_state")
        temperatureTextView = root.findViewById(R.id.temperaturetext)
        humidityTextView = root.findViewById(R.id.humiditytext)
        finedustTextView = root.findViewById(R.id.finedusttext)

        val peopleConnectText = root.findViewById<TextView>(R.id.peopleconnecttext)
        val openConnectText = root.findViewById<TextView>(R.id.openconnecttext)
        val lockConnectText = root.findViewById<TextView>(R.id.lockconnecttext)

        var isDataChanged = false // 데이터 변경 여부 플래그
        val timeoutInMillis = 60000 // 1분

        // 센서값 업데이트 함수
        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                activity?.runOnUiThread {
                    // 데이터를 가져와서 TextView에 설정
                    val temperature = dataSnapshot.child("temperature").getValue(String::class.java)
                    val humidity = dataSnapshot.child("humidity").getValue(String::class.java)
                    val finedust = dataSnapshot.child("finedust").getValue(String::class.java)

                    // 리소스 문자열 가져오기
                    val temperatureText = getString(R.string.temperature_template, temperature)
                    val humidityText = getString(R.string.humidity_template, humidity)
                    val finedustText = getString(R.string.finedust_template, finedust)

                    // TextView에 설정
                    temperatureTextView.text = temperatureText
                    humidityTextView.text = humidityText
                    finedustTextView.text = finedustText
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ReflowFragment", "Firebase onCancelled: $error")
            }
        })

        // 텍스트 업데이트 함수
        fun updateTexts(isConnected: Boolean) {
            activity?.runOnUiThread {
                // 연동 여부에 따라 텍스트 업데이트
                peopleConnectText.text = if (isConnected) "연동됨" else "연동 안됨"
                openConnectText.text = if (isConnected) "연동됨" else "연동 안됨"
                lockConnectText.text = if (isConnected) "연동됨" else "연동 안됨"
            }
        }

        val timer = object : CountDownTimer(timeoutInMillis.toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // 1초마다 호출되는 콜백 (아무 동작 필요 없음)
            }

            override fun onFinish() {
                // 타이머 종료 시 호출되는 콜백
                if (!isDataChanged) {
                    // 데이터 변경이 없을 경우
                    updateTexts(false)
                }
            }
        }


        stateRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                activity?.runOnUiThread {
                    // 데이터가 변경되었을 때 호출됩니다.
                    val isPeopleConnected = dataSnapshot.child("humancounter").getValue(Int::class.java) == 1
                    val isLockConnected = dataSnapshot.child("locker").getValue(Int::class.java) == 1
                    val isOpenConnected = dataSnapshot.child("opener").getValue(Int::class.java) == 1

                    isDataChanged = true // 데이터 변경이 있음을 표시

                    // 데이터가 변경되었을 때 텍스트 업데이트
                    peopleConnectText.text = if (isPeopleConnected) "연동됨" else "연동 안됨"
                    openConnectText.text = if (isOpenConnected) "연동됨" else "연동 안됨"
                    lockConnectText.text = if (isLockConnected) "연동됨" else "연동 안됨"

                    // 타이머 재시작
                    timer.cancel()
                    timer.start()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // 데이터 변경 감지 중 오류가 발생한 경우 호출됩니다.
                Log.e("FirebaseDatabase", "Database Error: ${databaseError.message}")
            }
        })


        // 초기에 타이머 시작
        timer.start()

        // 블루투스 연결 및 데이터 전송
        val bluetoothManager =
            requireActivity().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        binding.connectButton.setOnClickListener {
            if (!isConnected) {
                connectToDevice()
            } else {
                disconnectDevice()
            }
        }

        val wifiRef = FirebaseDatabase.getInstance().getReference("data/module_enable_state")

        wifiRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val data1 = dataSnapshot.child("wifi_ssid").getValue(String::class.java)
                val data2 = dataSnapshot.child("wifi_password").getValue(String::class.java)
                dataToSend = "$data1,$data2"
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // 오류 처리
                Log.e("FirebaseDatabase", "Database Error: ${databaseError.message}")
            }})

        binding.sendButton.setOnClickListener {
            if (isConnected) {
                // 연결된 경우 BLE 데이터 전송
                sendBLEData(dataToSend)

                // Firebase의 wifi_stats를 1로 설정
                wifiRef.child("wifi_stats").setValue(1)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("ReflowFragment", "wifi_stats를 1로 설정 성공")
                        } else {
                            Log.e("ReflowFragment", "wifi_stats 설정 실패: ${task.exception}")
                        }
                    }

            } else {
                // 연결되지 않은 경우에 대한 처리
            }
        }
        checkAndRequestPermissions()

        return root
    }

    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
        permissions.forEach { permission ->
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(permission)
                return
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())

    private fun connectToDevice() {
        val wifiRef = FirebaseDatabase.getInstance().getReference("data/module_enable_state")
        // Firebase의 wifi_stats를 0으로 설정
        wifiRef.child("wifi_stats").setValue(0)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // 1초의 지연을 주기
                    handler.postDelayed({
                        // 1초 후에 BLE 연결을 시작
                        val deviceAddress = "D4:D4:DA:4E:B7:4E"
                        val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
                        if (ActivityCompat.checkSelfPermission(
                                requireContext(),
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) != PackageManager.PERMISSION_GRANTED
                        ) return@postDelayed

                        bluetoothGatt = device.connectGatt(requireContext(), false, gattCallback)
                    }, 1000)
                } else {
                    // Firebase의 wifi_state 설정이 실패한 경우 처리
                    Log.e("RelfowFragment", "wifi_state 설정 실패: ${task.exception}")
                }
            }
    }



    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)

            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return
            }

            handler.post {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        isConnected = true
                        binding.connectButton.text = "연결 해제"
                        binding.connectionStatusTextView.text = "연동됨"

                        gatt.discoverServices()
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        isConnected = false
                        binding.connectButton.text = "연결"
                        binding.connectionStatusTextView.text = "연동 안됨"

                        bluetoothGatt?.close()
                    }
                } else {
                    // 추가사항
                }
            }
        }
    }

    private fun disconnectDevice() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) return
        bluetoothGatt?.disconnect()
    }

    private fun sendBLEData(data: String) {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) { return }
        val serviceUUID = "19B10000-E8F2-537E-4F6C-D104768A1214" // 디바이스에서 사용하는 서비스 UUID로 대체하세요.
        val characteristicUUID = "89f61a16-e754-494a-a7bc-1504eee282c0" // 디바이스에서 사용하는 Characteristic UUID로 대체하세요.
        val service = bluetoothGatt?.getService(UUID.fromString(serviceUUID))
        val characteristic = service?.getCharacteristic(UUID.fromString(characteristicUUID))
        val byteData = data.toByteArray()
        if (characteristic != null) {
            bluetoothGatt?.writeCharacteristic(characteristic, byteData, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)

        }
        else{
            // 추가사항
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}