package com.neppplus.gabozago.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.neppplus.gabozago.R
import com.neppplus.gabozago.databinding.ItemNotificationBinding
import com.neppplus.gabozago.datas.NotificationData

class NotificationAdapter(val mContext: Context, val mList: List<NotificationData>) :
    RecyclerView.Adapter<NotificationAdapter.NotificationAdapterViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NotificationAdapterViewHolder = NotificationAdapterViewHolder(
        DataBindingUtil.inflate(
            parent.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater,
            R.layout.item_notification,
            parent,
            false
        )
    )

    override fun onBindViewHolder(holder: NotificationAdapterViewHolder, position: Int) {
        holder.onBind(mList[position])
    }

    override fun getItemCount(): Int = mList.size

    class NotificationAdapterViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val viewDivisionLine = binding.viewDivisionLine

        fun onBind(item: NotificationData) {
            binding.txtNotificationMessage.text = item.message
            binding.txtNotificationTitle.text = item.title
            binding.txtNotificationUserDate.text = "${item.actUser.nickname} · ${item.getFormattedDateTime()}"

            when (item.type) {
                "약속초대" -> {
                    binding.icInviteAppointment.visibility = View.VISIBLE
                    binding.icRequestFriend.visibility = View.INVISIBLE
                    binding.icEditAppointment.visibility = View.INVISIBLE
                }
                "친구추가요청" -> {
                    binding.icRequestFriend.visibility = View.VISIBLE
                    binding.icInviteAppointment.visibility = View.INVISIBLE
                    binding.icEditAppointment.visibility = View.INVISIBLE
                }
                "약속변경" -> {
                    binding.icEditAppointment.visibility = View.VISIBLE
                    binding.icInviteAppointment.visibility = View.INVISIBLE
                    binding.icRequestFriend.visibility = View.INVISIBLE
                }
            }

            if(!item.isRead){
                binding.layoutNotificationItem.setBackgroundResource(R.color.lavender_25)
            }
        }
    }
}