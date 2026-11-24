package mx.edu.agrotech.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Raza(
    @SerialName("raza_id")
    val razaId: Long,
    val nombre: String,
    val tipo: String
)
