// 필요한 패키지 및 클래스 가져오기
package com.example.controlapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.controlapp.data.forecastModels.ForecastData
import com.example.controlapp.databinding.RvItemLayoutBinding
import com.squareup.picasso.Picasso
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

// RecyclerView를 위한 어댑터 클래스 정의
class RvAdapter(private val forecastArray: ArrayList<ForecastData>) : RecyclerView.Adapter<RvAdapter.ViewHolder>() {

    // ViewHolder 클래스 정의
    class ViewHolder(val binding: RvItemLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    // ViewHolder를 생성하고 반환하는 메서드
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(RvItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    // ViewHolder에 데이터를 바인딩하는 메서드
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = forecastArray[position]
        holder.binding.apply {
            // 날씨 아이콘 이미지 URL 생성 및 표시
            val imageIcon = currentItem.weather[0].icon
            val imageUrl = "https://openweathermap.org/img/wn/$imageIcon.png"
            Picasso.get().load(imageUrl).into(imgItem)

            // 온도, 날씨 상태, 시간 정보 표시
            tvItemTemp.text = "${currentItem.main.temp.toInt()} °C"
            tvItemStatus.text = currentItem.weather[0].description
            tvItemTime.text = displayTime(currentItem.dt_txt)
        }
    }

//    // 현재 시각을 가져오는 메서드 추가
//    private fun getCurrentTime(): LocalDateTime {
//        return LocalDateTime.now()
//    }

    private fun displayTime(dtTxt: String): CharSequence? {
        val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val outputFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")

        // 입력 문자열을 LocalDateTime으로 파싱
        val localDateTime = LocalDateTime.parse(dtTxt, inputFormatter)

        // 서버에서 제공하는 시간이 UTC라고 가정하고, 한국 시간으로 변환
        val zonedDateTime = ZonedDateTime.of(localDateTime, ZoneId.of("UTC"))
            .withZoneSameInstant(ZoneId.of("Asia/Seoul"))

        // 변환된 시간을 원하는 형식으로 출력
        return outputFormatter.format(zonedDateTime)
    }

    // 데이터 아이템 개수 반환
    override fun getItemCount(): Int {
        return forecastArray.size
    }
}