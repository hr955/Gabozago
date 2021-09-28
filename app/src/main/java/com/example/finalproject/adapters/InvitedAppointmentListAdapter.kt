package com.example.finalproject.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.finalproject.R
import com.example.finalproject.ViewAppointmentDetailActivity
import com.example.finalproject.ViewMapActivity
import com.example.finalproject.databinding.ItemAppointmentListBinding
import com.example.finalproject.datas.AppointmentData

class InvitedAppointmentListAdapter(val mContext: Context, val mList: List<AppointmentData>) :
    RecyclerView.Adapter<InvitedAppointmentListAdapter.InvitedAppointmentListAdapterViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): InvitedAppointmentListAdapterViewHolder = InvitedAppointmentListAdapterViewHolder(
        DataBindingUtil.inflate(
            parent.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater,
            R.layout.item_appointment_list,
            parent,
            false
        )
    )

    override fun onBindViewHolder(holder: InvitedAppointmentListAdapterViewHolder, position: Int) {
        holder.onBind(mList[position])
        holder.onMapClickEvent(mList[position], mContext)
    }

    override fun getItemCount(): Int = mList.size
    class InvitedAppointmentListAdapterViewHolder(private val binding: ItemAppointmentListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun onBind(item: AppointmentData) {
            binding.txtTitle.text = item.title
            binding.txtDate.text = item.getFormattedDateTime()
            binding.txtPlace.text = item.place
        }

        fun onMapClickEvent(item: AppointmentData, context: Context) {
            binding.btnMapDetail.setOnClickListener {
                val myIntent = Intent(context, ViewMapActivity::class.java)
                myIntent.putExtra("AppointmentData", item)
                context.startActivity(myIntent)
            }

            binding.layoutRoot.setOnClickListener {
                val myIntent = Intent(context, ViewAppointmentDetailActivity::class.java)
                myIntent.putExtra("AppointmentData", item)
                context.startActivity(myIntent)
            }
        }
    }
}