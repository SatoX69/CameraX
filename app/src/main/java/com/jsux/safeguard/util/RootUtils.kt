package com.jsux.safeguard.util

object RootUtils {
    fun execute(command: String) {
        try {
            val process = Runtime.getRuntime().exec("su -c $command")
            process.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

