package com.neppplus.gabozago

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.*
import com.naver.maps.map.overlay.InfoWindow
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.PathOverlay
import com.naver.maps.map.util.MarkerIcons
import com.neppplus.gabozago.adapters.AddFriendSpinnerAdapter
import com.neppplus.gabozago.databinding.ActivityEditAppointmentBinding
import com.neppplus.gabozago.datas.*
import com.neppplus.gabozago.services.MyJobService
import com.neppplus.gabozago.utils.ContextUtil
import com.neppplus.gabozago.utils.GlobalData
import com.neppplus.gabozago.utils.SizeUtil
import com.neppplus.gabozago.web.KakaoAPIService
import com.odsay.odsayandroidsdk.API
import com.odsay.odsayandroidsdk.ODsayData
import com.odsay.odsayandroidsdk.ODsayService
import com.odsay.odsayandroidsdk.OnResultCallbackListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class EditAppointmentActivity : BaseActivity() {

    lateinit var binding: ActivityEditAppointmentBinding

    private var mEditMode = false
    private var mAppointmentId = 0

    private val mSelectedDateTime = Calendar.getInstance()
    private val mDateFormat = SimpleDateFormat("yyyy. M. d (E)")
    private val mTimeFormat = SimpleDateFormat("a hh : mm")

    val mFriendList = ArrayList<UserData>()
    lateinit var mAddFriendSpinnerAdapter: AddFriendSpinnerAdapter
    private val mSelectedFriendList = ArrayList<UserData>()

    var mDepartureData = Documents("null", "null", 0.0, 0.0, null)
    var mDestinationData = Documents("null", "null", 0.0, 0.0, null)

    private val mDepartureMarker = Marker()
    private val mDepartureInfoWindow = InfoWindow()

    private val mDestinationMarker = Marker()
    private val mDestinationInfoWindow = InfoWindow()
    val mPath = PathOverlay()

    private var totalTime = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_edit_appointment)

        setupEvents()
        setValues()

    }

    override fun setupEvents() {
        dateSelectButtonClickEvent() // ?????? ??????
        timeSelectButtonClickEvent() // ?????? ??????
        addFriendButtonClickEvent() // ????????? ????????? ?????? ??????
        setDeparture() // ????????? ??????
        setDestination() // ????????? ??????
        saveButtonClickEvent() // ?????? ?????? ?????? ??????

        // ???????????? ??????
        binding.btnClose.setOnClickListener {
            finish()
        }

        // ??????????????? ???????????? ???????????? ??????
        binding.txtScrollHelp.setOnTouchListener { view, motionEvent ->
            binding.scrollview.requestDisallowInterceptTouchEvent(true)
            return@setOnTouchListener false
        }

        // ?????? ?????? ??? ?????? ????????? ????????? ?????????
        binding.edtAppointmentTitle.setOnEditorActionListener { v, actionId, event ->
            var handled = false

            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val inputMethodManager =
                    getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(
                    binding.edtAppointmentTitle.windowToken,
                    0
                )
                handled = true
            }

            handled
        }
    }

    override fun setValues() {
        setDataFromViewAppointmentActivity() // ?????? ??????????????? ?????? ????????? ??????
        getMyFriendListFromServer() // ?????? ?????? ????????????

        mAddFriendSpinnerAdapter =
            AddFriendSpinnerAdapter(mContext, R.layout.item_spinner_friend_list, mFriendList)
        binding.spinnerFriendList.adapter = mAddFriendSpinnerAdapter

    }

    // ?????? ?????? ????????????
    private fun getMyFriendListFromServer() {
        apiService.getRequestFriendList("my").enqueue(object : Callback<BasicResponse> {
            override fun onResponse(call: Call<BasicResponse>, response: Response<BasicResponse>) {
                mFriendList.clear()
                mFriendList.addAll(response.body()!!.data.friends)

                mAddFriendSpinnerAdapter.notifyDataSetChanged()
            }

            override fun onFailure(call: Call<BasicResponse>, t: Throwable) {}
        })
    }

    // ?????? ?????? ??????
    private fun dateSelectButtonClickEvent() {
        binding.txtSelectDate.setOnClickListener {
            val dateSetListener =
                DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
                    mSelectedDateTime.set(year, month, dayOfMonth)
                    binding.txtSelectDate.text = mDateFormat.format(mSelectedDateTime.time)
                }

            val datePicker = DatePickerDialog(
                mContext,
                dateSetListener,
                mSelectedDateTime.get(Calendar.YEAR),
                mSelectedDateTime.get(Calendar.MONTH),
                mSelectedDateTime.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.datePicker.minDate = Calendar.getInstance().timeInMillis
            datePicker.show()
        }
    }

    // ?????? ?????? ??????
    private fun timeSelectButtonClickEvent() {
        binding.txtSelectTime.setOnClickListener {
            val timeSetListener =
                TimePickerDialog.OnTimeSetListener { timePicker, hourOfDay, minute ->
                    mSelectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    mSelectedDateTime.set(Calendar.MINUTE, minute)

                    binding.txtSelectTime.text = mTimeFormat.format(mSelectedDateTime.time)

                    Log.d("TimeTest", Calendar.getInstance().timeInMillis.toString())
                    Log.d("TimeTest2", mSelectedDateTime.timeInMillis.toString())
                }
            val timepicker = TimePickerDialog(
                this,
                timeSetListener,
                mSelectedDateTime.get(Calendar.HOUR_OF_DAY),
                mSelectedDateTime.get(Calendar.MINUTE),
                false
            )
            timepicker.setTitle("?????? ??????")
            timepicker.show()
        }
    }

    // ????????? ?????? ?????? ??? ??????
    private fun addFriendButtonClickEvent() {
        binding.btnAddFriend.setOnClickListener {
            if (mFriendList.isEmpty()) return@setOnClickListener

            val selectedFriend = mFriendList[binding.spinnerFriendList.selectedItemPosition]

            if (mSelectedFriendList.contains(selectedFriend)) {
                Toast.makeText(mContext, "?????? ????????? ???????????????", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            inflateFlowLayoutItem(selectedFriend)
        }
    }

    // ????????? ?????? ?????? ???????????? ??????
    private fun inflateFlowLayoutItem(selectedFriend: UserData) {
        val inflater =
            LayoutInflater.from(mContext).inflate(R.layout.item_add_friend_list, null)
        val layout = inflater.findViewById<LinearLayout>(R.id.linear_layout)
        val txtFriendNickname = inflater.findViewById<TextView>(R.id.txt_friend_nickname)
        val btnDelete = inflater.findViewById<ImageView>(R.id.btn_delete)

        val param = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        param.marginEnd = SizeUtil.dpToPx(mContext, 5f).toInt()
        layout.layoutParams = param

        txtFriendNickname.text = selectedFriend.nickname

        mSelectedFriendList.add(selectedFriend)
        binding.flAddFriend.addView(layout)

        btnDelete.setOnClickListener {
            binding.flAddFriend.removeView(layout)
            mSelectedFriendList.remove(selectedFriend)
        }
    }

    // ????????? ?????? & Map Marker ??????
    private fun setDeparture() {
        binding.viewDeparture.setOnClickListener {
            startForSetDeparture.launch(Intent(this, SetDepartureActivity::class.java))
        }
    }

    private val startForSetDeparture: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {

                // ??? ????????? ???????????? ??????????????? or ???????????? ???????????????
                when (result.data!!.getStringExtra("DepartureType")) {
                    "MyPlaceList" -> {
                        val myData =
                            result.data!!.getSerializableExtra("DepartureData") as PlaceData
                        binding.txtSelectDeparture.text = "????????? | ${myData?.name}"
                        mDepartureData.apply {
                            placeName = myData.name
                            latitude = myData.latitude
                            longitude = myData.longitude
                        }
                    }
                    "SearchList" -> {
                        val myData =
                            result.data!!.getSerializableExtra("SearchPlaceData") as Documents
                        binding.txtSelectDeparture.text = "????????? | ${myData.placeName}"
                        mDepartureData = myData
                    }
                }

                setDepartureMarker()
            }
        }

    // ????????? ??????
    private fun setDepartureMarker() {
        val fm = supportFragmentManager
        val mapFragment = fm.findFragmentById(R.id.fragment_naver_map) as MapFragment?
            ?: MapFragment.newInstance().also {
                fm.beginTransaction().add(R.id.fragment_naver_map, it).commit()
            }
        mapFragment.getMapAsync {
            val departurePosition = LatLng(mDepartureData.latitude, mDepartureData.longitude)

            val cameraUpdate = CameraUpdate.scrollTo(departurePosition)
            it.moveCamera(cameraUpdate)

            mDepartureMarker.position = departurePosition
            mDepartureMarker.map = it

            mDepartureInfoWindow.adapter = object : InfoWindow.DefaultViewAdapter(mContext) {
                override fun getContentView(p0: InfoWindow): View {
                    val view =
                        LayoutInflater.from(mContext).inflate(R.layout.my_custom_info_window, null)
                    val txtPlace = view.findViewById<TextView>(R.id.txt_place)
                    val txtArrivalTime = view.findViewById<TextView>(R.id.txt_arrival_time)

                    txtPlace.text = "?????? | ${mDepartureData.placeName}"
                    txtArrivalTime.text = "-"

                    return view
                }
            }

            mDepartureInfoWindow.open(mDepartureMarker)

            // ???????????? ?????????????????? ??? ????????? ?????? ??????
            if (mDestinationData.placeName != "null") {
                drawStartPlaceToDestination(it)
            }
        }
    }

    // ????????? ?????? & Map Marker ??????
    private fun setDestination() {
        binding.viewDestination.setOnClickListener {
            startForSetDestination.launch(Intent(this, SetDestinationActivity::class.java))
        }
    }

    private val startForSetDestination: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val myData =
                    result.data!!.getSerializableExtra("SearchPlaceData") as Documents
                binding.txtSelectDestination.text = "????????? | ${myData.placeName}"
                mDestinationData = myData

                setDestinationMarker()
            }
        }

    // ????????? ??????
    private fun setDestinationMarker() {
        val fm = supportFragmentManager
        val mapFragment = fm.findFragmentById(R.id.fragment_naver_map) as MapFragment?
            ?: MapFragment.newInstance().also {
                fm.beginTransaction().add(R.id.fragment_naver_map, it).commit()
            }
        mapFragment.getMapAsync {
            val destinationPosition = LatLng(mDestinationData.latitude, mDestinationData.longitude)

            var cameraUpdate = CameraUpdate.scrollTo(destinationPosition)
            it.moveCamera(cameraUpdate)

            mDestinationMarker.position = destinationPosition
            mDestinationMarker.icon = MarkerIcons.BLACK
            mDestinationMarker.iconTintColor = Color.RED
            mDestinationMarker.map = it

            // ????????? ???????????? ( ???????????? ??? ?????? ????????? )
            if (mDepartureData.placeName == "null") {
                mDestinationMarker.position = destinationPosition
                mDestinationMarker.map = it

                mDestinationInfoWindow.adapter = object : InfoWindow.DefaultViewAdapter(mContext) {
                    override fun getContentView(p0: InfoWindow): View {
                        val view =
                            LayoutInflater.from(mContext)
                                .inflate(R.layout.my_custom_info_window, null)
                        val txtPlace = view.findViewById<TextView>(R.id.txt_place)
                        val txtArrivalTime = view.findViewById<TextView>(R.id.txt_arrival_time)

                        txtPlace.text = "????????? | ${mDestinationData.placeName}"
                        txtArrivalTime.text = "-"
                        return view
                    }
                }

                mDestinationInfoWindow.open(mDestinationMarker)
            } else {
                // ????????? ?????????
                drawStartPlaceToDestination(it)
            }
        }
    }

    // ????????? ????????? ???????????? ?????? ( ???????????? ????????? ?????? )
    private fun drawStartPlaceToDestination(naverMap: NaverMap) {
        val points = ArrayList<LatLng>()
        mPath.map = null

        points.add(LatLng(mDepartureData.latitude, mDepartureData.longitude))

        val odsay = ODsayService.init(mContext, getString(R.string.odsay_app_key))
        odsay.requestSearchPubTransPath(
            mDepartureData.longitude.toString(),
            mDepartureData.latitude.toString(),
            mDestinationData.longitude.toString(),
            mDestinationData.latitude.toString(),
            null,
            null,
            null,
            object : OnResultCallbackListener {
                override fun onSuccess(p0: ODsayData?, p1: API?) {
                    val jsonObj = p0!!.json

                    when (jsonObj.names().get(0)) {
                        "result" -> {
                            val resultObj = jsonObj.getJSONObject("result")

                            val pathArr = resultObj.getJSONArray("path")
                            val firstPathObj = pathArr.getJSONObject(0)

                            val infoObj = firstPathObj.getJSONObject("info")
                            totalTime = infoObj.getInt("totalTime")

                            setDestinationInfoWindow("$totalTime ??? ??????")

                            mDestinationInfoWindow.open(mDestinationMarker)

                            val subPathArr = firstPathObj.getJSONArray("subPath")
                            for (i in 0 until subPathArr.length()) {
                                val subPathObj = subPathArr.getJSONObject(i)
                                if (!subPathObj.isNull("passStopList")) {
                                    val passStopListObj = subPathObj.getJSONObject("passStopList")
                                    val stationsArr = passStopListObj.getJSONArray("stations")
                                    for (j in 0 until stationsArr.length()) {
                                        val stationObj = stationsArr.getJSONObject(j)
                                        points.add(
                                            LatLng(
                                                stationObj.getString("y").toDouble(),
                                                stationObj.getString("x").toDouble()
                                            )
                                        )
                                    }
                                }
                            }
                            points.add(
                                LatLng(
                                    mDestinationData.latitude,
                                    mDestinationData.longitude
                                )
                            )

                            mPath.coords = points
                            mPath.map = naverMap
                        }
                        "error" -> {
                            val errorObj = jsonObj.getJSONObject("error")
                            val errorCode = errorObj.getInt("code")
                            if (errorCode == -98) {
                                Toast.makeText(
                                    mContext,
                                    "???????????? ???????????? ????????? 700m ?????????\n???????????? ????????? ???????????? ????????????",
                                    Toast.LENGTH_LONG
                                ).show()

                                setDestinationInfoWindow("-")
                            }
                        }
                    }
                }

                override fun onError(p0: Int, p1: String?, p2: API?) {}
            }
        )
    }

    // ????????? ?????????
    fun setDestinationInfoWindow(value: String) {
        mDestinationInfoWindow.adapter =
            object : InfoWindow.DefaultViewAdapter(mContext) {
                override fun getContentView(p0: InfoWindow): View {
                    val view =
                        LayoutInflater.from(mContext).inflate(R.layout.my_custom_info_window, null)
                    val txtPlace = view.findViewById<TextView>(R.id.txt_place)
                    val txtArrivalTime =
                        view.findViewById<TextView>(R.id.txt_arrival_time)

                    txtPlace.text = "????????? | ${mDestinationData.placeName}"
                    txtArrivalTime.text = value

                    return view
                }
            }

        mDestinationInfoWindow.open(mDestinationMarker)
    }

    // ????????? ????????? ??????id ?????? string ??????
    private fun setFriendListString(): String? {
        val sb = StringBuilder()

        if (mSelectedFriendList.isEmpty()) {
            return null
        } else {
            for (i in mSelectedFriendList) {
                sb.append(i.id).append(",")
            }
            sb.deleteAt(sb.lastIndex)

            return sb.toString()
        }
    }

    // ?????? ?????? ?????? ????????? -> ?????? ?????? API ??????
    private fun saveButtonClickEvent() {

        binding.btnSave.setOnClickListener {
            val inputTitle = binding.edtAppointmentTitle.text.toString()
            val inputDate = SimpleDateFormat("yyyy-MM-dd HH:mm").format(mSelectedDateTime.time)

            if (inputTitle == "") {
                Toast.makeText(mContext, "????????? ??????????????????", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (binding.txtSelectDate.text == "?????? ??????") {
                Toast.makeText(mContext, "????????? ??????????????????", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (binding.txtSelectTime.text == "?????? ??????") {
                Toast.makeText(mContext, "????????? ??????????????????", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (mDepartureData.placeName == "null") {
                Toast.makeText(mContext, "???????????? ??????????????????", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (mDestinationData.placeName == "null") {
                Toast.makeText(mContext, "???????????? ??????????????????", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (Calendar.getInstance().timeInMillis - mSelectedDateTime.timeInMillis > 0) {
                Toast.makeText(mContext, "???????????? ????????? ?????????\n?????????????????? ????????? ???????????????", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            when (mEditMode) {
                // ?????? ??????
                true -> callSaveAPI(
                    apiService.putRequestEditAppointment(
                        mAppointmentId,
                        inputTitle,
                        inputDate,
                        mDepartureData.placeName,
                        mDepartureData.latitude,
                        mDepartureData.longitude,
                        mDestinationData.placeName,
                        mDestinationData.latitude,
                        mDestinationData.longitude,
                        setFriendListString()
                    )
                )
                // ?????? ?????? ??????
                false -> callSaveAPI(
                    apiService.postRequestAddAppointment(
                        inputTitle,
                        inputDate,
                        mDepartureData.placeName,
                        mDepartureData.latitude,
                        mDepartureData.longitude,
                        mDestinationData.placeName,
                        mDestinationData.latitude,
                        mDestinationData.longitude,
                        setFriendListString()
                    )
                )
            }
        }
    }

    // ?????? ????????? ?????? API ??????
    private fun callSaveAPI(apiService: Call<BasicResponse>) {
        apiService.enqueue(object : Callback<BasicResponse> {
            override fun onResponse(
                call: Call<BasicResponse>,
                response: Response<BasicResponse>
            ) {
                val js = getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
                val serviceComponent = ComponentName(mContext, MyJobService::class.java)

                Log.d("EditAppointmentTest1", mSelectedDateTime.time.toString())

                val now = Calendar.getInstance()

                // (???????????? - ???????????? - ???????????? - ????????????) ?????? JobScheduler ??????
                val jobTime = mSelectedDateTime.timeInMillis - (ContextUtil.getMyReadyMinute(
                    applicationContext
                ) * 60 * 1000) - (totalTime * 60 * 1000) - now.timeInMillis

                Log.d("JobTimeTest", jobTime.toString())

                val basicResponse = response.body()!!

                // setMinimumLatency - ?????? ?????? ??? ????????? ??????????????? ?????????
                // setOverrideDeadline - ?????? ????????? ??????????????? ??????
                val jobInfo =
                    JobInfo.Builder(basicResponse.data.appointment.id, serviceComponent)
                        .setMinimumLatency(jobTime)
                        .setOverrideDeadline(TimeUnit.MINUTES.toMillis(5))
                        .build()

                js.schedule(jobInfo)

                if (mEditMode) {
                    setResult(RESULT_OK, intent)
                }
                finish()
            }

            override fun onFailure(call: Call<BasicResponse>, t: Throwable) {}
        })

    }

    // ?????? ??????????????? ?????? ????????? ??????
    private fun setDataFromViewAppointmentActivity() {
        mEditMode = intent.getBooleanExtra("EditMode", false)
        if (mEditMode) {
            binding.txtToolbarTitle.text = "?????? ??????"

            val appointmentData = intent.getSerializableExtra("AppointmentData") as AppointmentData
            mSelectedDateTime.time =
                Date(appointmentData.datetime.time - Calendar.getInstance().timeZone.rawOffset)

            mAppointmentId = appointmentData.id

            mDepartureData.apply {
                placeName = appointmentData.startPlace
                latitude = appointmentData.startlatitude
                longitude = appointmentData.startlongitude

            }
            mDestinationData.apply {
                placeName = appointmentData.place
                latitude = appointmentData.latitude
                longitude = appointmentData.longitude
            }

            binding.edtAppointmentTitle.setText(appointmentData.title)
            binding.txtSelectDate.text = mDateFormat.format(mSelectedDateTime.time)
            binding.txtSelectTime.text = mTimeFormat.format(mSelectedDateTime.time)
            binding.txtSelectDeparture.text = "????????? | ${mDepartureData.placeName}"
            binding.txtSelectDestination.text = "????????? | ${mDestinationData.placeName}"

            mSelectedFriendList.add(GlobalData.loginUser!!)

            for (i in 1 until appointmentData.invitedFriendList.size) {
                inflateFlowLayoutItem(appointmentData.invitedFriendList[i])
            }

            setDepartureMarker()
            setDestinationMarker()
        }
    }
}
