package com.example.core.virtualization

enum class InstanceStatus(val label: String) {
    STOPPED("Stopped"),
    STARTING("Booting"),
    RUNNING("Running"),
    PAUSED("Paused"),
    ERROR("Error")
}
