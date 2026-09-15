package me.zolotov.kodepoint.generator.code

import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import me.zolotov.kodepoint.generator.RangeValue

internal fun encodeIntArrayAsString16(data: IntArray): String {
    return buildString {
        for ((index, value) in data.withIndex()) {
            require(value in 0..0xFFFF) {
                "Value at index $index (${hex(value)}) doesn't fit in 16-bit char (0x0000-0xFFFF)"
            }
            append(value.toChar())
        }
    }
}

internal fun encodeIntArrayAsString8(data: IntArray): String {
    return buildString {
        for (value in data) {
            require(value in 0..255) { "Value $value is outside the valid range 0-255 for 8-bit encoding" }
            append(value.toChar())
        }
    }
}

internal fun hex(value: Int): String {
    return value.toHexString(hexFormat)
}

internal fun rangeTriplets(ranges: List<RangeValue>, hexValue: Boolean): String =
    ranges.flatMap {
        listOf(
            hex(it.startCodepoint),
            hex(it.endCodepoint),
            if (hexValue) hex(it.value) else it.value.toString()
        )
    }.joinToString(", ")

internal fun intInternalConstProperty(name: String, literal: String): PropertySpec =
    PropertySpec.builder(name, Int::class, KModifier.INTERNAL, KModifier.CONST)
        .initializer("%L", literal)
        .build()

private val hexFormat = HexFormat {
    upperCase = true
    number {
        prefix = "0x"
        removeLeadingZeros = true
    }
}

