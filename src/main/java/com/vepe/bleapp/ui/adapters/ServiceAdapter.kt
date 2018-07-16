package com.vepe.bleapp.ui.adapters

import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothGattService.SERVICE_TYPE_PRIMARY
import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.vepe.bleapp.bl.GattService
import com.vepe.bleapp.utils.getShortUuid
import com.vepe.bleapp.R
import kotlinx.android.synthetic.main.item_service.view.*


class ServiceAdapter(private val scanListener: ScanAdapter.ScanItemClickListener) : ListAdapter<BluetoothGattService, ServiceAdapter.ViewHolder>(ServiceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(inflater.inflate(R.layout.item_service, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), scanListener)
    }

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        private val uuid: TextView = view.uuid
        private val charRecycler: RecyclerView = view.characteristics

        fun bind(item: BluetoothGattService, scanListener: ScanAdapter.ScanItemClickListener) {
            val typeString =
                    if (item.type == SERVICE_TYPE_PRIMARY) view.context.getString(R.string.service_primary)
                    else view.context.getString(R.string.service_secondary)

            val serviceName = try {
                GattService.getGattServiceName(item.uuid.getShortUuid()) + " - " + item.uuid.getShortUuid()
            } catch (e: IllegalStateException) {
                view.context.getString(R.string.unknown_service, item.uuid.getShortUuid())
            }
            uuid.text = view.context.getString(R.string.two_rows, serviceName, typeString)

            charRecycler.layoutManager = LinearLayoutManager(view.context)
            charRecycler.adapter = CharacteristicsAdapter(scanListener)
            (charRecycler.adapter as? CharacteristicsAdapter)?.submitList(item.characteristics)
        }
    }

    class ServiceDiffCallback : DiffUtil.ItemCallback<BluetoothGattService>() {

        override fun areItemsTheSame(oldItem: BluetoothGattService?, newItem: BluetoothGattService?): Boolean = oldItem?.uuid == newItem?.uuid

        override fun areContentsTheSame(oldItem: BluetoothGattService?, newItem: BluetoothGattService?): Boolean = oldItem == newItem
    }
}