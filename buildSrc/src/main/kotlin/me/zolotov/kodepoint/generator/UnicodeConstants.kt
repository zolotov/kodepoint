package me.zolotov.kodepoint.generator

const val UNICODE_VERSION = "16.0.0"
const val UNICODE_BASE_URL = "https://www.unicode.org/Public/$UNICODE_VERSION/ucd/"
const val MAX_CODEPOINT = 0x10FFFF

/**
 * All Unicode planes for generation.
 * Note: BMP starts at 0x100 because Latin1 (0x00-0xFF) is handled by CharacterDataLatin1.
 */
val PLANES = listOf(
    PlaneInfo("BMP", "CharacterDataBMP", startCodepoint = 0x0100, endCodepoint = 0xFFFF, sparse = false),
    PlaneInfo("SMP", "CharacterDataSMP", startCodepoint = 0x10000, endCodepoint = 0x1FFFF, sparse = true),
    PlaneInfo("SIP", "CharacterDataSIP", startCodepoint = 0x20000, endCodepoint = 0x2FFFF, sparse = true),
    PlaneInfo("SSP", "CharacterDataSSP", startCodepoint = 0x30000, endCodepoint = 0x10FFFF, sparse = true)
)

/**
 * General Category codes (5 bits = 31 values).
 */
enum class GeneralCategory(val code: Int, val abbrev: String) {
    Cn(0, "Cn"),   // Unassigned (default)
    Lu(1, "Lu"),   // Uppercase Letter
    Ll(2, "Ll"),   // Lowercase Letter
    Lt(3, "Lt"),   // Titlecase Letter
    Lm(4, "Lm"),   // Modifier Letter
    Lo(5, "Lo"),   // Other Letter
    Mn(6, "Mn"),   // Nonspacing Mark
    Mc(7, "Mc"),   // Spacing Combining Mark
    Me(8, "Me"),   // Enclosing Mark
    Nd(9, "Nd"),   // Decimal Number
    Nl(10, "Nl"),  // Letter Number
    No(11, "No"),  // Other Number
    Pc(12, "Pc"),  // Connector Punctuation
    Pd(13, "Pd"),  // Dash Punctuation
    Ps(14, "Ps"),  // Open Punctuation
    Pe(15, "Pe"),  // Close Punctuation
    Pi(16, "Pi"),  // Initial Punctuation
    Pf(17, "Pf"),  // Final Punctuation
    Po(18, "Po"),  // Other Punctuation
    Sm(19, "Sm"),  // Math Symbol
    Sc(20, "Sc"),  // Currency Symbol
    Sk(21, "Sk"),  // Modifier Symbol
    So(22, "So"),  // Other Symbol
    Zs(23, "Zs"),  // Space Separator
    Zl(24, "Zl"),  // Line Separator
    Zp(25, "Zp"),  // Paragraph Separator
    Cc(26, "Cc"),  // Control
    Cf(27, "Cf"),  // Format
    Cs(28, "Cs"),  // Surrogate
    Co(29, "Co");  // Private Use

    companion object {
        private val byAbbrev = entries.associateBy { it.abbrev }
        fun fromAbbrev(abbrev: String): GeneralCategory = byAbbrev[abbrev] ?: Cn
    }
}