package com.vepe.bleapp.utils

import java.nio.charset.Charset
import java.util.*


fun UUID.getShortUuid(): String {
    return "0x" + this.toString().substring(4, 8).toUpperCase()
}

fun ByteArray.convertToString(charset: Charset = Charset.defaultCharset()): String {
    return String(this, charset)
}