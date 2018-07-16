package com.vepe.bleapp.bl

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattService


data class BleDevice(val device: BluetoothDevice,
                     val services: List<BluetoothGattService> = emptyList(),
                     var connectionStatus: ConnectionStatus = ConnectionStatus.NotEngaged()
)