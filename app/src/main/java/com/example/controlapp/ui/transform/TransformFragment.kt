package com.example.controlapp.ui.transform


import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.controlapp.R
import com.example.controlapp.databinding.FragmentTransformBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import android.content.Context
import android.content.Intent
import android.net.NetworkCapabilities
import android.os.Handler
import android.provider.Settings
import android.widget.CheckBox
import android.widget.SeekBar


class TransformFragment : Fragment() {

    private var _binding: FragmentTransformBinding? = null
    private val binding get() = _binding!!
    private lateinit var peopleStateTextView: TextView
    private lateinit var autowindowStateTextView: TextView
    private lateinit var winodwlockstatetextview: TextView
    private lateinit var imageView: ImageView
    private lateinit var myRef: DatabaseReference
    private lateinit var humanRef: DatabaseReference
    private lateinit var seekBar: SeekBar
    private lateinit var textView: TextView
    private var autowindowValue = 0
    private var lockvalue = 0
    private var isImageOn = true
    // 클래스 내에서 사용할 이전 open_percent 값을 저장하는 변수
    private var previousOpenPercent: Int = 0
    private fun fetchOpenStateFromFirebase(buttonTextOpen: TextView, imageView: ImageView) {
        val database = FirebaseDatabase.getInstance()
        val openStateRef = database.reference.child("data").child("window_control").child("open_state")
        openStateRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                activity?.runOnUiThread {
                    val openStateString = dataSnapshot.getValue(Int::class.java)

                    // 가져온 openState 값에 따라 텍스트 뷰 내용 설정
                    if (openStateString == 0) {
                        buttonTextOpen.text = getString(R.string.close)
                        imageView.setImageResource(R.drawable.windowcloseimg) // 이미지 변경
                    } else {
                        buttonTextOpen.text = getString(R.string.open)
                        imageView.setImageResource(R.drawable.windowopenimg) // 이미지 변경
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // 데이터 가져오기가 실패한 경우 처리
                Log.e("TransformFragment", "Firebase onCancelled: $error")
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentTransformBinding.inflate(inflater, container, false)
        val root: View = binding.root
        // findViewById 및 초기화
        val wifisettingButton = root.findViewById<Button>(R.id.wifisettingButton)
        val buttonOpen = root.findViewById<Button>(R.id.button_windowopen)
        val buttonClose = root.findViewById<Button>(R.id.button_windowclose)
        val buttonStop = root.findViewById<Button>(R.id.button_windowstop)
        val autowindowbutton = root.findViewById<ImageButton>(R.id.imageButton14)
        val windowlock = root.findViewById<Button>(R.id.bluebttonbg4)
        val checkBox: CheckBox = root.findViewById(R.id.openpercent_onoff)
        imageView = root.findViewById(R.id.lockstateic)
        autowindowStateTextView = root.findViewById(R.id.autowindow_state)
        winodwlockstatetextview = root.findViewById(R.id.lock_state)
        peopleStateTextView = root.findViewById(R.id.people_state)
        seekBar = root.findViewById(R.id.seekBar)
        textView = root.findViewById(R.id.percentageTextView)

        val database = FirebaseDatabase.getInstance()
        myRef = database.reference.child("data").child("window_control")
        humanRef = database.reference.child("data").child("sesnor_state")

        // 퍼센트에 따른 지연 시간을 계산하는 함수
        fun calculateDelayMillisOpen(previousPercent: Int, currentPercent: Int): Long {
            // 퍼센트의 변화에 따라 지연 시간을 계산
            return ((currentPercent - previousPercent) * 100L) // 100ms 단위로 조절 가능
        }

        // 퍼센트에 따른 지연 시간을 계산하는 함수
        fun calculateDelayMillisClose(currentPercent: Int): Long {
            // 예시: 퍼센트가 낮아질수록 유지 시간이 길어짐
            return (currentPercent * 100L) // 100ms 단위로 조절 가능
        }


        // 버튼 클릭 이벤트 핸들러
        buttonOpen.setOnClickListener {
            if (checkBox.isChecked) {
                // Firebase에서 open_percent 값을 가져오기
                val openPercentRef = myRef.child("open_percent")

                // Firebase에서 값을 가져오는 비동기 이벤트 리스너 등록
                openPercentRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        // Firebase에서 얻은 현재 open_percent 값, 기본값은 0
                        val currentPercent = snapshot.getValue(Int::class.java) ?: 0

                        // 특정 퍼센트에 따라 opener를 11로 설정하고 유지하는 시간을 조절
                        val delayMillis = calculateDelayMillisOpen(previousOpenPercent, currentPercent)
                        myRef.child("opener").setValue(11)

                        // 일정 시간 후에 opener를 0으로 설정하는 작업을 예약
                        @Suppress("DEPRECATION")
                        Handler().postDelayed({
                            myRef.child("opener").setValue(0)
                            Log.d("TransformFragment", "일정 시간 후에 opener가 0으로 설정됨")
                        }, delayMillis)

                        // 현재 값을 이전 값으로 업데이트
                        previousOpenPercent = currentPercent
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // 에러 처리 (필요에 따라 구현)
                    }
                })
            } else {
                // 체크박스가 선택되어 있지 않으면 100%로 열기
                myRef.child("open_percent").setValue(100)

                // opener를 11로 설정하고 일정 시간 후에 0으로 설정하는 작업 예약
                val delayMillis = calculateDelayMillisOpen(previousOpenPercent, 100)
                myRef.child("opener").setValue(11)
                @Suppress("DEPRECATION")
                Handler().postDelayed({
                    myRef.child("opener").setValue(0)
                    Log.d("TransformFragment", "일정 시간 후에 opener가 0으로 설정됨")
                }, delayMillis)
            }
        }


        buttonClose.setOnClickListener {
            // Firebase에서 open_percent 값을 가져오기
            val openPercentRef = myRef.child("open_percent")

            openPercentRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val currentPercent = snapshot.getValue(Int::class.java) ?: 0

                    // 특정 퍼센트에 따라 opener를 10으로 설정하고 유지하는 시간을 조절
                    val delayMillis = calculateDelayMillisClose(currentPercent)
                    myRef.child("opener").setValue(10)

                    @Suppress("DEPRECATION")
                    Handler().postDelayed({
                        // 일정 시간 후에 opener를 0으로 설정
                        myRef.child("opener").setValue(0)
                        Log.d("TransformFragment", "일정 시간 후에 opener가 0으로 설정됨")
                    }, delayMillis)

                        // close를 누를 때 open_percent를 0으로 설정
                        myRef.child("open_percent").setValue(0)
                        Log.d("TransformFragment", "close 버튼 클릭 후 open_percent를 0으로 설정")

                        // close 버튼을 누를 때 previousOpenPercent를 초기화
                        previousOpenPercent = 0
                }

                override fun onCancelled(error: DatabaseError) {
                    // 에러 처리 (필요에 따라 구현)
                }
            })
        }

        buttonStop.setOnClickListener {
            myRef.child("opener").setValue(0)
            Log.d("TransformFragment", "Stop button clicked")
        }

        autowindowbutton.setOnClickListener {
            // 현재 autowindowValue를 토글
            autowindowValue = if (autowindowValue == 0) 1 else 0

            // Firebase에 값 업데이트
            myRef.child("autowindow").setValue(autowindowValue)

            // 텍스트 업데이트
            val newText = if (autowindowValue == 1) "ON" else "OFF"
            autowindowStateTextView.text = newText

            Log.d("TransformFragment", "Autowindow button clicked, Value: $autowindowValue")
        }

        windowlock.setOnClickListener {
            lockvalue = if (lockvalue == 0) 1 else 0
            myRef.child("locker").setValue(lockvalue)

            if (lockvalue == 1) {
                imageView.setImageResource(R.drawable.lockicon)
            } else {
                imageView.setImageResource(R.drawable.unlockico)
            }
            isImageOn = !isImageOn
            val newText = if (lockvalue == 1) "잠김" else "열림"
            winodwlockstatetextview.text = newText
            Log.d("TransformFragment", "Window lock button clicked, Value: $lockvalue")
        }

        // Firebase 데이터 변경을 감지하는 리스너 설정
        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                activity?.runOnUiThread {
                    // Firebase에서 값을 가져와서 TextView 업데이트
                    val autowindowState = dataSnapshot.child("autowindow").getValue(Int::class.java)


                    if (autowindowState == 1) {
                        autowindowStateTextView.text = getString(R.string.on)
                    } else if (autowindowState == 0) {
                        autowindowStateTextView.text = getString(R.string.off)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // 에러 처리
            }
        })

        humanRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                activity?.runOnUiThread {
                    val humanCount = dataSnapshot.child("human_count").getValue(Int::class.java)


                    // 화면에 human_count 업데이트
                    val humanCountText = getString(R.string.human_count_template, humanCount)
                    peopleStateTextView.text = humanCountText
                    Log.d("TransformFragment", "Data from Firebase: $humanCount")
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("TransformFragment", "Firebase onCancelled: $error")
            }
        })

        val wifiStateTextView = root.findViewById<TextView>(R.id.wifi_state)

        val connectivityManager =
            requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)

        if (networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            // 연결되어 있을 때
            wifiStateTextView.text = getString(R.string.on)

            // 연결되어 있을 때만 버튼 클릭 이벤트 핸들러 설정
            wifisettingButton.setOnClickListener(null)
        } else {
            // 연결되어 있지 않을 때
            wifiStateTextView.text = getString(R.string.off)

            // Wi-Fi 설정으로 이동하는 버튼 클릭 이벤트 핸들러 설정
            wifisettingButton.setOnClickListener {
                val wifiSettingsIntent = Intent(Settings.ACTION_WIFI_SETTINGS)
                startActivity(wifiSettingsIntent)
            }
        }

        val buttonTextOpen = root.findViewById<TextView>(R.id.buttontext_open)
        val imageView19 = root.findViewById<ImageView>(R.id.imageView19)
        // Firebase에서 데이터 가져오기
        fetchOpenStateFromFirebase(buttonTextOpen, imageView19)

        // Firebase에서 open_percent 값 가져오기
        val openPercentRef = myRef.child("open_percent")
        openPercentRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                activity?.runOnUiThread {
                    val openPercent = dataSnapshot.getValue(Long::class.java) ?: 0

                    // Firebase에서 가져온 값으로 SeekBar와 TextView 업데이트
                    seekBar.progress = openPercent.toInt()
                    textView.text = getString(R.string.open_percent_format, openPercent)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                // 처리 중 에러가 발생한 경우
            }
        })
                // CheckBox 상태 변경 이벤트 핸들러
                checkBox.setOnCheckedChangeListener { _, isChecked ->
                    if (!isChecked) {
                        // CheckBox가 해제되면 SeekBar를 0으로 설정하고 TextView를 숨김
                        seekBar.progress = 0
                        textView.visibility = View.GONE
                    }
                }

                seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                        if (checkBox.isChecked) {
                            // 10% 단위로 조절
                            val adjustedProgress = (progress / 10) * 10
                            seekBar.progress = adjustedProgress

                            // TextView에 표시
                            textView.text = getString(R.string.seekbar_progress_format, adjustedProgress)

                            // TextView 표시
                            textView.visibility = View.VISIBLE
                        } else {
                            // CheckBox가 선택되지 않았으면 드래그를 막고, TextView를 숨김
                            seekBar.progress = 0
                            textView.visibility = View.GONE
                        }
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar) {
                        // 사용자가 SeekBar를 터치하여 드래그 시작할 때 호출
                        if (!checkBox.isChecked) {
                            // CheckBox가 선택되지 않았으면 드래그를 막기 위해 SeekBar의 progress를 초기화
                            seekBar.progress = 0
                        }
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar) {
                        if (checkBox.isChecked) {
                            // 드래그가 완료된 퍼센트 값을 Firebase에 업로드
                            val progress = seekBar.progress
                            val seekBarRef = database.reference.child("data").child("window_control")
                                .child("open_percent")
                            seekBarRef.setValue(progress)
                        }
                    }
                })

        return root
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }



}
