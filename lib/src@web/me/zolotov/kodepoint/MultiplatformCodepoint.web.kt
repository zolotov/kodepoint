package me.zolotov.kodepoint


internal actual fun codePointsToStringPlatformSpecific(vararg codepoints: Int): String =
    codePointsToStringMultiplatform(*codepoints)

internal actual fun codepointOfPlatformSpecific(highSurrogate: Char, lowSurrogate: Char): Codepoint =
    codepointOfMultiplatform(highSurrogate, lowSurrogate)

internal actual fun highSurrogatePlatformSpecific(codepoint: Int): Char = highSurrogateMultiplatform(codepoint)

internal actual fun lowSurrogatePlatformSpecific(codepoint: Int): Char = lowSurrogateMultiplatform(codepoint)