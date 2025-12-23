package me.zolotov.kodepoint


internal actual fun codePointsToStringPlatformSpecific(vararg codepoints: Int): String = java.lang.String(codepoints, 0, codepoints.size).toString()

internal actual fun codepointOfPlatformSpecific(highSurrogate: Char, lowSurrogate: Char): Codepoint =
    Codepoint(Character.toCodePoint(highSurrogate, lowSurrogate))

internal actual fun highSurrogatePlatformSpecific(codepoint: Int): Char = Character.highSurrogate(codepoint)

internal actual fun lowSurrogatePlatformSpecific(codepoint: Int): Char = Character.lowSurrogate(codepoint)