package me.zolotov.kodepoint.generator

/**
 * Builds lookup tables for Unicode script data.
 */
fun buildScriptTables(scripts: Map<Int, String>): ScriptBuildResult {
    var nextScriptId = 0
    val scriptIds = mutableMapOf<String, Int>()
    scriptIds["Unknown"] = nextScriptId++

    for (cp in 0..MAX_CODEPOINT) {
        val script = scripts[cp] ?: "Unknown"
        if (script !in scriptIds) {
            scriptIds[script] = nextScriptId++
        }
    }

    val scriptNames = scriptIds.entries.sortedBy { it.value }.map { it.key }

    // Build Latin1 script data (0-255)
    val latin1ScriptIds = IntArray(256) { cp ->
        val script = scripts[cp] ?: "Unknown"
        scriptIds[script]!!
    }

    val planeResults = mutableListOf<ScriptPlaneResult>()

    for (plane in PLANES) {
        val paddedSize = 1 shl plane.totalBits

        val scriptData = IntArray(paddedSize) { offset ->
            val cp = plane.startCodepoint + offset
            if (cp <= plane.endCodepoint && cp <= MAX_CODEPOINT) {
                val script = scripts[cp] ?: "Unknown"
                scriptIds[script]!!
            } else {
                0
            }
        }

        if (plane.sparse) {
            val ranges = extractRanges(scriptData)
            planeResults.add(ScriptPlaneResult.Sparse(plane, ranges))
        } else {
            val result = findOptimalLookupTable(scriptData, plane.totalBits, 1)
            planeResults.add(ScriptPlaneResult.Table(plane, result))
        }
    }

    return ScriptBuildResult(
        planeResults = planeResults,
        scriptNames = scriptNames,
        latin1ScriptIds = latin1ScriptIds
    )
}


sealed interface ScriptPlaneResult {
    val plane: PlaneInfo
    val size: Int

    data class Table(override val plane: PlaneInfo, val table: LookupTable) : ScriptPlaneResult {
        override val size: Int = table.totalSize
    }

    data class Sparse(override val plane: PlaneInfo, val ranges: List<RangeValue>) : ScriptPlaneResult {
        override val size: Int = ranges.size * 9  // 4 bytes start + 4 bytes end + 1 byte script id
    }
}

class ScriptBuildResult(
    val planeResults: List<ScriptPlaneResult>,
    val scriptNames: List<String>,
    val latin1ScriptIds: IntArray,
)
