package com.example.tiffinapp.ui.screens.list

import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.tiffinapp.Analytics
import com.example.tiffinapp.data.model.Kitchen
import com.example.tiffinapp.data.model.KitchenState
import com.example.tiffinapp.ui.viewmodel.TiffinViewModel

// ── Brand colours ─────────────────────────────────────────────────────────────
private val TiffinOrange = Color(0xFFFF6B00)
private val VegGreen     = Color(0xFF2E7D32)
private val NonVegRed    = Color(0xFFC62828)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitchenListScreen(
    viewModel: TiffinViewModel,
    onNavigateToDetail: (Kitchen) -> Unit
) {
    val kitchenState by viewModel.kitchenState.collectAsState()
    val isPaid by viewModel.isPaid.collectAsState()

    // Log analytics once when the screen enters the composition
    LaunchedEffect(Unit) {
        Analytics.tracker.logEvent(Analytics.EVENT_KITCHEN_LIST_VIEWED)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text  = "🍱 Tiffin",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color      = TiffinOrange
                                )
                            )
                            Text(
                                text  = "Home-cooked meals delivered daily",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                // Visual search bar placeholder
                SearchBarPlaceholder()
            }
        }
    ) { innerPadding ->
        when (val state = kitchenState) {
            is KitchenState.Loading -> LoadingShimmer(modifier = Modifier.padding(innerPadding))

            is KitchenState.Success -> {
                LazyColumn(
                    modifier            = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .navigationBarsPadding(),   // ← last card clears gesture nav bar
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding      = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp, vertical = 12.dp
                    )
                ) {
                    items(items = state.kitchens, key = { it.id }) { kitchen ->
                        KitchenCard(
                            kitchen     = kitchen,
                            isPaid      = isPaid,
                            onCardClick = { onNavigateToDetail(kitchen) },
                            onSubscribe = { viewModel.onSubscribeClicked() }
                        )
                    }
                }
            }

            is KitchenState.Empty -> EmptyState(modifier = Modifier.padding(innerPadding))

            is KitchenState.Error -> ErrorState(
                message  = state.message,
                onRetry  = viewModel::retry,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

// ── Search Bar Placeholder ────────────────────────────────────────────────────

@Composable
private fun SearchBarPlaceholder() {
    Surface(
        modifier     = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape        = RoundedCornerShape(28.dp),
        color        = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier            = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment   = Alignment.CenterVertically
        ) {
            Icon(
                imageVector        = Icons.Default.Search,
                contentDescription = "Search",
                tint               = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text  = "Search kitchens, cuisines…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Kitchen Card ──────────────────────────────────────────────────────────────

@Composable
fun KitchenCard(
    kitchen: Kitchen,
    isPaid: Boolean = false,
    onCardClick: () -> Unit,
    onSubscribe: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier  = modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Header row: name + rating badge ─────────────────────────────
            Row(
                modifier            = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment   = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text     = kitchen.name,
                        style    = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text  = kitchen.cuisineType,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                RatingBadge(rating = kitchen.rating)
            }

            Spacer(Modifier.height(10.dp))

            // ── Tags row: veg/non-veg + price ───────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VegTag(isVeg = kitchen.isVeg)
                Text(
                    text  = "₹${kitchen.pricePerTiffin} / tiffin",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color      = TiffinOrange
                    )
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Subscribe CTA ────────────────────────────────────────────────
            Button(
                onClick  = {
                    if (isPaid) {
                        Toast.makeText(
                            context,
                            "Plan Active: Daily meals are scheduled from ${kitchen.name}!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        onSubscribe()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = if (isPaid) VegGreen else TiffinOrange
                ),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text  = if (isPaid) "Subscribed ✓" else "Subscribe",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

// ── Rating Badge ──────────────────────────────────────────────────────────────

@Composable
private fun RatingBadge(rating: Double) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFFFF3E0)
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector        = Icons.Default.Star,
                contentDescription = "Rating",
                tint               = Color(0xFFFB8C00),
                modifier           = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text  = rating.toString(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFFE65100)
                )
            )
        }
    }
}

// ── Veg / Non-Veg Tag ─────────────────────────────────────────────────────────

@Composable
private fun VegTag(isVeg: Boolean) {
    val color = if (isVeg) VegGreen else NonVegRed
    val label = if (isVeg) "🟢 Veg" else "🔴 Non-Veg"
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text      = label,
            modifier  = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style     = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color      = color
            )
        )
    }
}

// ── Shimmer Loading ───────────────────────────────────────────────────────────

@Composable
private fun LoadingShimmer(modifier: Modifier = Modifier) {
    Column(
        modifier            = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(6) { ShimmerCard() }
    }
}

@Composable
private fun ShimmerCard() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by transition.animateFloat(
        initialValue   = 0.3f,
        targetValue    = 0.9f,
        animationSpec  = infiniteRepeatable(
            animation  = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = shimmerAlpha),
        Color.LightGray.copy(alpha = shimmerAlpha * 0.5f),
        Color.LightGray.copy(alpha = shimmerAlpha)
    )
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start  = Offset.Zero,
        end    = Offset(400f, 400f)
    )

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(modifier = Modifier.fillMaxWidth(0.6f).height(18.dp).clip(RoundedCornerShape(4.dp)).background(brush))
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(0.4f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(brush))
            Spacer(Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(12.dp)).background(brush))
        }
    }
}

// ── Empty State ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier            = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "🍽️", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text(
            text  = "No kitchens found",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text  = "Check back soon for delicious home-cooked meals!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Error State ───────────────────────────────────────────────────────────────

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier            = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "😕", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text(
            text  = "Oops! Something went wrong",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text  = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onRetry,
            colors  = ButtonDefaults.buttonColors(containerColor = TiffinOrange)
        ) {
            Text("Retry")
        }
    }
}
