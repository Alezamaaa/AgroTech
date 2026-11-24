package mx.edu.agrotech.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PesajeSupabase(
    @SerialName("pesaje_id")
    val pesajeId: String,

    @SerialName("animal_id")
    val animalId: String,

    @SerialName("rancho_id")
    val ranchoId: String,

    val fecha: String,

    @SerialName("peso_kg")
    val pesoKg: Double,

    val observaciones: String? = null,

    @SerialName("registrado_por")
    val registradoPor: String? = null,

    @SerialName("creado_en")
    val creadoEn: String? = null
)
