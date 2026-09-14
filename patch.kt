                                                onCellClicked(row, col)
                                                lastClickTime = System.currentTimeMillis()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Active cell click highlight flash overlay moved outside the loops to avoid high-frequency recomposition of the full grid
                    if (activeRow != -1 && activeCol != -1 && !isMinimapMode && clickHighlightAlpha.value > 0.01f) {
                        val flashX = cellSize * activeCol
                        val flashY = cellSize * activeRow
                        Box(
                            modifier = Modifier
                                .offset(x = flashX, y = flashY)
                                .size(cellSize)
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                        colors = listOf(
                                            androidx.compose.ui.graphics.Color(0xFF0096C7).copy(alpha = clickHighlightAlpha.value),
                                            androidx.compose.ui.graphics.Color(0xFF0096C7).copy(alpha = clickHighlightAlpha.value * 0.4f),
                                            androidx.compose.ui.graphics.Color.Transparent
                                        )
                                    )
                                )
                        )
                    }

                    val showDirectionPicker = (showDirectionPickerState || directionPickerAlpha.value > 0.05f) && activeRow != -1 && activeCol != -1
                    if (showDirectionPicker) {
                        val r = activeRow
                        val c = activeCol
