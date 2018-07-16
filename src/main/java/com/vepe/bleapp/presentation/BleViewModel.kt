package com.vepe.bleapp.presentation

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.util.Log
import com.vepe.bleapp.bl.BleDevice
import com.vepe.bleapp.bl.ConnectionStatus
import com.vepe.bleapp.utils.convertToString
import java.io.UnsupportedEncodingException


class BleViewModel : ViewModel() {

    companion object {
        const val TAG = "BleViewModel"
        const val SCAN_PERIOD = 5_000L // scanning for 5 seconds
    }

    private var bleAdapter: BluetoothAdapter? = null

    private val scanResultsObservable: MutableLiveData<HashMap<String, BleDevice>> = MutableLiveData()

    fun getScanResults() = scanResultsObservable

    private val scanResults = hashMapOf<String, BleDevice>()

    private var isScanning = false

    private var isConnected = false

    private var scanHandler: Handler? = null

    private var gatt: BluetoothGatt? = null

    private var connectedDevice: BleDevice? = null

    private val connectionStatusLiveData: MutableLiveData<BleDevice> = MutableLiveData()

    fun getConnectionStatus() = connectionStatusLiveData

    private val gattClientCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (status != BluetoothGatt.GATT_SUCCESS) {
                disconnectGattServer(ConnectionStatus.Error())
                return
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                isConnected = true
                connectedDevice?.let {
                    connectionStatusLiveData.postValue(BleDevice(it.device, it.services, connectionStatus = ConnectionStatus.Success()))
                }
                gatt?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                disconnectGattServer(ConnectionStatus.Error())
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status != BluetoothGatt.GATT_SUCCESS) {
                return
            }
            gatt?.services?.let { services ->
                services.forEach { Log.i("services", "uuid: ${it.uuid}") }
                connectedDevice?.let {
                    connectedDevice = BleDevice(it.device, services, ConnectionStatus.Success())
                    connectionStatusLiveData.postValue(connectedDevice)
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicChanged(gatt, characteristic)
            val messageString: String = try {
                characteristic?.value?.convertToString() ?: ""
            } catch (e: UnsupportedEncodingException) {
                Log.e(TAG, "Unable to convert message bytes to string")
                "Error"
            }
            Log.i(TAG, "characteristic value: $messageString")
            setValueToCharacteristic(characteristic)
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            Log.i(TAG, "onCharacteristicWrite status: $status")
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
            Log.i(TAG, "onCharacteristicRead status: $status, value: ${String(characteristic?.value
                    ?: byteArrayOf(0))}")
            setValueToCharacteristic(characteristic)
        }

        private fun setValueToCharacteristic(characteristic: BluetoothGattCharacteristic?) {
            connectedDevice?.let {
                if (it.services.isEmpty()) {
                    Log.i(TAG, "No services with characteristics found.")
                    return
                }
                it.services.first { it.characteristics.contains(characteristic) }.getCharacteristic(characteristic?.uuid)?.value = characteristic?.value
                connectionStatusLiveData.postValue(BleDevice(it.device, it.services, ConnectionStatus.Success()))
            }

        }
    }

    private val scanCallback = object : ScanCallback() {

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let {
                scanResults.put(it.device.address, BleDevice(it.device))
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.forEach {
                scanResults[it.device.address] = BleDevice(it.device)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.i(TAG, "Scan failed with errorCode $errorCode")
            scanResultsObservable.postValue(hashMapOf())
        }
    }

    fun setBluetoothAdapter(adapter: BluetoothAdapter?) {
        bleAdapter = adapter
    }

    fun isBleEnabled(): Boolean = bleAdapter != null && bleAdapter?.isEnabled == true

    fun scan() {
        val filters = arrayListOf<ScanFilter>()
        val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build()

        bleAdapter?.bluetoothLeScanner?.startScan(filters, settings, scanCallback)

        isScanning = true
        scanHandler = Handler()
        scanHandler?.postDelayed(this::stopScan, SCAN_PERIOD)
    }

    fun stopScan() {
        if (isScanning && bleAdapter != null && (bleAdapter?.isEnabled == true) && bleAdapter?.bluetoothLeScanner != null) {
            bleAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            scanResultsObservable.postValue(scanResults)

            isScanning = false
            scanHandler = null
        }
    }

    fun connectDevice(device: BluetoothDevice, context: Context) {
        connectedDevice = BleDevice(device)
        gatt = device.connectGatt(context, false, gattClientCallback)
    }

    fun disconnectGattServer(status: ConnectionStatus) {
        connectedDevice?.let {
            connectionStatusLiveData.postValue(BleDevice(it.device, it.services, status))
        }
        isConnected = false
        gatt?.disconnect()
        gatt?.close()
    }

    fun signUpForCharacteristicNotifications(characteristic: BluetoothGattCharacteristic) {
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        val initialized = gatt?.setCharacteristicNotification(characteristic, true)
        Log.i(TAG, "Characteristic notifications initialized: $initialized")
    }

    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        val success: Boolean = gatt?.readCharacteristic(characteristic) ?: false
        Log.i(TAG, "read characteristic success: $success")
    }

    fun writeCharacteristic(characteristic: BluetoothGattCharacteristic, value: String) {
        characteristic.setValue(value)
        val success: Boolean = gatt?.writeCharacteristic(characteristic) ?: false
        Log.i(TAG, "write characteristic success: $success")
    }

    fun signUpForCharacteristicIndications(characteristic: BluetoothGattCharacteristic) {
        characteristic.writeType = BluetoothGattCharacteristic.PROPERTY_INDICATE
        val initialized = gatt?.setCharacteristicNotification(characteristic, true)
        Log.i(TAG, "Characteristic indications initialized: $initialized")
    }
}