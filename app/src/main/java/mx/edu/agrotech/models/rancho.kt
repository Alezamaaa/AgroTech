package mx.edu.agrotech.models

import kotlinx.serialization.Serializable

@Serializable
data class Rancho(
    val id: String,

    val usuario_id: String,

    val nombre: String,

    val localidad: String? = null,

    val municipio: String? = null,

    val estado_region: String? = null
)
