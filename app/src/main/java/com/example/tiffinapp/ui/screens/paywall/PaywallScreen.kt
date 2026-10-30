package com.example.tiffinapp.ui.screens.paywall

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.tiffinapp.Analytics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val TiffinOrange   = Color(0xFFFF6B00)
private val TiffinOrangeDk = Color(0xFFD84600)
private val GoldYellow     = Color(0xFFFFB300)

/** Computes the human-readable charge date: current time + 24 hours. */
private fun computeChargeDate(): String {
    val tomorrow = Date(System.currentTimeMillis() + 86_400_000L)
    val formatter = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault())
    return formatter.format(tomorrow)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    trigger: String,
    onSubscribe: () -> Unit,
    onDismiss: () -> Unit
) {
    // Log analytics exactly once when this composable first enters the composition
    LaunchedEffect(trigger) {
        Analytics.tracker.logEvent(
            Analytics.EVENT_PAYWALL_SHOWN,
            mapOf(Analytics.PARAM_TRIGGER to trigger)
        )
    }

    // Compute once per composition so the date doesn't flicker on recomposition
    val chargeDate = remember { computeChargeDate() }

    Scaffold(
        topBar = {
            TopAppBar(
                title  = {},
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Dismiss paywall",
                            tint               = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TiffinOrange
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .navigationBarsPadding()          // ← clears the gesture/button nav bar
                .background(
                    Brush.verticalGradient(
                        listOf(TiffinOrange, TiffinOrangeDk, Color(0xFF212121))
                    )
                )
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Hero emoji ────────────────────────────────────────────────────
            Spacer(Modifier.height(24.dp))
            Text(text = "🍱", style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(8.dp))

            Text(
                text      = "Start Your Tiffin Trial",
                style     = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color      = Color.White
                ),
                textAlign = TextAlign.Center,
                modifier  = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text      = "Home-cooked meals, unlimited kitchens,\ncancellable anytime.",
                style     = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.8f)),
                textAlign = TextAlign.Center,
                modifier  = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(Modifier.height(28.dp))

            // ── Pricing card ─────────────────────────────────────────────────
            Surface(
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape         = RoundedCornerShape(20.dp),
                color         = Color.White.copy(alpha = 0.12f),
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier            = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Trial price
                    Text(
                        text  = "₹1",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color      = GoldYellow
                        )
                    )
                    Text(
                        text  = "for the first 24 hours",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    // Dynamic charge date notice
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "₹1 is charged now, and ₹249 will be charged on " +
                                    "$chargeDate 24 hours later.",
                            style    = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            ),
                            textAlign = TextAlign.Center,
                            modifier  = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Feature list ─────────────────────────────────────────────────
            Column(
                modifier            = Modifier.padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeatureRow("Access all 12+ home kitchens")
                FeatureRow("Fresh daily tiffin delivered")
                FeatureRow("Weekly menu planning")
                FeatureRow("Cancel anytime, no lock-in")
            }

            Spacer(Modifier.height(32.dp))

            // ── CTA button ───────────────────────────────────────────────────
            Button(
                onClick  = onSubscribe,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(56.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = GoldYellow),
                shape    = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text  = "Start Trial & Subscribe",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color      = Color(0xFF1A1A1A)
                    )
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text  = "By subscribing, you agree to our Terms & Privacy Policy.\nYou can cancel anytime.",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White.copy(alpha = 0.5f)
                ),
                textAlign = TextAlign.Center,
                modifier  = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FeatureRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier         = Modifier
                .size(22.dp)
                .background(GoldYellow, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.Check,
                contentDescription = null,
                tint               = Color(0xFF1A1A1A),
                modifier           = Modifier.size(14.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text  = text,
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
        )
    }
}
