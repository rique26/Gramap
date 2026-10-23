package com.rique.gramap

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform