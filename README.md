# 🏠 **집킴이 - 창문 잠금개폐 제어 어플리케이션**  
**집의 보안을 스마트하게 강화하는 IoT 창문 제어 앱**  
스마트폰을 이용해 창문을 원격으로 제어하고, **침입 방지 및 실내 환경 모니터링**까지!  
**집킴이**와 함께라면 언제 어디서나 **안전하고 편리하게 창문을 관리**

---

## 🔍 **주요 기능**  

### 1️⃣ **메인 탭**  
- **현재 창문 개폐 상태 파악**: 실시간 창문 상태 확인  
- **자동 개폐 기능 ON/OFF**: 창문을 자동으로 열고 닫음  
- **창문 개폐도 10% 단위 설정**:  
  - **OPEN** (열기), **STOP** (중간 정지), **CLOSE** (닫기) 기능으로 세밀한 제어 가능  
- **데이터 연결 상태**: 앱과 장치 간 연결 상태 표시  
- **창문 원격 잠금**: 외출 중에도 스마트폰으로 창문 잠금  
- **재실 인원 수 제공**: 실내 인원 수 실시간 제공

| **시작화면** | **스플래시화면** | **홈화면** |
|----------------------------------|----------------------------------|----------------------------------|
|<img width="200" alt="시작화면" src="https://github.com/user-attachments/assets/d5dc3214-f9a0-4343-9717-70605be24d30" />| <img width="200" alt="스플래시" src="https://github.com/user-attachments/assets/fe3c4141-6338-499f-b393-e606c9a8bff9" /> | <img width="400" alt="홈화면" src="https://github.com/user-attachments/assets/94b8e208-03bb-4dd5-99ac-578526688c95" />|

### 2️⃣ **연동 탭**  
- **실내 미세먼지 농도 / 온습도 제공**:  
  - 실내 공기 질과 온도를 쉽게 파악 가능  
- **센서 연동 상태 제공**:  
  - **잠금장치, 인원감지, 개폐장치, 블루투스** 연동 상태 한눈에 확인 가능  
- **블루투스 연결 / WIFI 정보 전송**: 장치와의 연결 상태 및 네트워크 정보 확인 가능
  
| **연동화면** |
|----------------------------------|
| <img width="400" alt="연동화면" src="https://github.com/user-attachments/assets/f042e091-24e8-4199-a15c-602e94bdc047" />|

### 3️⃣ **날씨 탭**  
- **GPS 기반 날씨 정보 제공**:  
  - 실시간 **날씨 정보** 확인 및 **도시 검색** 기능 제공  
- **5일 기상 예보 제공**:  
  - 가까운 미래의 날씨 예보 제공

| **날씨화면** |
|----------------------------------|
| <img width="400" alt="날씨화면" src="https://github.com/user-attachments/assets/de614627-9ff4-48eb-b069-c54d6f340398" />|

### 4️⃣ **설정 탭**  
- **WIFI 설정값 입력**: WIFI 연결 설정  
- **침입자 알림 설정**:  
  - 재실 인원이 변경될 때 사용자에게 푸시 알림 전송  
- **자동 잠금 상세 설정**:  
  - **미세먼지 농도, 온습도, 재실 인원** 등을 기준으로 자동 잠금 설정
    
| **설정화면** |
|----------------------------------|
| <img width="550" alt="설정화면" src="https://github.com/user-attachments/assets/df595653-73a4-49cd-88a4-90cb19a11af0" />|

---

## 🛠 **기술 스택**  
| **Category**            | **Technologies**  |
|-------------------------|-----------------------------------------------|
| **Architecture**        | Google App Architecture, MVVM |
| **Networking**         | Retrofit, OkHttp, Interceptor, Gson          |
| **Data Storage**       | Firebase RealTimebase                         |
| **UI/UX**              | Glide, Material 3                            |
| **Navigation**         | Navigation Graph                             |
| **Bluetooth**          | Bluetooth Low Energy                         |
| **API**                | OpenWeather API                               |
| **Permissions**        | Runtime Permission, Gatt permission         |
| **UI State**           | StateFlow                                  |
| **Concurrency**        | Thread, Coroutine, Flow                     |
| **Data Binding**       | ViewBinding, FlowBinding                    |

---

## 📊 **제품 구성 요소**  

### 1️⃣ **개폐부**  
- **고무 바퀴**와 **스텝모터**를 이용하여 창문을 **개폐**  
- **온습도**와 **미세먼지 센서**를 이용해 실내 환경 정보를 **수집**

| **개폐부** | **개폐부 동작원리** | 
|----------------------------------|----------------------------------|
|![개폐부](https://github.com/user-attachments/assets/e39c9782-7ff8-4547-adcc-75e38de0993c) |<img width="400" alt="image" src="https://github.com/user-attachments/assets/9122fb91-ca5b-4e9c-9aa2-9f2e3a9a8176" /> | 

### 2️⃣ **잠금부**  
- **서보모터**의 각도 변경을 통해 **BRACKET**을 이용해 창문을 **잠금**  
- **마그네틱 센서**를 이용하여 잠금 상태를 **확인**

| **잠금부** | **잠금부 동작원리** | 
|----------------------------------|----------------------------------|
|  ![잠금부](https://github.com/user-attachments/assets/6cb1aaa4-9632-43f9-b649-1bf96b45eab0)|<img width="407" alt="image" src="https://github.com/user-attachments/assets/4d77c86c-db29-44fd-b9cb-b5b0cbbf2d91" /> |

### 3️⃣ **재실 인원 감지부 (센서부)**  
- 각 **센서 값**을 이용하여 **입퇴실 인원**을 실시간으로 **측정**  
- **현관문 앞에 부착**하여 **실내 인원**을 **자동 감지**

| **센서부** | **센서부 동작원리** | 
|----------------------------------|----------------------------------|
| <img width="400" alt="센서부" src="https://github.com/user-attachments/assets/5e9072a5-0398-42a1-81ff-72b567125a56" />|<img width="384" alt="image" src="https://github.com/user-attachments/assets/fa0d7e5c-0311-43a4-9de4-9fb986d68b67" /> |

---

## 🧑‍💻 **어플리케이션 흐름도**  

| **어플리케이션 흐름도** | **동작알고리즘** |
|----------------------------------|----------------------------------|
| <img width="428" alt="어플흐름도" src="https://github.com/user-attachments/assets/3bdee439-3ce4-415a-a22c-62427e628c91" />| <img width="500" alt="동작알고리즘" src="https://github.com/user-attachments/assets/6a4e71fc-3ae8-46a3-b8dd-7cd0a7dc4a4f" /> |
---
