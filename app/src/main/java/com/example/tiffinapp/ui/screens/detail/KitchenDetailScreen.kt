package com.example.tiffinapp.ui.screens.detail

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tiffinapp.Analytics
import com.example.tiffinapp.data.model.Kitchen
import com.example.tiffinapp.ui.viewmodel.TiffinViewModel

private val TiffinOrange = Color(0xFFFF6B00)
private val VegGreen     = Color(0xFF2E7D32)
private val NonVegRed    = Color(0xFFC62828)

// Ordered days for consistent display
private val DAYS_ORDER = listOf(
    "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitchenDetailScreen(
    viewModel: TiffinViewModel,
    onBack: () -> Unit
) {
    val kitchen by viewModel.selectedKitchen.collectAsState()
    val isPaid by viewModel.isPaid.collectAsState()
    val context = LocalContext.current

    kitchen?.let { k ->
        // Log analytics when detail screen is displayed
        LaunchedEffect(k.id) {
            Analytics.tracker.logEvent(
                Analytics.EVENT_KITCHEN_DETAIL_VIEWED,
                mapOf(Analytics.PARAM_KITCHEN_ID to k.id)
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text  = k.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                // Sticky bottom Subscribe CTA
                Surface(
                    modifier        = Modifier.navigationBarsPadding(),
                    shadowElevation = 16.dp,
                    color           = MaterialTheme.colorScheme.surface
                ) {
                    Button(
                        onClick  = {
                            if (isPaid) {
                                Toast.makeText(
                                    context,
                                    "Subscription Active: Daily meals are scheduled from ${k.name}!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                viewModel.onSubscribeClicked()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = if (isPaid) VegGreen else TiffinOrange
                        ),
                        shape    = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text  = if (isPaid) "✓ Plan Active — Daily Delivery Confirmed" else "🍱 Subscribe — ₹${k.pricePerTiffin} / tiffin",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            KitchenDetailContent(
                kitchen  = k,
                modifier = Modifier.padding(innerPadding)
            )
        }
    } ?: run {
        // Fallback if selectedKitchen is null (shouldn't happen in normal flow)
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Kitchen not found")
        }
    }
}

@Composable
private fun KitchenDetailContent(
    kitchen: Kitchen,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier            = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {

        // ── Hero header ──────────────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(
                                TiffinOrange.copy(alpha = 0.85f),
                                TiffinOrange.copy(alpha = 0.4f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = "🍱",
                    style = MaterialTheme.typography.displayLarge
                )
            }
        }

        // ── Info section ─────────────────────────────────────────────────────
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {

                Text(
                    text  = kitchen.name,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = kitchen.cuisineType,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    RatingChip(rating = kitchen.rating)
                    VegChip(isVeg = kitchen.isVeg)
                    PriceChip(price = kitchen.pricePerTiffin)
                }
            }
        }

        item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp)) }

        // ── Weekly menu section ──────────────────────────────────────────────
        item {
            Text(
                text     = "📅 Weekly Menu",
                style    = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        items(DAYS_ORDER.size) { index ->
            val day  = DAYS_ORDER[index]
            val meal = kitchen.weeklyMenu[day] ?: "Rest day"
            MenuDayRow(day = day, meal = meal)
            if (index < DAYS_ORDER.size - 1) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color    = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }

        // Bottom padding so content clears the sticky bottom bar
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun MenuDayRow(day: String, meal: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape     = RoundedCornerShape(10.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier          = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text     = day,
                style    = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color      = TiffinOrange
                ),
                modifier = Modifier.width(90.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text  = meal,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun RatingChip(rating: Double) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFFFF3E0)
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector        = Icons.Default.Star,
                contentDescription = "Rating",
                tint               = Color(0xFFFB8C00),
                modifier           = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text  = rating.toString(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFFE65100)
                )
            )
        }
    }
}

@Composable
private fun VegChip(isVeg: Boolean) {
    val color = if (isVeg) VegGreen else NonVegRed
    val label = if (isVeg) "🟢 Veg" else "🔴 Non-Veg"
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text      = label,
            modifier  = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style     = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color      = color
            )
        )
    }
}

@Composable
private fun PriceChip(price: Int) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = TiffinOrange.copy(alpha = 0.12f)
    ) {
        Text(
            text      = "₹$price / tiffin",
            modifier  = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style     = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color      = TiffinOrange
            )
        )
    }
}
