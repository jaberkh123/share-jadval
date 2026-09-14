import re

with open("app/src/main/java/com/example/ui/Screens.kt", "r") as f:
    content = f.read()

target = """                        if (isAcross) {
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
                        }"""

replacement = """                        if (isAcross) {
                            // Active line is horizontal. Place picker above or below the cell.
                            val cellCenter = (cellSize * c) + (cellSize / 2f)
                            rawX = cellCenter - (pickerWidth / 2f)
                            
                            // Check if there is enough space above, otherwise put below
                            val spaceAbove = (cellSize * r)
                            if (spaceAbove >= pickerHeight + 12.dp) {
                                rawY = (cellSize * r) - pickerHeight - 8.dp
                            } else {
                                rawY = (cellSize * (r + 1)) + 8.dp
                            }
                        } else {
                            // Active line is vertical. Place picker to the left or right of the cell.
                            val cellCenter = (cellSize * r) + (cellSize / 2f)
                            rawY = cellCenter - (pickerHeight / 2f)
                            
                            // Check if there is enough space to the left, otherwise put right
                            val spaceLeft = (cellSize * c)
                            val spaceRight = gridWidth - (cellSize * (c + 1))
                            
                            if (spaceLeft >= pickerWidth + 12.dp) {
                                rawX = spaceLeft - pickerWidth - 8.dp
                            } else if (spaceRight >= pickerWidth + 12.dp) {
                                rawX = (cellSize * (c + 1)) + 8.dp
                            } else {
                                // Not enough space on either side (very narrow grid), just offset it slightly and let it overlap partially
                                rawX = spaceLeft - (pickerWidth / 2f) + (cellSize / 2f)
                            }
                        }"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/Screens.kt", "w") as f:
    f.write(content)

