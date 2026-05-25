package com.example

import android.content.Context
import android.util.Log
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

data class FontOption(
    val id: String,
    val name: String,
    val isCustom: Boolean = false,
    val localFileName: String? = null
)

object ThemeFontManager {
    private const val TAG = "ThemeFontManager"
    private const val PREFS_NAME = "keyboard_font_prefs"
    private const val KEY_SELECTED_FONT = "selected_font_id"
    private const val KEY_CUSTOM_FONTS = "custom_fonts_list"

    private val _fonts = MutableStateFlow<List<FontOption>>(emptyList())
    val fonts: StateFlow<List<FontOption>> = _fonts

    private val _selectedFont = MutableStateFlow<FontOption?>(null)
    val selectedFont: StateFlow<FontOption?> = _selectedFont

    // Stable CDN links to download high-quality Sinhala fonts
    private val preloadedFonts = listOf(
        // Apple-style clean, rounded curved Sinhala font (Noto Sans Sinhala is the gold standard)
        "apple_sinhala" to Triple("Apple Sinhala MN", "https://fonts.gstatic.com/s/notosanssinhala/v20/8Qv_XSD6H4K8eS7tH1V1e3wL-g8bPr76M0A.ttf", "apple_sinhala.ttf"),
        // Classic high-quality Serif Sinhala font
        "abhaya_libre" to Triple("Abhaya Libre (Serif)", "https://fonts.gstatic.com/s/abhayalibre/v13/FKqmFzVDyS_K_dfq8W0vGfX7_RFr8w.ttf", "abhaya_libre.ttf")
    )

    fun init(context: Context) {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaultList = mutableListOf(
            FontOption("default", "System Default")
        )

        // Add preloaded fonts to list (even if not yet downloaded, they will fall back to default until downloaded)
        for ((id, info) in preloadedFonts) {
            defaultList.add(FontOption(id, info.first, isCustom = false, localFileName = info.third))
        }

        // Load custom user font options saved in prefs
        val customFontsString = sharedPrefs.getString(KEY_CUSTOM_FONTS, "") ?: ""
        if (customFontsString.isNotEmpty()) {
            customFontsString.split(";").forEach {
                val parts = it.split(",")
                if (parts.size == 3) {
                    defaultList.add(FontOption(parts[0], parts[1], isCustom = true, localFileName = parts[2]))
                }
            }
        }

        _fonts.value = defaultList

        // Restore selected font
        val selectedId = sharedPrefs.getString(KEY_SELECTED_FONT, "apple_sinhala") ?: "apple_sinhala"
        _selectedFont.value = defaultList.find { it.id == selectedId } ?: defaultList.first()

        // Asynchronously check files and download missing preloaded fonts
        triggerPreloadedDownloads(context)
    }

    fun selectFont(context: Context, fontId: String) {
        val fontOption = _fonts.value.find { it.id == fontId } ?: return
        _selectedFont.value = fontOption
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_SELECTED_FONT, fontId)
            .apply()
    }

    /**
     * Lets users register a custom fonts copied from a custom file URI
     */
    fun registerCustomFont(context: Context, fontName: String, fontFile: File) {
        val uniqueId = "custom_${System.currentTimeMillis()}"
        val localName = "${uniqueId}.ttf"
        val destinationFile = File(context.filesDir, localName)

        try {
            fontFile.copyTo(destinationFile, overwrite = true)
            val newOption = FontOption(uniqueId, fontName, isCustom = true, localFileName = localName)

            // Update state flow
            val updatedList = _fonts.value + newOption
            _fonts.value = updatedList

            // Persist locally
            val customFontsString = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_CUSTOM_FONTS, "") ?: ""
            val appendString = if (customFontsString.isEmpty()) {
                "$uniqueId,$fontName,$localName"
            } else {
                "$customFontsString;$uniqueId,$fontName,$localName"
            }

            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putString(KEY_CUSTOM_FONTS, appendString)
                .apply()

            // Auto-select the newly added custom font
            selectFont(context, uniqueId)
        } catch (e: Exception) {
            Log.e(TAG, "Error registering custom font", e)
        }
    }

    /**
     * Resolves the font option to a Compose FontFamily
     */
    fun getFontFamily(context: Context, fontOption: FontOption?): FontFamily {
        if (fontOption == null || fontOption.id == "default" || fontOption.localFileName == null) {
            return FontFamily.Default
        }

        val file = File(context.filesDir, fontOption.localFileName)
        if (file.exists() && file.length() > 0) {
            try {
                return FontFamily(Font(file = file))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load font file: ${file.absolutePath}, fallback to default", e)
            }
        }
        return FontFamily.Default
    }

    private fun triggerPreloadedDownloads(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val client = OkHttpClient()
            for ((_, info) in preloadedFonts) {
                val (_, url, fileName) = info
                val localFile = File(context.filesDir, fileName)

                if (!localFile.exists() || localFile.length() == 0L) {
                    try {
                        Log.d(TAG, "Downloading preloaded font: $fileName from $url")
                        val request = Request.Builder().url(url).build()
                        client.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                response.body?.byteStream()?.use { inputStream ->
                                    FileOutputStream(localFile).use { outputStream ->
                                        inputStream.copyTo(outputStream)
                                    }
                                }
                                Log.d(TAG, "Successfully downloaded font: $fileName")
                            } else {
                                Log.e(TAG, "Failed downloading font $fileName. Status code: ${response.code}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception downloading font $fileName", e)
                    }
                }
            }
        }
    }
}
