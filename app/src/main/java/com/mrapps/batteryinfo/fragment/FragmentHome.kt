package com.mrapps.batteryinfo.fragment

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mrapps.batteryinfo.R
import com.mrapps.batteryinfo.databinding.FragmentHomeBinding
import com.mrapps.batteryinfo.meter.Range
import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class FragmentHome : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var isReceiverRegistered = false
    val batteryUseList = mutableListOf<Int>()
    var count = 0

    private val batteryReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                if (intent == null || _binding == null) return
                // ... semua kode kamu yang lama, tapi ganti requireContext() jadi context ?: return
                val deviceStatus = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = deviceStatus == BatteryManager.BATTERY_STATUS_CHARGING || deviceStatus == BatteryManager.BATTERY_STATUS_FULL

                binding.chargingStatus.text = if (isCharging) "Charging" else "Discharging"
                
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (scale <= 0) return
                val batteryPct = level / scale.toFloat()
                val batteryPercentage = (batteryPct * 100).toInt()
                binding.batteryPercentage.text = "$batteryPercentage%"

                // voltage
                var d = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1).toDouble()
                if (d > 12) d /= 1000.0
                val df = DecimalFormat("#.##")
                binding.voltage.text = df.format(d) + "V"

                // temp
                val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1).toDouble()
                binding.temperature.text = df.format(temp / 10) + "°C"

                val capacity = getAmperageSafe() ?: 0f
                val watt = capacity * d / 1000
                binding.watt.text = df.format(watt) + "W"
                binding.technology.text = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion"
                binding.abv?.attachBatteryIntent(intent)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val handler = Handler(Looper.getMainLooper())
    val handler2 = Handler(Looper.getMainLooper())
    val range = Range()
    val range2 = Range()
    var maxPower = 0.00
    var minPower = 0.00

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            if (!isReceiverRegistered) {
                requireActivity().registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                isReceiverRegistered = true
            }
            setupHalfGauge()
            setupOnClicks()
            setupBatteryInfo()
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun setupHalfGauge() {
        try {
            range.color = requireContext().getColor(R.color.red)
            range.from = -3000.00; range.to = 0.00
            range2.color = requireContext().getColor(R.color.green)
            range2.from = 1.00; range2.to = 3000.00
            binding.halfGauge.addRange(range)
            binding.halfGauge.addRange(range2)
            binding.halfGauge.setNeedleColor(requireContext().getColor(R.color.needleColor))
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun getAmperageSafe(): Float? {
        return try {
            val bm = requireContext().getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            var cur = -bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW).toFloat()
            if (cur == -2147483648f || cur == 0f) return 0f // not supported
            if (abs(cur / 1000) < 1.0) cur *= 1000
            cur / 1000
        } catch (e: Exception) { 0f }
    }

    private fun getAmperage(context: Context): String? {
        return try {
            val v = getAmperageSafe()
            DecimalFormat("#.##").format(v?.toDouble() ?: 0.0)
        } catch (e: Exception) { "0" }
    }
    private fun getAmperageSamsung(context: Context): String? = getAmperage(context)

    private fun setupOnClicks() {
        binding.resetMaxOut.setOnClickListener { minPower = 0.0 }
        binding.resetMaxIn.setOnClickListener { maxPower = 0.0 }
    }

    @SuppressLint("SetTextI18n")
    private fun setupBatteryInfo() {
        try {
            val totalCapacity = getBatteryCapacity(requireActivity()).toInt()
            binding.maxCapacity.text = "$totalCapacity mAh"
            binding.version.text = Build.VERSION.RELEASE
            binding.model.text = Build.MODEL
            binding.manufacture.text = Build.MANUFACTURER
        } catch (e: Exception) { e.printStackTrace() }
    }

    @SuppressLint("PrivateApi")
    fun getBatteryCapacity(context: Context?): Double {
        return try {
            val obj = Class.forName("com.android.internal.os.PowerProfile").getConstructor(Context::class.java).newInstance(context)
            Class.forName("com.android.internal.os.PowerProfile").getMethod("getAveragePower", String::class.java).invoke(obj, "battery.capacity") as Double
        } catch (e: Exception) { 4000.0 }
    }

    override fun onResume() {
        super.onResume()
        handler.post(object : Runnable {
            override fun run() {
                try {
                    if (_binding == null) return
                    val currentValue = getAmperageSafe()?.toDouble() ?: 0.0
                    binding.halfGauge.value = currentValue
                    val df = DecimalFormat("#.##")
                    val amp = getAmperageSafe() ?: 0f
                    if (amp < 0) minPower = min(minPower, amp.toDouble())
                    if (amp > 0) maxPower = max(maxPower, amp.toDouble())
                    binding.maxIn.text = "Max In = " + df.format(maxPower) + " mA"
                    binding.maxOut.text = "Max Out = " + df.format(minPower) + " mA"
                } catch (e: Exception) { e.printStackTrace() }
                handler.postDelayed(this, 500)
            }
        })
    }

    override fun onPause() {
        super.onPause()
        try { if (isReceiverRegistered) { requireActivity().unregisterReceiver(batteryReceiver); isReceiverRegistered = false } } catch (e: Exception) {}
        handler.removeCallbacksAndMessages(null)
        handler2.removeCallbacksAndMessages(null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try { if (isReceiverRegistered) { requireActivity().unregisterReceiver(batteryReceiver); isReceiverRegistered = false } } catch (e: Exception) {}
        handler.removeCallbacksAndMessages(null)
        handler2.removeCallbacksAndMessages(null)
        _binding = null
    }
}
