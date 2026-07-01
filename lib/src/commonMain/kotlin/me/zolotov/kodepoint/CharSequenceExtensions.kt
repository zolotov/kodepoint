package me.zolotov.kodepoint

fun CharSequence.codePointAt(index: Int): Codepoint {
  if (index !in indices) throw IndexOutOfBoundsException("Index out of range: $index, size: $length")

  val firstChar = this[index]
  if (firstChar.isHighSurrogate() && index + 1 < length) {
    val secondChar = this[index + 1]
    if (secondChar.isLowSurrogate()) {
      return Codepoint.fromChars(firstChar, secondChar)
    }
  }
  return Codepoint(firstChar.code)
}

fun CharSequence.codePointBefore(index: Int): Codepoint {
  val startIndex = index - 1
  if (startIndex !in indices) throw IndexOutOfBoundsException("Index out of range: $startIndex, size: $length")

  val secondChar = this[startIndex]
  if (secondChar.isLowSurrogate() && startIndex - 1 >= 0) {
    val firstChar = this[startIndex - 1]
    if (firstChar.isHighSurrogate()) {
      return Codepoint.fromChars(firstChar, secondChar)
    }
  }

  return Codepoint(secondChar.code)
}

inline fun CharSequence.forEachCodepoint(f: (Codepoint) -> Unit) {
    var i = 0
    val len = length
    while (i < len) {
        val c1 = get(i++)
        if (c1.isHighSurrogate() && i < len) {
            val c2 = get(i)
            if (c2.isLowSurrogate()) {
                i++
                f(Codepoint.fromChars(c1, c2))
                continue
            }
        }
        f(Codepoint(c1.code))
    }
}

inline fun CharSequence.forEachCodepointReversed(f: (Codepoint) -> Unit) {
    var i = length - 1
    while (i >= 0) {
        val c2 = get(i--)
        if (c2.isLowSurrogate() && i >= 0) {
            val c1 = get(i)
            if (c1.isHighSurrogate()) {
                i--
                f(Codepoint.fromChars(c1, c2))
                continue
            }
        }
        f(Codepoint(c2.code))
    }
}

fun CharSequence.codepoints(offset: Int, direction: Direction = Direction.FORWARD): Iterator<Codepoint> =
  when (direction) {
    Direction.FORWARD -> ForwardCodepointIterator(this, offset)
    Direction.BACKWARD -> BackwardCodepointIterator(this, offset)
  }

enum class Direction {
  FORWARD,
  BACKWARD,
}

private class ForwardCodepointIterator(
    private val source: CharSequence,
    private var index: Int,
) : Iterator<Codepoint> {
    override fun hasNext(): Boolean = index < source.length

    override fun next(): Codepoint {
        if (!hasNext()) throw NoSuchElementException()

        val firstChar = source[index++]
        if (firstChar.isHighSurrogate() && index < source.length) {
            val secondChar = source[index]
            if (secondChar.isLowSurrogate()) {
                index++
                return Codepoint.fromChars(firstChar, secondChar)
            }
        }
        return Codepoint(firstChar.code)
    }
}

private class BackwardCodepointIterator(
    private val source: CharSequence,
    offset: Int,
) : Iterator<Codepoint> {
    private var index = offset - 1

    override fun hasNext(): Boolean = index >= 0

    override fun next(): Codepoint {
        if (!hasNext()) throw NoSuchElementException()

        val secondChar = source[index--]
        if (secondChar.isLowSurrogate() && index >= 0) {
            val firstChar = source[index]
            if (firstChar.isHighSurrogate()) {
                index--
                return Codepoint.fromChars(firstChar, secondChar)
            }
        }
        return Codepoint(secondChar.code)
    }
}
