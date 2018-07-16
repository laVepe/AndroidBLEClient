package com.vepe.bleapp.ui.adapters

import android.bluetooth.BluetoothGattCharacteristic
import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import com.vepe.bleapp.utils.convertToString
import com.vepe.bleapp.utils.getShortUuid
import com.vepe.bleapp.R
import kotlinx.android.synthetic.main.item_characteristic.view.*
import java.io.UnsupportedEncodingException


class CharacteristicsAdapter(private val scanListener: ScanAdapter.ScanItemClickListener) :
        ListAdapter<BluetoothGattCharacteristic, CharacteristicsAdapter.ViewHolder>(CharacteristicDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(inflater.inflate(R.layout.item_characteristic, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), scanListener)
    }

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        private val characteristic: TextView = view.characteristic
        private val button: ImageButton = view.get_values_button
        private val value: TextView = view.char_value

        fun bind(item: BluetoothGattCharacteristic, scanListener: ScanAdapter.ScanItemClickListener) {
            var uuidString = "\t" + item.uuid.getShortUuid()
            item.descriptors.forEach {
                uuidString += "\n\t\tDescriptors: " + it.uuid.getShortUuid()
            }
            characteristic.text = uuidString

            value.visibility = if (item.value == null) View.GONE else {
                val charValue = try {
                    item.value.convertToString()
                } catch (e: UnsupportedEncodingException) {
                    Log.e("CharacteristicsAdapter", "Unable to convert message bytes to string")
                    "Error"
                }
                value.text = view.context.getString(R.string.value, charValue)
                View.VISIBLE
            }

            val buttonIcon = when (item.properties) {
                BluetoothGattCharacteristic.PROPERTY_NOTIFY -> R.drawable.ic_notifications_off
                BluetoothGattCharacteristic.PROPERTY_INDICATE -> R.drawable.ic_notifications_off
                BluetoothGattCharacteristic.PROPERTY_WRITE -> R.drawable.ic_write
                else -> R.drawable.ic_download
            }
            button.setImageDrawable(view.context.resources.getDrawable(buttonIcon, null))

            button.setOnClickListener {
                Log.i("CharacteristicsAdapter", "characteristic properties: ${item.properties}")
                when (item.properties) {
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY -> scanListener.onSetCharacteristicNotifications(item)
                    BluetoothGattCharacteristic.PROPERTY_READ -> scanListener.onReadCharacteristic(item)
                    BluetoothGattCharacteristic.PROPERTY_WRITE -> scanListener.onWriteCharacteristic(item, (System.currentTimeMillis()/2).toString())
                    BluetoothGattCharacteristic.PROPERTY_INDICATE -> scanListener.onCharacteristicIndicate(item)
                }
            }
        }
    }

    class CharacteristicDiffCallback : DiffUtil.ItemCallback<BluetoothGattCharacteristic>() {

        override fun areItemsTheSame(oldItem: BluetoothGattCharacteristic?, newItem: BluetoothGattCharacteristic?): Boolean = oldItem?.uuid == newItem?.uuid

        override fun areContentsTheSame(oldItem: BluetoothGattCharacteristic?, newItem: BluetoothGattCharacteristic?): Boolean = oldItem == newItem
    }
}