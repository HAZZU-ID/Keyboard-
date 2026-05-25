package com.example

import android.content.Context
import android.util.Log
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class KeyboardMode {
    ENGLISH, SINGLISH
}

enum class CapsState {
    LOWERCASE, SHIFT_ONCE, CAPS_LOCK
}

class KeyboardViewModel : ViewModel() {
    private val _keyboardMode = MutableStateFlow(KeyboardMode.SINGLISH)
    val keyboardMode: StateFlow<KeyboardMode> = _keyboardMode

    private val _capsState = MutableStateFlow(CapsState.LOWERCASE)
    val capsState: StateFlow<CapsState> = _capsState

    // Stable committed text
    private val _stableText = MutableStateFlow("")
    
    // Combined display text (Stable + Transliterated Composing)
    private val _notepadText = MutableStateFlow("")
    val notepadText: StateFlow<String> = _notepadText

    // Live composing English buffer used for dynamic real-time transliteration
    private val _composingBuffer = MutableStateFlow("")
    val composingBuffer: StateFlow<String> = _composingBuffer


    // Translation UI block states
    private val _isTranslationOpen = MutableStateFlow(false)
    val isTranslationOpen: StateFlow<Boolean> = _isTranslationOpen

    private val _translationInput = MutableStateFlow("")
    val translationInput: StateFlow<String> = _translationInput

    private val _translateFromEnglish = MutableStateFlow(true)
    val translateFromEnglish: StateFlow<Boolean> = _translateFromEnglish

    private val _isTranslating = MutableStateFlow(false)
    val isTranslating: StateFlow<Boolean> = _isTranslating

    private val _translationResult = MutableStateFlow("")
    val translationResult: StateFlow<String> = _translationResult

    // Selected Font Flow - initialized with Apple Sinhala font
    private val _fontFamily = MutableStateFlow<FontFamily>(FontFamily.Default)
    val fontFamily: StateFlow<FontFamily> = _fontFamily

    // Dark/Light Keyboard UI theme
    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme

    // State of key being touched currently for magnified pop-up preview bubble
    private val _activePrevewKey = MutableStateFlow<String?>(null)
    val activePreviewKey: StateFlow<String?> = _activePrevewKey

    fun setKeyboardMode(mode: KeyboardMode) {
        // Complete compiling buffer when switching mode
        commitComposingText()
        _keyboardMode.value = mode
    }

    fun toggleKeyboardMode() {
        setKeyboardMode(if (_keyboardMode.value == KeyboardMode.ENGLISH) KeyboardMode.SINGLISH else KeyboardMode.ENGLISH)
    }

    fun toggleCaps() {
        _capsState.value = when (_capsState.value) {
            CapsState.LOWERCASE -> CapsState.SHIFT_ONCE
            CapsState.SHIFT_ONCE -> CapsState.CAPS_LOCK
            CapsState.CAPS_LOCK -> CapsState.LOWERCASE
        }
    }

    fun resetCapsAfterKeyPress() {
        if (_capsState.value == CapsState.SHIFT_ONCE) {
            _capsState.value = CapsState.LOWERCASE
        }
    }

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun updateFont(context: Context, option: FontOption) {
        _fontFamily.value = ThemeFontManager.getFontFamily(context, option)
    }

    fun setActivePreviewKey(key: String?) {
        _activePrevewKey.value = key
    }

    // --- In-App Notepad Text Editors Operations ---

    fun onKeyPressInNotepad(key: String) {
        if (_keyboardMode.value == KeyboardMode.ENGLISH) {
            _stableText.value += key
            _notepadText.value = _stableText.value
        } else {
            // Singlish real-time transliteration mode
            // Check if key is a letter that belongs to phonetic parsing, otherwise commit active buffer
            if (key.length == 1 && key[0].lowercaseChar() in 'a'..'z') {
                val newBuffer = _composingBuffer.value + key
                _composingBuffer.value = newBuffer
                updateNotepadWithActiveBuffer()
            } else {
                // Typings of space, period or special characters commit previous composition
                commitComposingText()
                _stableText.value += key
                _notepadText.value = _stableText.value
            }
        }
    }

    fun onBackspaceInNotepad() {
        if (_composingBuffer.value.isNotEmpty()) {
            _composingBuffer.value = _composingBuffer.value.dropLast(1)
            updateNotepadWithActiveBuffer()
        } else if (_stableText.value.isNotEmpty()) {
            _stableText.value = _stableText.value.dropLast(1)
            _notepadText.value = _stableText.value
        }
    }

    fun onSpaceInNotepad() {
        commitComposingText()
        _stableText.value += " "
        _notepadText.value = _stableText.value
    }

    fun onEnterInNotepad() {
        commitComposingText()
        _stableText.value += "\n"
        _notepadText.value = _stableText.value
    }

    fun clearNotepad() {
        _stableText.value = ""
        _composingBuffer.value = ""
        _notepadText.value = ""
    }

    private fun updateNotepadWithActiveBuffer() {
        val transliterated = SinglishTransliteration.transliterate(_composingBuffer.value)
        _notepadText.value = _stableText.value + transliterated
    }

    private fun commitComposingText() {
        if (_composingBuffer.value.isNotEmpty()) {
            val transliterated = SinglishTransliteration.transliterate(_composingBuffer.value)
            _stableText.value += transliterated
            _composingBuffer.value = ""
            _notepadText.value = _stableText.value
        }
    }

    // Direct interface to update raw text (for setting translations directly)
    fun appendTextToNotepad(text: String) {
        commitComposingText()
        _stableText.value += text
        _notepadText.value = _stableText.value
    }

    // --- Google Translation Block Handlers ---

    fun toggleTranslationToolbar() {
        _isTranslationOpen.value = !_isTranslationOpen.value
        _translationResult.value = ""
        _translationInput.value = ""
    }

    fun updateTranslationInput(input: String) {
        _translationInput.value = input
    }

    fun toggleTranslationDirection() {
        _translateFromEnglish.value = !_translateFromEnglish.value
    }

    fun executeTranslation(context: Context) {
        val query = _translationInput.value.trim()
        if (query.isEmpty()) return

        _isTranslating.value = true
        _translationResult.value = "Translating..."

        viewModelScope.launch {
            try {
                val result = TranslationService.translate(query, _translateFromEnglish.value)
                _translationResult.value = result
            } catch (e: Exception) {
                _translationResult.value = "Translation error: ${e.message}"
            } finally {
                _isTranslating.value = false
            }
        }
    }
}
