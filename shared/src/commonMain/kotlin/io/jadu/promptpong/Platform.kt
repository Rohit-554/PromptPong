package io.jadu.promptpong

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform