package com.draindetective.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draindetective.ui.theme.AmberGold
import com.draindetective.ui.theme.BgDark
import com.draindetective.ui.theme.CardBorder
import com.draindetective.ui.theme.CardElevated
import com.draindetective.ui.theme.FlameOrange
import com.draindetective.ui.theme.FlameRed
import com.draindetective.ui.theme.TealNeon
import com.draindetective.ui.theme.TextMuted
import com.draindetective.ui.theme.TextPrimary
import com.draindetective.ui.theme.TextSecondary
import com.draindetective.usage.SuspectApp
import com.draindetective.usage.UsageTracker

@Composable
fun SuspectLineupSection(
    suspects: List<SuspectApp>,
    hasUsagePermission: Boolean,
    onGrantPermissionClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🕵️ Suspect Lineup (Foreground Apps)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            if (hasUsagePermission && suspects.isNotEmpty()) {
                Text(
                    text = "Top Active",
                    fontSize = 11.sp,
                    color = TealNeon,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        if (!hasUsagePermission) {
            // Friendly Explainer Card for Usage Access
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardElevated, RoundedCornerShape(20.dp))
                    .border(1.dp, AmberGold.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔑", fontSize = 24.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Detective Needs Usage Access",
                            color = AmberGold,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Android protects foreground app statistics. To identify which specific apps were consuming power during this drain hour, grant Usage Access in system settings.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                    Button(
                        onClick = onGrantPermissionClicked,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberGold,
                            contentColor = BgDark
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Grant Usage Access", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        } else if (suspects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardElevated, RoundedCornerShape(16.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "No prominent foreground apps detected during this specific hour interval.",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                suspects.forEachIndexed { index, app ->
                    SuspectItemRow(app = app, rank = index + 1)
                }
            }
        }
    }
}

@Composable
fun SuspectItemRow(app: SuspectApp, rank: Int) {
    val bitmap = remember(app.icon) {
        app.icon?.let { drawableToBitmap(it) }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardElevated, RoundedCornerShape(16.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank badge
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    if (rank == 1) FlameRed.copy(alpha = 0.2f) else CardBorder,
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "#$rank",
                color = if (rank == 1) FlameRed else TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.width(12.dp))

        // App Icon
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = app.appName,
                modifier = Modifier.size(36.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(CardBorder, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("📱", fontSize = 18.sp)
            }
        }

        Spacer(Modifier.width(12.dp))

        // App Details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.appName,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = app.packageName,
                color = TextMuted,
                fontSize = 10.sp
            )
        }

        // Active minutes
        Text(
            text = "${app.foregroundMinutes} min",
            color = FlameOrange,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun drawableToBitmap(drawable: Drawable): Bitmap {
    val width = drawable.intrinsicWidth.coerceAtLeast(1).coerceAtMost(128)
    val height = drawable.intrinsicHeight.coerceAtLeast(1).coerceAtMost(128)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
