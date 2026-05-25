package com.example

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.*
import androidx.savedstate.*

class SinhalaInputMethodService : InputMethodService(),
    LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    // Service Lifecycle registration support so Compose rendering works perfectly inside IME
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val controller = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry

    private lateinit var viewModel: KeyboardViewModel
    private var composingBuffer = StringBuilder()

    override fun onCreate() {
        super.onCreate()
        controller.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        
        viewModel = KeyboardViewModel()
        // Initialize static fonts list
        ThemeFontManager.init(applicationContext)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        composingBuffer.clear()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        commitComposing()
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    override fun onCreateInputView(): View {
        val composeView = ComposeView(this)
        composeView.setContent {
            SamsungKeyboardUI(
                viewModel = viewModel,
                modifier = Modifier.fillMaxWidth(),
                onSystemCommit = { char ->
                    handleSystemCommit(char)
                },
                onSystemBackspace = {
                    handleSystemBackspace()
                },
                onSystemSpace = {
                    handleSystemSpace()
                },
                onSystemEnter = {
                    handleSystemEnter()
                }
            )
        }

        // Setup the Service context as the Lifecycle holder of this view hierarchies
        composeView.setViewTreeLifecycleOwner(this)
        composeView.setViewTreeViewModelStoreOwner(this)
        composeView.setViewTreeSavedStateRegistryOwner(this)

        return composeView
    }

    private fun handleSystemCommit(text: String) {
        val ic = currentInputConnection ?: return
        val currentMode = viewModel.keyboardMode.value

        if (currentMode == KeyboardMode.ENGLISH) {
            ic.commitText(text, 1)
        } else {
            // Singlish real-time transliteration mapping
            if (text.length == 1 && text[0].lowercaseChar() in 'a'..'z') {
                composingBuffer.append(text)
                val transliterated = SinglishTransliteration.transliterate(composingBuffer.toString())
                ic.setComposingText(transliterated, 1)
            } else {
                commitComposing()
                ic.commitText(text, 1)
            }
        }
    }

    private fun handleSystemBackspace() {
        val ic = currentInputConnection ?: return
        if (viewModel.keyboardMode.value == KeyboardMode.SINGLISH && composingBuffer.isNotEmpty()) {
            composingBuffer.deleteCharAt(composingBuffer.length - 1)
            if (composingBuffer.isEmpty()) {
                ic.setComposingText("", 1)
            } else {
                val transliterated = SinglishTransliteration.transliterate(composingBuffer.toString())
                ic.setComposingText(transliterated, 1)
            }
        } else {
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
        }
    }

    private fun handleSystemSpace() {
        val ic = currentInputConnection ?: return
        commitComposing()
        ic.commitText(" ", 1)
    }

    private fun handleSystemEnter() {
        val ic = currentInputConnection ?: return
        commitComposing()
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
    }

    private fun commitComposing() {
        val ic = currentInputConnection ?: return
        if (composingBuffer.isNotEmpty()) {
            ic.finishComposingText()
            composingBuffer.clear()
        }
    }
}
