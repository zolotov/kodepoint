package me.zolotov.kodepoint.generator

import org.jetbrains.amper.plugins.Output
import org.jetbrains.amper.plugins.TaskAction
import java.io.Writer
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.writer

@TaskAction
@OptIn(ExperimentalPathApi::class)
fun generate(@Output generatedSourceDir: Path) {
    println("Running Codepoint Generator...")
    generatedSourceDir.deleteRecursively()

    generateFile(generatedSourceDir.createDirectories().resolve("GeneratedCodepoints.kt")) {
        packageStmt("me.zolotov.kodepoint.generated")
        newLine()
        blockComment(
            """
                 Auto-generated codepoint data from Unicode database
                 Generated at: ${java.time.Instant.now()}
            """.trimIndent()
        )
        obj("HelloWorld") {
            lineComment("The functions prints `Hello World`")
            function("sayHelloWorld()") {
                statement("println(\"Hello World\")")
            }
        }
    }
}

@DslMarker
annotation class Generator

private fun generateFile(outputFile: Path, body: FileGenerator.() -> Unit) {
    outputFile.writer().use { writer ->
        FileGenerator(writer).body()
    }
}

@Generator
private open class TextGenerator(
    private val indent: Int,
    private val writer: Writer
) {
    private val indentString = indent.indent()

    fun blockComment(comment: String) {
        writer.appendLine("$indentString/**")
        comment.lineSequence().forEach { line ->
            writer.appendLine("$indentString * $line")
        }
        writer.appendLine("$indentString */")
    }

    fun lineComment(comment: String) {
        writer.appendLine("$indentString// $comment")
    }

    fun newLine() {
        writer.appendLine("\n")
    }
}

@Generator
private class FileGenerator(private val writer: Writer) : TextGenerator(0, writer) {
    fun packageStmt(packageName: String) {
        writer.appendLine("package $packageName")
    }

    fun obj(name: String, body: ObjectGenerator.() -> Unit) {
        writer.appendLine("object $name {")
        ObjectGenerator(1, writer).body()
        writer.appendLine("}")
    }
}

@Generator
private class ObjectGenerator(
    private val indent: Int,
    private val writer: Writer
) : TextGenerator(indent, writer) {
    private val indentString = indent.indent()

    fun function(signature: String, body: BodyGenerator.() -> Unit) {
        writer.appendLine("${indentString}fun $signature {")
        BodyGenerator(indent + 1, writer).body()
        writer.appendLine("$indentString}")
    }
}

@Generator
private class BodyGenerator(private val indent: Int, private val writer: Writer) : TextGenerator(indent, writer) {
    private val indentString = indent.indent()

    fun statement(statement: String) {
        writer.appendLine("$indentString$statement")
    }
}

private fun Int.indent(): String {
    return " ".repeat(this * 2)
}