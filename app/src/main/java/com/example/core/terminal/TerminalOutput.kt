package com.example.core.terminal

data class TerminalLine(
    val text: String,
    val type: LineType = LineType.OUTPUT
)

enum class LineType {
    COMMAND,
    OUTPUT,
    ERROR,
    SUCCESS,
    SYSTEM,
    WARNING
}
