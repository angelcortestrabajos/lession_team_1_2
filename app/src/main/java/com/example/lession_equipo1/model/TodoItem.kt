package com.example.lession_equipo1.model

import kotlinx.serialization.Serializable

@Serializable
data class TodoItem(val id: Int, val name: String)
