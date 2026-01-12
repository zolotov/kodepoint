package me.zolotov.kodepoint.generator.dsl

import java.io.Writer

@DslMarker
annotation class KotlinCodeDsl

fun kotlinFile(packageName: String, block: KotlinFileBuilder.() -> Unit): KotlinFileBuilder {
    return KotlinFileBuilder(packageName).apply(block)
}

@KotlinCodeDsl
class KotlinFileBuilder(private val packageName: String) {
    private val imports = mutableListOf<String>()
    private val content = StringBuilder()
    private var kdoc: KdocBuilder? = null

    fun import(vararg packages: String) {
        imports.addAll(packages)
    }

    fun kdoc(block: KdocBuilder.() -> Unit) {
        kdoc = KdocBuilder().apply(block)
    }

    fun objectDeclaration(name: String, internal: Boolean = true, block: ObjectBuilder.() -> Unit) {
        val visibility = if (internal) "internal " else ""
        content.appendLine("${visibility}object $name {")
        ObjectBuilder(content, indent = "    ").apply(block)
        content.appendLine("}")
    }

    fun enumClass(
        name: String,
        internal: Boolean = false,
        annotations: List<String> = emptyList(),
        block: EnumBuilder.() -> Unit
    ) {
        val visibility = if (internal) "internal " else ""
        annotations.forEach { content.appendLine(it) }
        content.appendLine("${visibility}enum class $name {")
        EnumBuilder(content, indent = "    ").apply(block).also { it.finalize() }
        content.appendLine("}")
    }

    fun writeTo(writer: Writer) {
        writer.appendLine("package $packageName")
        writer.appendLine()

        if (imports.isNotEmpty()) {
            imports.sorted().forEach {
                writer.appendLine("import $it")
            }
            writer.appendLine()
        }
        kdoc?.writeTo(writer)
        writer.append(content)
    }
}

@KotlinCodeDsl
class KdocBuilder {
    private val lines = mutableListOf<String>()

    fun line(text: String) {
        lines.add(text)
    }

    fun emptyLine() {
        lines.add("")
    }

    fun multiline(text: String) {
        text.lineSequence().forEach { lines.add(it) }
    }

    fun writeTo(writer: Writer) {
        writer.appendLine("/**")
        lines.forEach { line ->
            if (line.isEmpty()) {
                writer.appendLine(" *")
            } else {
                writer.appendLine(" * $line")
            }
        }
        writer.appendLine(" */")
    }
}

@KotlinCodeDsl
class ObjectBuilder(private val content: StringBuilder, private val indent: String) {
    fun const(name: String, value: Any, private: Boolean) {
        val visibility = if (private) "private " else ""
        content.appendLine("${indent}${visibility}const val $name = $value")
    }

    fun emptyLine() {
        content.appendLine()
    }

    fun stringProperty(
        name: String,
        value: String,
        private: Boolean = true,
        const: Boolean = false,
        chunkSize: Int = 8000
    ) {
        val visibility = if (private) "private " else ""

        // Chunk before escaping to avoid splitting escape sequences
        if (value.length <= chunkSize) {
            val escaped = escapeForKotlin(value)
            val constModifier = if (const) "const " else ""
            content.appendLine("$indent${visibility}${constModifier}val $name = \"$escaped\"")
        } else {
            val chunks = value.chunked(chunkSize)
            content.appendLine("$indent${visibility}val $name: String = buildString {")
            for (chunk in chunks) {
                content.appendLine("$indent    append(\"${escapeForKotlin(chunk)}\")")
            }
            content.appendLine("$indent}")
        }
    }

    fun encodedString16Property(name: String, data: IntArray, private: Boolean = true, chunkSize: Int = 8000) {
        val encoded = buildString {
            for (value in data) {
                append((value and 0xFFFF).toChar())
            }
        }
        stringProperty("${name}_DATA", encoded, private, const = encoded.length <= chunkSize, chunkSize = chunkSize)
    }

    fun encodedString32Property(name: String, data: IntArray, private: Boolean = true, chunkSize: Int = 8000) {
        val encoded = buildString {
            for (value in data) {
                append(((value ushr 16) and 0xFFFF).toChar())
                append((value and 0xFFFF).toChar())
            }
        }
        stringProperty("${name}_DATA", encoded, private, const = encoded.length <= chunkSize, chunkSize = chunkSize)
    }

    fun byteArrayFromStringProperty(name: String, data: IntArray, private: Boolean = true, chunkSize: Int = 8000) {
        val encoded = buildString {
            for (value in data) {
                append((value and 0xFF).toChar())
            }
        }
        val visibility = if (private) "private " else ""

        // Chunk before escaping to avoid splitting escape sequences
        if (encoded.length <= chunkSize) {
            val escaped = escapeForKotlin(encoded)
            content.appendLine("$indent${visibility}val ${name}_DATA: ByteArray = \"$escaped\".let { s -> ByteArray(s.length) { s[it].code.toByte() } }")
        } else {
            val chunks = encoded.chunked(chunkSize)
            content.appendLine("$indent${visibility}val ${name}_DATA: ByteArray = buildString {")
            for (chunk in chunks) {
                content.appendLine("$indent    append(\"${escapeForKotlin(chunk)}\")")
            }
            content.appendLine("$indent}.let { s -> ByteArray(s.length) { s[it].code.toByte() } }")
        }
    }

    fun intArrayProperty(
        name: String,
        values: List<String>,
        private: Boolean = true,
        inline: Boolean = false
    ) {
        val visibility = if (private) "private " else ""
        if (inline || values.size <= 10) {
            content.appendLine("$indent${visibility}val $name = intArrayOf(${values.joinToString(", ")})")
        } else {
            content.appendLine("$indent${visibility}val $name = intArrayOf(")
            content.appendLine("$indent    ${values.joinToString(", ")}")
            content.appendLine("$indent)")
        }
    }

    fun byteArrayProperty(
        name: String,
        values: List<String>,
        private: Boolean = true,
        inline: Boolean = false
    ) {
        val visibility = if (private) "private " else ""
        if (inline || values.size <= 10) {
            content.appendLine("$indent${visibility}val $name = byteArrayOf(${values.joinToString(", ")})")
        } else {
            content.appendLine("$indent${visibility}val $name = byteArrayOf(")
            content.appendLine("$indent    ${values.joinToString(", ")}")
            content.appendLine("$indent)")
        }
    }

    fun function(
        name: String,
        params: List<Pair<String, String>> = emptyList(),
        returnType: String? = null,
        private: Boolean = false,
        block: FunctionBodyBuilder.() -> Unit
    ) {
        val visibility = if (private) "private " else ""
        val paramsStr = params.joinToString(", ") { "${it.first}: ${it.second}" }
        val returnTypeStr = if (returnType != null) ": $returnType" else ""

        content.appendLine("$indent${visibility}fun $name($paramsStr)$returnTypeStr {")
        FunctionBodyBuilder(content, "$indent    ").apply(block)
        content.appendLine("$indent}")
    }

    fun expressionFunction(
        name: String,
        params: List<Pair<String, String>> = emptyList(),
        returnType: String? = null,
        private: Boolean = false,
        expression: String
    ) {
        val visibility = if (private) "private " else ""
        val paramsStr = params.joinToString(", ") { "${it.first}: ${it.second}" }
        val returnTypeStr = if (returnType != null) ": $returnType" else ""

        content.appendLine("$indent${visibility}fun $name($paramsStr)$returnTypeStr = $expression")
    }
}

@KotlinCodeDsl
class FunctionBodyBuilder(private val content: StringBuilder, private val indent: String) {
    fun returnStatement(expression: String) {
        content.appendLine("${indent}return $expression")
    }

    fun returnWhen(subject: String? = null, block: WhenBuilder.() -> Unit) {
        if (subject != null) {
            content.appendLine("${indent}return when ($subject) {")
        } else {
            content.appendLine("${indent}return when {")
        }
        WhenBuilder(content, "$indent    ").apply(block)
        content.appendLine("$indent}")
    }

    fun variable(name: String, value: String, mutable: Boolean = false) {
        val keyword = if (mutable) "var" else "val"
        content.appendLine("$indent$keyword $name = $value")
    }
}

@KotlinCodeDsl
class WhenBuilder(private val content: StringBuilder, private val indent: String) {

    fun branch(condition: String, result: String) {
        content.appendLine("$indent$condition -> $result")
    }

    fun branch(condition: String, block: FunctionBodyBuilder.() -> Unit) {
        content.appendLine("$indent$condition -> {")
        FunctionBodyBuilder(content, "$indent    ").apply(block)
        content.appendLine("$indent}")
    }

    fun elseCase(result: String) {
        content.appendLine("${indent}else -> $result")
    }
}

@KotlinCodeDsl
class EnumBuilder(private val content: StringBuilder, private val indent: String) {
    private val entries = mutableListOf<String>()

    fun entry(name: String, params: String? = null) {
        val entry = if (params != null) "$name($params)" else name
        entries.add(entry)
    }

    fun entries(names: List<String>) {
        entries.addAll(names)
    }

    internal fun finalize() {
        entries.forEachIndexed { index, entry ->
            val separator = if (index == entries.lastIndex) ";" else ","
            content.appendLine("$indent$entry$separator")
        }
    }
}

fun escapeForKotlin(s: String): String = buildString(s.length * 2) {
    for (c in s) {
        when {
            c == '\\' -> append("\\\\")
            c == '"' -> append("\\\"")
            c == '\n' -> append("\\n")
            c == '\r' -> append("\\r")
            c == '\t' -> append("\\t")
            c == '$' -> append("\\$")
            c.code < 32 || c.code == 127 || c.code in 0xD800..0xDFFF ->
                append("\\u${c.code.toString(16).padStart(4, '0')}")
            else -> append(c)
        }
    }
}