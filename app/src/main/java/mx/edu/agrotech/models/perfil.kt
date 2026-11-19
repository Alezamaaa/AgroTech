package mx.edu.agrotech.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Perfil(
    val id: String,

    @SerialName("nombre_completo")
    val nombreCompleto: String,

    val rol: String
)
