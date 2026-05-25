package com.example

import android.util.Log

object SinglishTransliteration {
    // Maps of complex multi-character consonants and vowels.
    private val consonantsMap = mapOf(
        "sh" to "ශ්",
        "sh" to "ශ්",
        "th" to "ත්",
        "dh" to "ද්",
        "ch" to "ච්",
        "kh" to "ඛ්",
        "gh" to "ග්",
        "ph" to "ඵ්",
        "bh" to "භ්",
        "jh" to "ඣ්",
        "nd" to "ඳ්",
        "ng" to "ඟ්",
        "mb" to "ඹ්",
        "ny" to "ඤ්",
        "gn" to "ඥ්",
        "kn" to "ඥ්",
        "t" to "ට්",
        "d" to "ඩ්",
        "k" to "ක්",
        "g" to "ග්",
        "j" to "ජ්",
        "p" to "ප්",
        "b" to "බ්",
        "m" to "ම්",
        "y" to "ය්",
        "r" to "ර්",
        "l" to "ල්",
        "v" to "ව්",
        "w" to "ව්",
        "s" to "ස්",
        "h" to "හ්",
        "f" to "ෆ්",
        "c" to "ක්",
        "T" to "ට්",
        "D" to "ඩ්",
        "N" to "ණ්",
        "L" to "ළ්",
        "S" to "ශ්",
        "K" to "ඛ්",
        "G" to "ඝ්"
    )

    // Standalone vowel map (vowels at the beginning of a word or after another vowel/space)
    private val independentVowelsMap = mapOf(
        "aae" to "ඈ",
        "ae" to "ඇ",
        "aa" to "ආ",
        "ii" to "ඊ",
        "uu" to "ඌ",
        "ee" to "ඒ",
        "oo" to "ඕ",
        "au" to "ඖ",
        "ai" to "ඓ",
        "a" to "අ",
        "i" to "ඉ",
        "u" to "උ",
        "e" to "එ",
        "o" to "ඔ",
        "A" to "ආ"
    )

    // Dependent vowel modifiers mapping
    private val dependentVowelsMap = mapOf(
        "aae" to "ෑ",
        "ae" to "ැ",
        "aa" to "ා",
        "ii" to "ී",
        "uu" to "ූ",
        "ee" to "ේ",
        "oo" to "ෝ",
        "au" to "ෞ",
        "ai" to "ෛ",
        "a" to "", // empty means inherits just Hal-Kirima removal
        "i" to "ි",
        "u" to "ු",
        "e" to "ෙ",
        "o" to "ො",
        "A" to "ා"
    )

    // Special combinations
    private val specialMap = mapOf(
        "kriya" to "ක්‍රියා",
        "lanka" to "ලංකා"
    )

    /**
     * Converts a Singlish input string to phonetically equivalent Sinhala.
     * We process word by word to make it accurate and clean.
     */
    fun transliterate(input: String): String {
        if (input.isEmpty()) return ""

        // Preserve trailing spaces or non-letter characters
        val words = input.split(" ")
        val result = StringBuilder()

        for (i in words.indices) {
            val word = words[i]
            if (word.isEmpty()) {
                result.append("")
            } else {
                result.append(transliterateWord(word))
            }
            if (i < words.size - 1) {
                result.append(" ")
            }
        }

        // Keep spaces trailing exactly
        if (input.endsWith(" ")) {
            result.append(" ")
        }
        return result.toString()
    }

    private fun transliterateWord(word: String): String {
        // Quick check for unique exact matches
        specialMap[word.lowercase()]?.let { return it }

        val sb = StringBuilder()
        var index = 0
        val len = word.length

        // State tracker: was the previous token a consonant that is pending a vowel?
        var lastConsonantBase: String? = null

        while (index < len) {
            // 1. Try to match vowels first if we don't have a pending consonant
            // or if we want to check vowel modifiers for the pending consonant.
            
            // Look ahead for vowel modifier mapping (longest vowel first)
            var foundVowel = false
            for (vLen in 3 downTo 1) {
                if (index + vLen <= len) {
                    val potentialVowel = word.substring(index, index + vLen)
                    if (dependentVowelsMap.containsKey(potentialVowel)) {
                        val vowelMod = dependentVowelsMap[potentialVowel]!!
                        if (lastConsonantBase != null) {
                            // Apply dependent modifier to the pending consonant
                            // Remove the hal character '්' (Unicode 0DCA) from the end of the last consonant
                            if (sb.isNotEmpty() && sb.endsWith('්')) {
                                sb.deleteCharAt(sb.length - 1)
                            }
                            
                            // Handling Rakaransaya / Yansaya before modifying
                            // If user typed "kr" or "ky", we might have modifiers already.
                            sb.append(vowelMod)
                            lastConsonantBase = null
                        } else {
                            // Standalone vowel
                            sb.append(independentVowelsMap[potentialVowel] ?: potentialVowel)
                        }
                        index += vLen
                        foundVowel = true
                        break
                    }
                }
            }

            if (foundVowel) continue

            // 2. Look ahead for special Rakaransaya / Yansaya rules:
            // "kr" -> ක + ්‍ර (rakaransaya) + hal. e.g. "kriya" -> "ක්‍රියා"
            // "y" after a consonant acts as yansaya "්‍ය".
            if (lastConsonantBase != null && index < len) {
                val nextChar = word[index]
                if (nextChar == 'r' || nextChar == 'R') {
                    // Check if we can apply rakaransaya
                    if (sb.isNotEmpty() && sb.endsWith('්')) {
                        sb.deleteCharAt(sb.length - 1) // remove hal kirima
                        sb.append("\u0DCA\u200D\u0DBB") // add rakaransaya diacritic
                        sb.append("්") // put hal-kirima back temporarily, will dissolve if vowel follows
                        index++
                        continue
                    }
                } else if (nextChar == 'y' || nextChar == 'Y') {
                    // Check if we can apply yansaya
                    if (sb.isNotEmpty() && sb.endsWith('්')) {
                        sb.deleteCharAt(sb.length - 1) // remove hal kirima
                        sb.append("\u0DCA\u200D\u0DBA") // add yansaya diacritic
                        sb.append("්") // put hal-kirima back temporarily
                        index++
                        continue
                    }
                }
            }

            // 3. Match consonants (longest match first)
            var foundConsonant = false
            for (cLen in 2 downTo 1) {
                if (index + cLen <= len) {
                    val potentialConsonant = word.substring(index, index + cLen)
                    if (consonantsMap.containsKey(potentialConsonant)) {
                        val consonantVal = consonantsMap[potentialConsonant]!!
                        sb.append(consonantVal)
                        lastConsonantBase = consonantVal
                        index += cLen
                        foundConsonant = true
                        break
                    }
                }
            }

            if (foundConsonant) continue

            // If nothing matched, just output character itself
            val char = word[index]
            sb.append(char)
            lastConsonantBase = null
            index++
        }

        return sb.toString()
    }
}
