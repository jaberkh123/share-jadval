import re

with open('app/src/main/java/com/example/ui/Screens.kt', 'r') as f:
    content = f.read()

pill_func = """
@Composable
fun CoinPill(coins: Int, onClick: () -> Unit) {
    val isDark = LocalDarkTheme.current
    androidx.compose.foundation.layout.Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
        modifier = androidx.compose.ui.Modifier
            .background(
                color = if (isDark) DarkSurface else androidx.compose.ui.graphics.Color(0xFFADE8F4),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
            )
            .border(
                1.5.dp,
                if (isDark) DarkPrimary else androidx.compose.ui.graphics.Color(0xFF00B4D8),
                androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        androidx.compose.material3.Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.Add,
            contentDescription = "کسب سکه",
            tint = if (isDark) DarkOnPrimary else androidx.compose.ui.graphics.Color(0xFF0096C7),
            modifier = androidx.compose.ui.Modifier.size(16.dp)
        )
        androidx.compose.material3.Text(
            text = coins.toString(),
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = if (isDark) DarkPrimary else androidx.compose.ui.graphics.Color(0xFF0077B6)
            )
        )
        androidx.compose.material3.Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.MonetizationOn,
            contentDescription = "سکه",
            tint = if (isDark) DarkOnPrimary else androidx.compose.ui.graphics.Color(0xFF0096C7),
            modifier = androidx.compose.ui.Modifier.size(18.dp)
        )
    }
}
"""

if "fun CoinPill(" not in content:
    content = content.replace("fun AppContent(", pill_func + "\nfun AppContent(")
    with open('app/src/main/java/com/example/ui/Screens.kt', 'w') as f:
        f.write(content)
    print("Added CoinPill")
else:
    print("Already exists")
