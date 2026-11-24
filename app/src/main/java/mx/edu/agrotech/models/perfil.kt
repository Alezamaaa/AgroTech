
package mx.edu.agrotech.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Perfil(
    @SerialName("perfil_id")
    val perfilId: String,

    @SerialName("nombre_completo")
    val nombreCompleto: String,

    val correo: String,
    val rol: String,

    @SerialName("creado_en")
    val creadoEn: String? = null
)
