package com.example.hostuser

object UserCore {
    init { System.loadLibrary("user_core") }

    @JvmStatic external fun nativeInit(caFilePath: String)
    @JvmStatic external fun nativeConnect(url: String)
}
