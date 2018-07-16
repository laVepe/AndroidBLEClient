package com.vepe.bleapp.ui

import android.Manifest
import android.annotation.TargetApi
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView.VERTICAL
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.vepe.bleapp.R
import com.vepe.bleapp.bl.BleDevice
import com.vepe.bleapp.bl.ConnectionStatus
import com.vepe.bleapp.presentation.BleViewModel
import com.vepe.bleapp.ui.adapters.ScanAdapter
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(), ScanAdapter.ScanItemClickListener {

    companion object {
        const val REQUEST_ENABLE_BT = 1
        const val REQUEST_FINE_LOCATION = 2
    }

    private lateinit var viewModel: BleViewModel

    private val scanAdapter = ScanAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewModel = ViewModelProviders.of(this).get(BleViewModel::class.java)

        scanRecycler.layoutManager = LinearLayoutManager(applicationContext)
        val itemDecor = DividerItemDecoration(applicationContext, VERTICAL)
        scanRecycler.addItemDecoration(itemDecor)

        viewModel.getScanResults().observe(this, Observer {
            progressBar.visibility = View.GONE
            scanRecycler.visibility = View.VISIBLE
            Log.i("MainActivity", "scan results returned: ${it?.size}")
            it?.let {
                scanAdapter.submitList(it.values.toList())
                scanRecycler.adapter = scanAdapter
                it.forEach {
                    Log.i("MainActivity", "device: ${it.key}")
                }
            }
        })

        viewModel.getConnectionStatus().observe(this, Observer { it ->
            it?.let {
                if (it.connectionStatus is ConnectionStatus.NotEngaged) {
                    viewModel.getScanResults().value?.get(it.device.address)?.connectionStatus = it.connectionStatus
                    scanAdapter.submitList(viewModel.getScanResults().value?.values?.toList() ?: emptyList())
                } else scanAdapter.submitList(listOf(it))

                if (it.connectionStatus is ConnectionStatus.Error)
                    Toast.makeText(applicationContext, getString(R.string.error_message), Toast.LENGTH_SHORT).show()
            }
        })

        checkIfBleSupportedOnDevice()
    }

    override fun onDisconnectClicked() {
        viewModel.disconnectGattServer(ConnectionStatus.NotEngaged())
    }

    override fun onScanItemClicked(device: BluetoothDevice) {
        Log.i("MainActivity", "Connecting to ${device.address}...")
        scanAdapter.submitList(listOf(BleDevice(device, connectionStatus = ConnectionStatus.Connecting())))
        viewModel.connectDevice(device, applicationContext)
    }

    override fun onSetCharacteristicNotifications(characteristic: BluetoothGattCharacteristic) {
        viewModel.signUpForCharacteristicNotifications(characteristic)
    }

    override fun onCharacteristicIndicate(characteristic: BluetoothGattCharacteristic) {
        viewModel.signUpForCharacteristicIndications(characteristic)
    }

    override fun onReadCharacteristic(characteristic: BluetoothGattCharacteristic) {
        viewModel.readCharacteristic(characteristic)
    }

    override fun onWriteCharacteristic(characteristic: BluetoothGattCharacteristic, value: String) {
        viewModel.writeCharacteristic(characteristic, value)
    }

    private fun checkIfBleSupportedOnDevice() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show()
            return
        }
        if (!hasLocationPermissions()) {
            requestLocationPermission()
        } else setupBle()
    }

    private fun setupBle() {
        viewModel.setBluetoothAdapter((getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter)
        if (viewModel.isBleEnabled()) {
            progressBar.visibility = View.VISIBLE
            scanRecycler.visibility = View.GONE
            viewModel.scan()
        } else {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_ENABLE_BT -> if (resultCode == RESULT_OK) setupBle()
            else Toast.makeText(this, R.string.ble_permission_not_granted, Toast.LENGTH_SHORT).show()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_refresh -> {
                if (hasLocationPermissions()) setupBle()
                else requestLocationPermission()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopScan()
    }

    override fun onStop() {
        viewModel.disconnectGattServer(ConnectionStatus.NotEngaged())
        super.onStop()
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun hasLocationPermissions(): Boolean {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun requestLocationPermission() {
        requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_FINE_LOCATION)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_FINE_LOCATION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) setupBle()
                else Toast.makeText(this, R.string.location_permission_not_granted, Toast.LENGTH_SHORT).show()
                return
            }
        }
    }
}
