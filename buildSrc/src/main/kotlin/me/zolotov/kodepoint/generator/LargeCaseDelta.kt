package me.zolotov.kodepoint.generator

data class LastCaseDeltaRanges(
    val toLower: List<RangeValue>,
    val toUpper: List<RangeValue>
)

private data class CaseMapping(
    val codepoint: Int,
    val target: Int,
)

fun buildLargeCaseDeltas(characters: Array<CharacterData>, characterProperties: IntArray): LastCaseDeltaRanges {
    val toLower = mutableListOf<CaseMapping>()
    val toUpper = mutableListOf<CaseMapping>()
    characterProperties.forEachIndexed { codepoint, packed ->
        when {
            packed and PropertyPacker.HAS_LARGE_LOWERCASE_DELTA_BIT != 0 -> {
                toLower.add(CaseMapping(codepoint, characters[codepoint].lowerCase))
            }

            packed and PropertyPacker.HAS_LARGE_UPPERCASE_DELTA_BIT != 0 -> {
                toUpper.add(CaseMapping(codepoint, characters[codepoint].upperCase))
            }
        }
    }

    return LastCaseDeltaRanges(
        toLower = convertToRelativeDeltas(toLower),
        toUpper = convertToRelativeDeltas(toUpper)
    )
}

private fun convertToRelativeDeltas(sorted: List<CaseMapping>): List<RangeValue> {
    return buildList {
        var startCp = sorted[0].codepoint
        var prevCp = sorted[0].codepoint
        var prevDelta = sorted[0].target - sorted[0].codepoint

        for (i in 1 until sorted.size) {
            val entry = sorted[i]
            val delta = entry.target - entry.codepoint
            if (entry.codepoint == prevCp + 1 && delta == prevDelta) {
                prevCp = entry.codepoint
            } else {
                add(RangeValue(startCp, prevCp, prevDelta))
                startCp = entry.codepoint
                prevCp = entry.codepoint
                prevDelta = delta
            }
        }
        add(RangeValue(startCp, prevCp, prevDelta))
    }
}

