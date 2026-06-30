package me.zolotov.kodepoint

fun <T : Appendable> T.appendCodePoint(codepoint: Int): T {
  if (codepoint ushr 16 == 0) {
    append(codepoint.toChar())
  }
  else {
    append(highSurrogate(codepoint))
    append(lowSurrogate(codepoint))
  }
  return this
}

fun <T : Appendable> T.appendCodePoint(codepoint: Codepoint): T = appendCodePoint(codepoint.codepoint)
