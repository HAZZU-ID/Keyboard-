package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: KeyboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize our Font system of download and local storage options
        ThemeFontManager.init(applicationContext)

        // Set default font value to start
        val firstFont = ThemeFontManager.fonts.value.firstOrNull { it.id == "apple_sinhala" }
            ?: ThemeFontManager.fonts.value.firstOrNull()
        if (firstFont != null) {
            viewModel.updateFont(applicationContext, firstFont)
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SamsungKeyboardPlayground(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun SamsungKeyboardPlayground(
    viewModel: KeyboardViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val notepadText = viewModel.notepadText.collectAsState().value
    val isDark = viewModel.isDarkTheme.collectAsState().value
    val currentFontFamily = viewModel.fontFamily.collectAsState().value
    val keyboardMode = viewModel.keyboardMode.collectAsState().value
    val activeFontOption by ThemeFontManager.selectedFont.collectAsState()

    var showSystemGuide by remember { mutableStateOf(false) }

    // Unified dynamic theme colors
    val primaryBg = if (isDark) Color(0xFF0F1318) else Color(0xFFF2F2F7)
    val cardBg = if (isDark) Color(0xFF161B26) else Color(0xFFFFFFFF)
    val textColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
    val headerBrush = Brush.linearGradient(
        colors = listOf(Color(0xFF2563EB), Color(0xFF0F62FE), Color(0xFF1D4ED8))
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val liveBuffer = viewModel.composingBuffer.collectAsState().value

    Column(
        modifier = modifier
            .background(primaryBg)
            .fillMaxSize()
    ) {
        // --- COOL ACCENT HEADER DESIGN ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBrush)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SAMSUNG",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Sinhala Singlish Keyboard",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = { showSystemGuide = !showSystemGuide },
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                ) {
                    Icon(
                        imageVector = if (showSystemGuide) Icons.Default.Close else Icons.Default.HelpOutline,
                        contentDescription = "Show setup guide"
                    )
                }
            }
        }

        // Expanded workspace scrolling section
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(14.dp)
        ) {
            // --- POP-UP DEVICE SETUP SERVICE INSTRUCTIONS ---
            AnimatedVisibility(
                visible = showSystemGuide,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .border(1.5.dp, Color(0xFF2563EB), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E2638) else Color(0xFFEBF2FF)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Keyboard, contentDescription = null, tint = Color(0xFF2563EB))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Enable Keyboard on Real Phone",
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF1E3A8A),
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "To use this keyboard in other apps (Viber, WhatsApp, etc.):",
                            fontSize = 12.sp,
                            color = textColor.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val steps = listOf(
                            "1. Go to your Phone Settings",
                            "2. Navigate to System -> Languages & Input -> On-screen keyboards",
                            "3. Click 'Manage keyboards' and enable Sinhala Singlish Keyboard",
                            "4. Switch to this keyboard whenever typing to type Singlish!"
                        )
                        steps.forEach { step ->
                            Text(
                                text = step,
                                fontSize = 11.sp,
                                color = textColor.copy(alpha = 0.85f),
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 2.dp)
                            )
                        }
                    }
                }
            }

            // --- MAIN TYPING PALETTE CARD ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 240.dp)
                    .shadow(2.dp, RoundedCornerShape(24.dp))
                    .border(1.dp, if (isDark) Color(0xFF2C3545) else Color(0xFFE2E8F0), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Animated pulsing dot + Active Transliteration status
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF2563EB).copy(alpha = pulseAlpha), RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SINGLISH TRANSLITERATION ACTIVE",
                                color = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        Row {
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(notepadText))
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = textColor.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { viewModel.clearNotepad() },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Clear", tint = textColor.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Text contents display space
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Text(
                            text = if (notepadText.isEmpty()) "Tap the Samsung keyboard keys below to start typing..." else notepadText,
                            color = if (notepadText.isEmpty()) textColor.copy(alpha = 0.4f) else textColor,
                            fontSize = 18.sp,
                            fontFamily = currentFontFamily,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (liveBuffer.isNotEmpty()) "$liveBuffer...|" else if (notepadText.isNotEmpty()) "typing..." else "|",
                            color = if (isDark) Color(0x99A1A1AA) else Color(0x9964748B),
                            fontSize = 14.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- WORD STATISTICS CARD ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Char Count
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.TextFormat, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF0F62FE))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Chars: ${notepadText.length}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor.copy(alpha = 0.8f)
                        )
                    }
                }

                // Selected Font info
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.FontDownload, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF10B981))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Font: ${activeFontOption?.name ?: "System"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // --- THE KEYBOARD RENDERING AT THE BOTTOM ---
        SamsungKeyboardUI(
            viewModel = viewModel,
            modifier = Modifier.navigationBarsPadding()
        )
    }
}
