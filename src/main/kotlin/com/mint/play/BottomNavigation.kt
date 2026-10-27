
package com.mint.play

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector

private data class NavItem(
    val label: String,
    val icon: ImageVector
)

private val navItems = listOf(
    NavItem("Video", VideoIcon),
    NavItem("Music", MusicIcon),
    NavItem("Tools", ToolsIcon),
    NavItem("Settings", SettingsIcon)
)

@Composable
fun BottomNavigation(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NavBackground)
            .padding(
                horizontal = 8.dp,
                vertical = 6.dp // CHANGE: Overall navbar top/bottom padding. Try 0.dp, 2.dp, 6.dp...
            ),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        navItems.forEachIndexed { index, item ->
            val selected = selectedTab == index
            val tint = if (selected) Accent else Inactive

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onTabSelected(index) }
                    .padding(
                        horizontal = 2.dp,
                        vertical = 3.dp // CHANGE: Padding inside each tab. Try 0.dp, 2.dp, 4.dp...
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(
                    0.dp // CHANGE: Gap between icon and label. Try 0.dp, 2.dp, 4.dp...
                )
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = tint,
                    modifier = Modifier.size(
                        22.dp // CHANGE: Icon size. Try 20.dp, 22.dp, 24.dp...
                    )
                )

                Text(
                    text = item.label,
                    color = tint,
                    fontSize = 10.sp, // CHANGE: Label text size. Try 9.sp, 10.sp, 11.sp...
                    fontWeight = if (selected) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Medium
                    },
                    maxLines = 1
                )
            }
        }
    }
}