package com.example

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SamsungKeyboardUI(
    viewModel: KeyboardViewModel,
    modifier: Modifier = Modifier,
    onSystemCommit: ((String) -> Unit)? = null,
    onSystemBackspace: (() -> Unit)? = null,
    onSystemSpace: (() -> Unit)? = null,
    onSystemEnter: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isDark = viewModel.isDarkTheme.collectAsState().value
    val keyboardMode = viewModel.keyboardMode.collectAsState().value
    val capsState = viewModel.capsState.collectAsState().value
    val isTranslateOpen = viewModel.isTranslationOpen.collectAsState().value
    val activePreviewKey = viewModel.activePreviewKey.collectAsState().value
    val currentFontFamily = viewModel.fontFamily.collectAsState().value

    // Theme values (Matching "Sleek Interface" styling spec)
    val keyboardBg = if (isDark) Color(0xFF131720) else Color(0xFFEDEFF2)
    val keyBg = if (isDark) Color(0xFF222938) else Color(0xFFFFFFFF)
    val keyText = if (isDark) Color(0xFFE5EAF3) else Color(0xFF131822)
    val specialKeyBg = if (isDark) Color(0xFF1C202B) else Color(0xFFD5D8DF)
    val accentColor = Color(0xFF2563EB) // Clean Blue Accent to match Tailwind theme
    val accentLight = Color(0x222563EB)

    // Font family lists
    val fontOptions by ThemeFontManager.fonts.collectAsState()
    val selectedFontOption by ThemeFontManager.selectedFont.collectAsState()

    var showFontSelector by remember { mutableStateOf(false) }

    // File picker to add custom TTF
    val customFontLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                // Read font file and copy
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val tempFile = File.createTempFile("custom_font_import", ".ttf", context.cacheDir)
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                    ThemeFontManager.registerCustomFont(context, "My Font ${fontOptions.size}", tempFile)
                    // Refresh view font
                    val lastFont = ThemeFontManager.fonts.value.lastOrNull()
                    if (lastFont != null) {
                        viewModel.updateFont(context, lastFont)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .background(keyboardBg)
            .border(
                border = BorderStroke(1.dp, if (isDark) Color(0xFF232D3F) else Color(0x3394A3B8)),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            )
            .padding(top = 4.dp, bottom = 4.dp)
            .pointerInput(Unit) {} // Prevent click-through
    ) {
        // --- SAMSUNG TOOLBAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(if (isDark) Color(0xFF0F1219) else Color(0xFFE5E7EB).copy(alpha = 0.5f))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Language Switching quick pill
                Surface(
                    onClick = { viewModel.toggleKeyboardMode() },
                    shape = RoundedCornerShape(16.dp),
                    color = if (keyboardMode == KeyboardMode.SINGLISH) accentColor else specialKeyBg,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = if (keyboardMode == KeyboardMode.SINGLISH) "SINGLISH ⟨සිංහල⟩" else "ENGLISH ⟨US⟩",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                // G-Translate Button
                IconButton(
                    onClick = {
                        viewModel.toggleTranslationToolbar()
                        showFontSelector = false
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "Translate",
                        tint = if (isTranslateOpen) accentColor else keyText.copy(alpha = 0.7f)
                    )
                }

                // Custom Font selector button
                IconButton(
                    onClick = {
                        showFontSelector = !showFontSelector
                        viewModel.toggleTranslationToolbar().takeIf { isTranslateOpen }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FontDownload,
                        contentDescription = "Font settings",
                        tint = if (showFontSelector) accentColor else keyText.copy(alpha = 0.7f)
                    )
                }

                // Quick theme toggle
                IconButton(
                    onClick = { viewModel.toggleTheme() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle theme",
                        tint = keyText.copy(alpha = 0.7f)
                    )
                }
            }

            // Samsung Branding indicator
            Text(
                text = "SAMSUNG",
                color = keyText.copy(alpha = 0.4f),
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        // --- GOOGLE TRANSLATE BOX (SAMSUNG STYLE) ---
        AnimatedVisibility(
            visible = isTranslateOpen,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDark) Color(0xFF222733) else Color(0xFFF3F4F6))
                    .padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val fromEng = viewModel.translateFromEnglish.collectAsState().value
                    Text(
                        text = if (fromEng) "English ➔ Sinhala" else "Sinhala ➔ English",
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )

                    IconButton(
                        onClick = { viewModel.toggleTranslationDirection() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CompareArrows,
                            contentDescription = "Swap direct",
                            tint = accentColor
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    val inputVal = viewModel.translationInput.collectAsState().value
                    val isTranslating = viewModel.isTranslating.collectAsState().value

                    TextField(
                        value = inputVal,
                        onValueChange = { viewModel.updateTranslationInput(it) },
                        placeholder = { Text("Type word/phrase to translate...") },
                        modifier = Modifier.weight(1f).height(48.dp),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = keyBg,
                            unfocusedContainerColor = keyBg,
                            focusedIndicatorColor = accentColor
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { viewModel.executeTranslation(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(44.dp),
                        enabled = !isTranslating
                    ) {
                        if (isTranslating) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Translate", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }

                // Show translation result and option to Insert
                val translationResult = viewModel.translationResult.collectAsState().value
                if (translationResult.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .background(keyBg, RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = translationResult,
                            color = keyText,
                            fontFamily = currentFontFamily,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (onSystemCommit != null) {
                                    onSystemCommit(translationResult)
                                } else {
                                    viewModel.appendTextToNotepad(translationResult)
                                }
                                viewModel.toggleTranslationToolbar()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Insert", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        // --- COMPOSABLE FONT SELECTOR TRAY ---
        AnimatedVisibility(
            visible = showFontSelector,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDark) Color(0xFF222733) else Color(0xFFF3F4F6))
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Customize Keyboard Fonts",
                        fontWeight = FontWeight.Bold,
                        color = keyText,
                        fontSize = 12.sp
                    )

                    Button(
                        onClick = { customFontLauncher.launch("font/ttf") },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Add .TTF Font", fontSize = 10.sp, color = Color.White)
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(fontOptions) { option ->
                        val isSelected = selectedFontOption?.id == option.id
                        val previewFontFamily = remember(option) {
                            ThemeFontManager.getFontFamily(context, option)
                        }

                        Surface(
                            onClick = {
                                ThemeFontManager.selectFont(context, option.id)
                                viewModel.updateFont(context, option)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) accentLight else keyBg,
                            border = if (isSelected) BorderStroke(1.5.dp, accentColor) else null,
                            modifier = Modifier.shadow(2.dp, RoundedCornerShape(12.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = option.name,
                                    color = if (isSelected) accentColor else keyText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                // Sinhala preview text
                                Text(
                                    text = "ලාංකික අභිමානය",
                                    color = if (isSelected) accentColor else keyText.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                    fontFamily = previewFontFamily
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- ACTUAL KEYBOARD LAYOUT GRID ---
        Spacer(modifier = Modifier.height(4.dp))

        // Row 1: q w e r t y u i o p
        val row1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            row1.forEach { key ->
                SamsungKey(
                    label = applyCaps(key, capsState),
                    onPress = { handleKeyInput(applyCaps(key, capsState), viewModel, onSystemCommit) },
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f),
                    bgColor = keyBg,
                    textColor = keyText,
                    currentFontFamily = currentFontFamily
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Row 2: a s d f g h j k l
        val row2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp), // margin to look aligned like real keyboard
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            row2.forEach { key ->
                SamsungKey(
                    label = applyCaps(key, capsState),
                    onPress = { handleKeyInput(applyCaps(key, capsState), viewModel, onSystemCommit) },
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f),
                    bgColor = keyBg,
                    textColor = keyText,
                    currentFontFamily = currentFontFamily
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Row 3: Caps, z, x, c, v, b, n, m, Backspace
        val row3 = listOf("z", "x", "c", "v", "b", "n", "m")
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shift / Caps key
            val shiftIcon = when (capsState) {
                CapsState.LOWERCASE -> Icons.Default.ArrowUpward
                CapsState.SHIFT_ONCE -> Icons.Default.KeyboardArrowUp
                CapsState.CAPS_LOCK -> Icons.Default.Upload
            }
            SamsungIconKey(
                imageVector = shiftIcon,
                onPress = { viewModel.toggleCaps() },
                modifier = Modifier.width(44.dp),
                bgColor = if (capsState != CapsState.LOWERCASE) accentLight else specialKeyBg,
                tintColor = if (capsState != CapsState.LOWERCASE) accentColor else keyText
            )

            // Consonants
            row3.forEach { key ->
                SamsungKey(
                    label = applyCaps(key, capsState),
                    onPress = { handleKeyInput(applyCaps(key, capsState), viewModel, onSystemCommit) },
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f),
                    bgColor = keyBg,
                    textColor = keyText,
                    currentFontFamily = currentFontFamily
                )
            }

            // Backspace key
            SamsungIconKey(
                imageVector = Icons.Default.Backspace,
                onPress = {
                    triggerFeedback(context)
                    if (onSystemBackspace != null) {
                        onSystemBackspace()
                    } else {
                        viewModel.onBackspaceInNotepad()
                    }
                },
                modifier = Modifier.width(44.dp),
                bgColor = specialKeyBg,
                tintColor = keyText
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Row 4: Mode switch (?123), Language, SPACE, Period (.), Enter
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Symbols Switch (?123 toggle)
            SamsungKey(
                label = "?123",
                onPress = {
                    triggerFeedback(context)
                    // Toggle typing numbers easily or symbols
                    if (onSystemCommit != null) {
                        onSystemCommit("?")
                    } else {
                        viewModel.onKeyPressInNotepad("?")
                    }
                },
                viewModel = viewModel,
                modifier = Modifier.width(52.dp),
                bgColor = specialKeyBg,
                textColor = keyText,
                currentFontFamily = currentFontFamily
            )

            // Language / Space layout
            Box(
                modifier = Modifier
                    .weight(3f)
                    .height(44.dp)
                    .shadow(1.dp, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .background(keyBg)
                    .clickable {
                        triggerFeedback(context)
                        if (onSystemSpace != null) {
                            onSystemSpace()
                        } else {
                            viewModel.onSpaceInNotepad()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    if (keyboardMode == KeyboardMode.SINGLISH) {
                        Text(
                            text = "SINGLISH",
                            color = keyText.copy(alpha = 0.4f),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = "English (US)",
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = currentFontFamily
                        )
                    } else {
                        Text(
                            text = "ENGLISH (US)",
                            color = keyText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = currentFontFamily
                        )
                    }
                }
            }

            // Period symbol shortcuts
            SamsungKey(
                label = ".",
                onPress = { handleKeyInput(".", viewModel, onSystemCommit) },
                viewModel = viewModel,
                modifier = Modifier.width(36.dp),
                bgColor = keyBg,
                textColor = keyText,
                currentFontFamily = currentFontFamily
            )

            // Enter / Commit output
            SamsungIconKey(
                imageVector = Icons.Default.KeyboardReturn,
                onPress = {
                    triggerFeedback(context)
                    if (onSystemEnter != null) {
                        onSystemEnter()
                    } else {
                        viewModel.onEnterInNotepad()
                    }
                },
                modifier = Modifier.width(52.dp),
                bgColor = accentColor,
                tintColor = Color.White
            )
        }
    }
}

@Composable
fun SamsungKey(
    label: String,
    onPress: () -> Unit,
    viewModel: KeyboardViewModel,
    modifier: Modifier = Modifier,
    bgColor: Color,
    textColor: Color,
    currentFontFamily: FontFamily
) {
    val context = LocalContext.current
    val activePreviewKey = viewModel.activePreviewKey.collectAsState().value
    val isPressed = activePreviewKey == label

    Box(
        modifier = modifier
            .height(44.dp)
            .shadow(
                elevation = if (isPressed) 4.dp else 1.dp,
                shape = RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPressed) bgColor.copy(alpha = 0.7f) else bgColor)
            .pointerInput(label) {
                detectTapGestures(
                    onPress = {
                        viewModel.setActivePreviewKey(label)
                        triggerFeedback(context)
                        try {
                            onPress()
                        } finally {
                            awaitRelease()
                            viewModel.setActivePreviewKey(null)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = if (label.length > 1) 12.sp else 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = currentFontFamily,
            textAlign = TextAlign.Center
        )

        // Magnified popping preview balloon above key! (Pure Samsung design)
        if (isPressed && label.length == 1) {
            Box(
                modifier = Modifier
                    .offset(y = (-52).dp)
                    .size(width = 46.dp, height = 50.dp)
                    .shadow(6.dp, RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F62FE), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = currentFontFamily,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun SamsungIconKey(
    imageVector: ImageVector,
    onPress: () -> Unit,
    modifier: Modifier = Modifier,
    bgColor: Color,
    tintColor: Color
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .height(44.dp)
            .shadow(1.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable {
                triggerFeedback(context)
                onPress()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = tintColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun applyCaps(key: String, capsState: CapsState): String {
    return if (capsState == CapsState.LOWERCASE) key else key.uppercase()
}

/**
 * Triggers feedback like typing sound and haptics
 */
private fun triggerFeedback(context: Context) {
    try {
        // Play click sound
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 1.0f)

        // Shake device briefly
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(15)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun handleKeyInput(key: String, viewModel: KeyboardViewModel, systemCommit: ((String) -> Unit)?) {
    if (systemCommit != null) {
        // IME Mode handles composing buffers inside SinhalaInputMethodService
        systemCommit(key)
    } else {
        // Notepad Playground mode
        viewModel.onKeyPressInNotepad(key)
    }
    viewModel.resetCapsAfterKeyPress()
}
