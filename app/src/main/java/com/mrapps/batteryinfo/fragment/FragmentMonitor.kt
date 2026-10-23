package com.mrapps.batteryinfo.fragment

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mrapps.batteryinfo.R
import com.mrapps.batteryinfo.databinding.FragmentMonitorBinding
import com.mrapps.batteryinfo.meter.Range
import com.rejowan.chart.data.Entry
import com.rejowan.chart.data.LineData
import com.rejowan.chart.data.LineDataSet
import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class FragmentMonitor : Fragment() {
    private var _binding: FragmentMonitorBinding? = null
    private val binding get() = _binding!!
    private var isReceiverRegistered = false
    var capacity = 0f
    var voltage = 0f
    val handler = Handler(Looper.getMainLooper())
    private var maxPower = 0.00
    private var minPower = 0.00
    private val powerAverageList = mutableListOf<Double>()
    private var count = 0
    private var startCapacity = 0f
    private val rangeVoltage = Range(); private val range2Voltage = Range(); private val range3Voltage = Range()
    private val rangeTemperature = Range(); private val range2Temperature = Range(); private val range3Temperature = Range(); private val range4Temperature = Range()
    var temp = 0.0

    private val batteryReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                if (intent == null || _binding == null) return
                var d = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1).toDouble()
                if (d > 12) d /= 1000.0
                voltage = DecimalFormat("#.##").format(d).toFloatOrNull() ?: 0f
                temp = DecimalFormat("#.##").format(intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1).toDouble() / 10).toDoubleOrNull() ?: 0.0
            } catch (e: Exception) {}
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMonitorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            if (!isReceiverRegistered) {
                requireActivity().registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                isReceiverRegistered = true
            }
            setupBattery(); setupCapacity(); setupTemp(); setupVolt(); setupHalfGaugeTemp(); setupHalfGaugeVolt(); startCapacity()
        } catch (e: Exception) {}
    }

    private fun startCapacity() { try { val bm = requireContext().getSystemService(Context.BATTERY_SERVICE) as BatteryManager; startCapacity = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) / 1000f; binding.startCapacity.text = "$startCapacity mAh" } catch (e: Exception) {} }
    private fun setupHalfGaugeTemp() { try { rangeTemperature.color=requireContext().getColor(R.color.linkColor); rangeTemperature.from=0.0; rangeTemperature.to=20.0; range2Temperature.color=requireContext().getColor(R.color.green); range2Temperature.from=20.0; range2Temperature.to=30.0; range3Temperature.color=requireContext().getColor(R.color.warningColor); range3Temperature.from=30.0; range3Temperature.to=40.0; range4Temperature.color=requireContext().getColor(R.color.red); range4Temperature.from=40.0; range4Temperature.to=50.0; binding.halfGaugeTemp.addRange(rangeTemperature); binding.halfGaugeTemp.addRange(range2Temperature); binding.halfGaugeTemp.addRange(range3Temperature); binding.halfGaugeTemp.addRange(range4Temperature) } catch (e: Exception) {} }
    private fun setupHalfGaugeVolt() { try { rangeVoltage.color=requireContext().getColor(R.color.warningColor); rangeVoltage.from=0.0; rangeVoltage.to=2.5; range2Voltage.color=requireContext().getColor(R.color.green); range2Voltage.from=2.5; range2Voltage.to=4.5; range3Voltage.color=requireContext().getColor(R.color.red); range3Voltage.from=4.5; range3Voltage.to=6.0; binding.halfGaugeVolt.addRange(rangeVoltage); binding.halfGaugeVolt.addRange(range2Voltage); binding.halfGaugeVolt.addRange(range3Voltage) } catch (e: Exception) {} }
    private fun setupBattery() { try { binding.batteryChart.description.isEnabled=false; binding.batteryChart.xAxis.isEnabled=false; binding.batteryChart.axisLeft.isEnabled=false; binding.batteryChart.legend.isEnabled=false; binding.batteryChart.data=LineData() } catch (e: Exception) {} }
    private fun setupCapacity() { try { binding.capacityChart.description.isEnabled=false; binding.capacityChart.xAxis.isEnabled=false; binding.capacityChart.axisLeft.isEnabled=false; binding.capacityChart.legend.isEnabled=false; binding.capacityChart.data=LineData() } catch (e: Exception) {} }
    private fun setupTemp() { try { binding.tempChart.description.isEnabled=false; binding.tempChart.xAxis.isEnabled=false; binding.tempChart.axisLeft.isEnabled=false; binding.tempChart.legend.isEnabled=false; binding.tempChart.data=LineData() } catch (e: Exception) {} }
    private fun setupVolt() { try { binding.voltChart.description.isEnabled=false; binding.voltChart.xAxis.isEnabled=false; binding.voltChart.axisLeft.isEnabled=false; binding.voltChart.legend.isEnabled=false; binding.voltChart.data=LineData() } catch (e: Exception) {} }

    private fun updateBattery() { try { val d=binding.batteryChart.data?:return; var s=d.getDataSetByIndex(0); if(s==null){s=createSet(); d.addDataSet(s)}; d.addEntry(Entry(s.entryCount.toFloat(), getAmperage(requireContext())?.toFloatOrNull()?:0f),0); if(s.entryCount>25)s.removeFirst(); d.notifyDataChanged(); binding.batteryChart.notifyDataSetChanged(); binding.batteryChart.invalidate() } catch (e: Exception) {} }
    private fun updateBatteryCapacity() { try { val bm=requireActivity().getSystemService(Context.BATTERY_SERVICE) as BatteryManager; val cur=bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)/1000f; val d=binding.capacityChart.data?:return; var s=d.getDataSetByIndex(0); if(s==null){s=createSet(); d.addDataSet(s)}; d.addEntry(Entry(s.entryCount.toFloat(), cur),0); if(s.entryCount>25)s.removeFirst(); d.notifyDataChanged(); binding.capacityChart.notifyDataSetChanged(); binding.capacityChart.invalidate() } catch (e: Exception) {} }
    private fun updateTemp() { try { val d=binding.tempChart.data?:return; var s=d.getDataSetByIndex(0); if(s==null){s=createSet(); d.addDataSet(s)}; d.addEntry(Entry(s.entryCount.toFloat(), temp.toFloat()),0); if(s.entryCount>25)s.removeFirst(); d.notifyDataChanged(); binding.tempChart.notifyDataSetChanged(); binding.tempChart.invalidate() } catch (e: Exception) {} }
    private fun updateVolt() { try { val d=binding.voltChart.data?:return; var s=d.getDataSetByIndex(0); if(s==null){s=createSet(); d.addDataSet(s)}; d.addEntry(Entry(s.entryCount.toFloat(), voltage),0); if(s.entryCount>25)s.removeFirst(); d.notifyDataChanged(); binding.voltChart.notifyDataSetChanged(); binding.voltChart.invalidate() } catch (e: Exception) {} }
    private fun createSet(): LineDataSet { val s=LineDataSet(null,""); s.mode=LineDataSet.Mode.HORIZONTAL_BEZIER; s.setDrawCircles(false); s.lineWidth=1.8f; s.color=requireContext().getColor(R.color.colorPrimary); s.setDrawValues(false); return s }

    private fun getAmperage(context: Context): String? { return try { val bm=context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager; var cur=-bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW).toFloat(); if(cur==-2147483648f) return "0"; if(abs(cur/1000)<1.0) cur*=1000; DecimalFormat("#.##").format((cur/1000).toDouble()) } catch (e: Exception) { "0" } }
    @SuppressLint("PrivateApi") fun getBatteryCapacity(context: Context?): Double { return try { val o=Class.forName("com.android.internal.os.PowerProfile").getConstructor(Context::class.java).newInstance(context); Class.forName("com.android.internal.os.PowerProfile").getMethod("getAveragePower", String::class.java).invoke(o, "battery.capacity") as Double } catch (e: Exception) { 4000.0 } }

    fun setupMonitor() {
        try {
            count++
            val amp = getAmperage(requireActivity())?.toFloatOrNull() ?: 0f
            val df = DecimalFormat("#.##")
            if (amp < 0) minPower = min(minPower, amp.toDouble())
            if (amp > 0) maxPower = max(maxPower, amp.toDouble())
            binding.maxInPower.text = "${df.format(maxPower)} mA"
            binding.maxOutPower.text = "${df.format(minPower)} mA"
            if (count % 3 == 0) { count = 0; powerAverageList.clear() } else { powerAverageList.add(amp.toDouble()); binding.averagePower.text = "${df.format(powerAverageList.average())} mA" }
            val totalCapacity = getBatteryCapacity(requireContext())
            val bm = requireActivity().getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val cur = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) / 1000
            binding.endCapacity.text = "$cur mAh"
            binding.totalCapacity.text = "$totalCapacity mAh"
            binding.halfGaugeTemp.value = temp
            binding.halfGaugeVolt.value = voltage.toDouble()
        } catch (e: Exception) {}
    }

    override fun onResume() {
        super.onResume()
        handler.post(object : Runnable {
            override fun run() {
                try { if (_binding==null) return; setupMonitor(); updateBattery(); updateBatteryCapacity(); updateTemp(); updateVolt() } catch (e: Exception) {}
                handler.postDelayed(this, 1000)
            }
        })
        startCapacity()
    }

    override fun onPause() {
        super.onPause()
        try { if (isReceiverRegistered) { requireActivity().unregisterReceiver(batteryReceiver); isReceiverRegistered = false } } catch (e: Exception) {}
        handler.removeCallbacksAndMessages(null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try { if (isReceiverRegistered) { requireActivity().unregisterReceiver(batteryReceiver); isReceiverRegistered = false } } catch (e: Exception) {}
        handler.removeCallbacksAndMessages(null)
        _binding = null
    }
}
