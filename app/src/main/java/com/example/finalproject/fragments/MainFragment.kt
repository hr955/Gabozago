package com.example.finalproject.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finalproject.R
import com.example.finalproject.adapters.AppointmentListAdapter
import com.example.finalproject.databinding.FragmentMainBinding
import com.example.finalproject.datas.AppointmentData
import com.example.finalproject.datas.BasicResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainFragment : BaseFragment() {

    lateinit var binding: FragmentMainBinding
    lateinit var mAdapter: AppointmentListAdapter
    val mAppointmentList = ArrayList<AppointmentData>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_main, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setupEvents()
        setValues()
    }

    override fun onResume() {
        super.onResume()

        getAppointmentListFromServer()

    }

    override fun setupEvents() {
    }

    override fun setValues() {
        mAdapter = AppointmentListAdapter(mContext, mAppointmentList)

        binding.rvAppointmentList.apply {
            adapter = mAdapter
            layoutManager =
                LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false)
        }

    }

    // 서버에서 일정 리스트를 받아와 리사이클러뷰에 뿌려주는 함수
    fun getAppointmentListFromServer() {
        apiService.getRequestAppointmentList().enqueue(object : Callback<BasicResponse> {
            override fun onResponse(call: Call<BasicResponse>, response: Response<BasicResponse>) {
                mAppointmentList.clear()
                mAppointmentList.addAll(response.body()!!.data.appointments)
                mAdapter.notifyDataSetChanged()
            }

            override fun onFailure(call: Call<BasicResponse>, t: Throwable) {
            }
        })
    }
}