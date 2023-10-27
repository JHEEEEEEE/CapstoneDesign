// 필요한 패키지 및 클래스 가져오기
package com.example.controlapp.ui.slideshow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.controlapp.databinding.FragmentSlideshowBinding
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.effort.weatherapp.utils.RetrofitInstance
import com.example.controlapp.R
import com.example.controlapp.adapter.RvAdapter
import com.example.controlapp.data.forecastModels.ForecastData
import com.example.controlapp.databinding.BottomSheetLayoutBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.squareup.picasso.Picasso
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SlideshowFragment : Fragment() {
    // 뷰 바인딩을 위한 변수 선언
    private var _binding: FragmentSlideshowBinding? = null
    private val binding get() = _binding!!

    // 위치 정보 및 날씨 데이터를 처리하기 위한 변수 및 객체 선언
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var city: String = "sejong"
    private lateinit var sheetLayoutBinding: BottomSheetLayoutBinding
    private lateinit var dialog: BottomSheetDialog

    // Fragment의 뷰를 생성하는 메서드
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 뷰 바인딩 초기화
        _binding = FragmentSlideshowBinding.inflate(inflater, container, false)
        val root = binding.root
        return root
    }

    // Fragment의 뷰가 생성된 후 호출되는 메서드
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bottom Sheet를 위한 레이아웃 및 다이얼로그 초기화
        sheetLayoutBinding = BottomSheetLayoutBinding.inflate(layoutInflater)
        dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetTheme)
        dialog.setContentView(sheetLayoutBinding.root)

        // 위치 제공자 클라이언트 초기화
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // 검색창에 대한 이벤트 리스너 설정
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    city = query
                }
                getCurrentWeather(city)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        // 현재 날씨 정보 가져오기
        getCurrentWeather(city)

        // 5일 기상 예보를 보기 위한 다이얼로그 열기
        binding.tvForecast.setOnClickListener {
            openDialog()
        }

        // 현재 위치 기반으로 날씨 정보 가져오기
        binding.tvLocation.setOnClickListener {
            fetchLocation()
        }
    }

    // 5일 기상 예보 다이얼로그 열기
    private fun openDialog() {
        getForecast()

        // RecyclerView 설정
        sheetLayoutBinding.rvForecast.apply {
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(requireContext(), 1, RecyclerView.HORIZONTAL, false)
        }

        // 다이얼로그 애니메이션 설정 및 표시
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.show()
    }

    // 5일 기상 예보 데이터 가져오기
    @SuppressLint("SetTextI18n")
    @OptIn(DelicateCoroutinesApi::class)
    private fun getForecast() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                // Retrofit을 사용하여 API 호출
                val response = RetrofitInstance.api.getForecast(
                    city,
                    "metric",
                    requireContext().getString(R.string.api_key)
                )

                // UI 쓰레드에서 처리
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val data = response.body()!!

                        // 5일 기상 예보 데이터를 RecyclerView에 표시
                        val forecastArray: ArrayList<ForecastData> = data.list as ArrayList<ForecastData>
                        val adapter = RvAdapter(forecastArray)
                        sheetLayoutBinding.rvForecast.adapter = adapter
                        sheetLayoutBinding.tvSheet.text = "5일 기상 예보 in ${data.city.name}"

                    } else {
                        // 처리할 작업이 있다면 여기에 추가하세요.
                        Toast.makeText(
                            requireContext(),
                            "응답이 올바르지 않습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "네트워크 오류 ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: HttpException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "서버 오류 ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // 현재 위치 가져오기
    private fun fetchLocation() {
        // 위치 정보 가져오기
        val task: Task<Location> = fusedLocationProviderClient.lastLocation

        // 위치 권한 확인
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 101
            )
            return
        }

        // 위치 정보가 성공적으로 가져온 경우
        @Suppress("DEPRECATION")
        task.addOnSuccessListener { location ->
            if (location != null) {
                val geocoder = Geocoder(requireContext(), Locale.getDefault())

                // 주소 변환 (API 레벨에 따라 다르게 처리)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(
                        location.latitude,
                        location.longitude,
                        1
                    ) { addresses ->
                        if (addresses.isNotEmpty()) {
                            city = addresses[0].locality
                            getCurrentWeather(city)
                        }
                    }
                } else {
                    val address = geocoder.getFromLocation(
                        location.latitude,
                        location.longitude,
                        1
                    ) as List<Address>
                    if (address.isNotEmpty()) {
                        city = address[0].locality
                        getCurrentWeather(city)
                    }
                }
            } else {
                Log.d("location", "null")
            }
        }
    }

    // 현재 날씨 정보 가져오기
    @SuppressLint("SetTextI18n")
    @OptIn(DelicateCoroutinesApi::class)
    private fun getCurrentWeather(city: String) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                // Retrofit을 사용하여 현재 날씨 API 호출
                val response = RetrofitInstance.api.getCurrentWeather(
                    city,
                    "metric",
                    requireContext().getString(R.string.api_key)
                )

                // UI 쓰레드에서 처리
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val data = response.body()!!

                        // 날씨 아이콘 및 기타 정보 표시
                        val iconId = data.weather[0].icon
                        val imgUrl = "https://openweathermap.org/img/wn/$iconId.png"
                        Picasso.get().load(imgUrl).into(binding.imgWeather)

                        // 기타 정보 설정
                        binding.tvSunset.text = dateFormatConverter(data.sys.sunset.toLong())
                        binding.tvSunrise.text = dateFormatConverter(data.sys.sunrise.toLong())

                        binding.apply {
                            tvStatus.text = data.weather[0].description
                            tvWind.text = "${data.wind.speed} KM/H"
                            tvLocation.text = "${data.name}\n${data.sys.country}"
                            tvTemp.text = "${data.main.temp.toInt()}°C"
                            tvFeelsLike.text = "체감온도:${data.main.feels_like.toInt()}°C"
                            tvHumidity.text = "${data.main.humidity} %"
                            tvPressure.text = "${data.main.pressure} hPa"
                            tvUpdateTime.text = "마지막 업데이트:${
                                dateFormatConverter(
                                    data.dt.toLong()
                                )
                            }"
                        }
                    } else {
                        // 처리할 작업이 있다면 여기에 추가하세요.
                        Toast.makeText(
                            requireContext(),
                            "응답이 올바르지 않습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "네트워크 오류 ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: HttpException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "서버 오류 ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // 날짜 형식 변환 메서드
    private fun dateFormatConverter(date: Long): String {
        return SimpleDateFormat(
            "hh:mm a",
            Locale.KOREAN
        ).format(Date(date * 1000))
    }
}
