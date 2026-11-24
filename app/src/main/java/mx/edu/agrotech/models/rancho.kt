
package mx.edu.agrotech.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Rancho(
    @SerialName("rancho_id")
    val ranchoId: String,

    @SerialName("propietario_id")
    val propietarioId: String,

    val nombre: String,
    val localidad: String? = null,
    val municipio: String? = null,
    val estado: String? = null,

    val pais: String = "México",
    val direccion: String? = null,

    @SerialName("zona_horaria")
    val zonaHoraria: String = "America/Mexico_City",

    @SerialName("moneda_preferida")
    val monedaPreferida: String = "MXN",

    @SerialName("unidad_peso")
    val unidadPeso: String = "kg",

    val activo: Boolean = true,

    @SerialName("creado_en")
    val creadoEn: String? = null
)
