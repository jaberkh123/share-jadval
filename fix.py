with open('app/src/main/java/com/example/ui/Screens.kt', 'r') as f:
    content = f.read()
    
# Find the start of the corruption
start_str = "                    val showDirectionPicker = (showDirectionPickerState || directionPickerAlpha.value > 0.05f) && activeRow != -1 && activeCol != -1"
end_str = """                                .height(pickerHeight)
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {"""
start_idx = content.find(start_str)
end_idx = content.find(end_str, start_idx) + len(end_str)

replacement = """                    val showDirectionPicker = (showDirectionPickerState || directionPickerAlpha.value > 0.05f) && activeRow != -1 && activeCol != -1
                    if (showDirectionPicker) {
                        val r = activeRow
                        val c = activeCol
                        
                        // Design a glassy, larger direction picker
                        val pickerWidth = 120.dp
                        val pickerHeight = 40.dp
                        val padding = 4.dp
                        val isAcross = viewModel.activeDirection == "across"
                        
                        var rawX: androidx.compose.ui.unit.Dp
                        var rawY: androidx.compose.ui.unit.Dp
                        
                        if (isAcross) {
                            // Active line is horizontal. Place picker above or below the cell.
                            val cellCenter = (cellSize * c) + (cellSize / 2f)
                            rawX = cellCenter - (pickerWidth / 2f)
                            
                            rawY = if (isMinimapMode) {
                                if (r >= rows / 2) {
                                    (cellSize * r) - pickerHeight - 8.dp
                                } else {
                                    (cellSize * (r + 1)) + 8.dp
                                }
                            } else {
                                if (r >= 2) {
                                    (cellSize * r) - pickerHeight - 8.dp
                                } else {
                                    (cellSize * (r + 1)) + 8.dp
                                }
                            }
                        } else {
                            // Active line is vertical. Place picker to the left or right of the cell.
                            val cellCenter = (cellSize * r) + (cellSize / 2f)
                            rawY = cellCenter - (pickerHeight / 2f)
                            
                            rawX = if (isMinimapMode) {
                                if (c >= cols / 2) {
                                    (cellSize * c) - pickerWidth - 8.dp
                                } else {
                                    (cellSize * (c + 1)) + 8.dp
                                }
                            } else {
                                if (c >= 2) {
                                    (cellSize * c) - pickerWidth - 8.dp
                                } else {
                                    (cellSize * (c + 1)) + 8.dp
                                }
                            }
                        }
                        
                        val clampedXOffset = rawX.coerceIn(padding, gridWidth - pickerWidth - padding)
                        val clampedYOffset = rawY.coerceIn(padding, gridHeight - pickerHeight - padding)

                        Box(
                            modifier = Modifier
                                .offset(x = clampedXOffset, y = clampedYOffset)
                                .graphicsLayer(alpha = directionPickerAlpha.value)
                                .shadow(
                                    elevation = 10.dp,
                                    shape = RoundedCornerShape(24.dp),
                                    clip = false
                                )
                                .background(
                                    color = if (LocalDarkTheme.current) DarkSurface.copy(alpha=0.95f) else Color(0xF2ADE8F4), // Glassy Slate look
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .border(
                                    width = 1.2.dp,
                                    color = if (LocalDarkTheme.current) Color(0x33CAF0F8) else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .width(pickerWidth)
                                .height(pickerHeight)
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {"""
                        
new_content = content[:start_idx] + replacement + content[end_idx:]

with open('app/src/main/java/com/example/ui/Screens.kt', 'w') as f:
    f.write(new_content)
