package com.vepe.bleapp.bl


object GattService {
    val services = hashMapOf(
            "0x1800" to "Generic Access",
            "0x1811" to "Alert Notification Service",
            "0x1815" to "Automation IO",
            "0x180F" to "Battery Service",
            "0x1810" to "Blood Pressure",
            "0x181B" to "Body Composition",
            "0x181E" to "Bond Management Service",
            "0x181F" to "Continuous Glucose Monitoring",
            "0x1805" to "Current Time Service",
            "0x1818" to "Cycling Power",
            "0x1816" to "Cycling Speed and Cadence",
            "0x180A" to "Device Information",
            "0x181A" to "Environmental Sensing",
            "0x1826" to "Fitness Machine",
            "0x1801" to "Generic Attribute",
            "0x1808" to "Glucose",
            "0x1809" to "Health Thermometer",
            "0x180D" to "Heart Rate",
            "0x1823" to "HTTP Proxy",
            "0x1812" to "Human Interface Device",
            "0x1802" to "Immediate Alert",
            "0x1821" to "Indoor Positioning",
            "0x1820" to "Internet Protocol Support Service",
            "0x1803" to "Link Loss",
            "0x1819" to "Location and Navigation",
            "0x1827" to "Mesh Provisioning Service",
            "0x1828" to "Mesh Proxy Service",
            "0x1807" to "Next DST Change Service",
            "0x1825" to "Object Transfer Service",
            "0x180E" to "Phone Alert Status Service",
            "0x1822" to "Pulse Oximeter Service",
            "0x1829" to "Reconnection Configuration",
            "0x1806" to "Reference Time Update Service",
            "0x1814" to "Running Speed and Cadence",
            "0x1813" to "Scan Parameters",
            "0x1824" to "Transport Discovery",
            "0x1804" to "Tx Power",
            "0x181C" to "User Data",
            "0x181D" to "Weight Scale"
    )

    fun getGattServiceName(shortUuid: String): String =
            services.get(shortUuid) ?: throw IllegalStateException("Unknown Gatt Service Uuid $shortUuid")
}