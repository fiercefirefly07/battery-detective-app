package com.draindetective.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draindetective.domain.ContextDrainRate
import com.draindetective.domain.DeviceContext
import com.draindetective.ui.theme.AmberGold
import com.draindetective.ui.theme.BlueAccent
import com.draindetective.ui.theme.CardBorder
import com.draindetective.ui.theme.CardElevated
import com.draindetective.ui.theme.FlameOrange
import com.draindetective.ui.theme.FlameRed
import com.draindetective.ui.theme.PurpleAccent
import com.draindetective.ui.theme.TealNeon
import com.draindetective.ui.theme.TextMuted
import com.draindetective.ui.theme.TextPrimary
import com.draindetective.ui.theme.TextSecondary

@Composable
fun ContextDrainChart(
    rates: List<ContextDrainRate>,
    modifier: Modifier = Modifier
) {
    val maxRate = (rates.maxOfOrNull { it.ratePerHour } ?: 1f).coerceAtLeast(1f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CardElevated, RoundedCornerShape(24.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text(
                text = "Context Fingerprint Drain Rates",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Where was your phone when battery was drained?",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        if (rates.isEmpty() || rates.all { it.ratePerHour == 0f }) {
            Text(
                text = "No situational drain patterns recorded yet. As your phone samples desk, pocket, and moving states, rates appear here.",
                color = TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rates.forEach { item ->
                    ContextRateRow(item = item, maxRate = maxRate)
                }
            }
        }
    }
}

@Composable
private fun ContextRateRow(
    item: ContextDrainRate,
    maxRate: Float
) {
    val color = when (item.context) {
        DeviceContext.IN_POCKET_OR_BAG -> FlameRed
        DeviceContext.MOVING -> FlameOrange
        DeviceContext.IN_USE -> PurpleAccent
        DeviceContext.ON_DESK -> TealNeon
        DeviceContext.CHARGING -> AmberGold
        DeviceContext.UNKNOWN -> BlueAccent
    }

    val progress by animateFloatAsState(
        targetValue = (item.ratePerHour / maxRate).coerceIn(0.04f, 1f),
        animationSpec = tween(700),
        label = "contextBar"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.context.emoji, fontSize = 16.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = item.context.displayName,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "${"%.1f".format(item.ratePerHour)}%/hr",
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(5.dp))

        // Bar container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(CardBorder.copy(alpha = 0.5f), RoundedCornerShape(50))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(color, RoundedCornerShape(50))
            )
        }

        Spacer(Modifier.height(3.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${item.sampleCount} snapshots recorded",
                color = TextMuted,
                fontSize = 10.sp
            )
            Text(
                text = "-${item.totalDrop}% total drop",
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
}
