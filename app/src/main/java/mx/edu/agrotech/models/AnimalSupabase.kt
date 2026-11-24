package mx.edu.agrotech.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnimalSupabase(
    @SerialName("animal_id")
    val animalId: String,

    @SerialName("rancho_id")
    val ranchoId: String,

    val arete: String,
    val siniiga: String? = null,

    @SerialName("registro_genealogico")
    val registroGenealogico: String? = null,

    val sexo: String,

    @SerialName("raza_id")
    val razaId: Long? = null,

    @SerialName("fecha_nacimiento")
    val fechaNacimiento: String? = null,

    @SerialName("madre_id")
    val madreId: String? = null,

    @SerialName("padre_id")
    val padreId: String? = null,

    @SerialName("madre_externa_descripcion")
    val madreExternaDescripcion: String? = null,

    @SerialName("padre_externo_descripcion")
    val padreExternoDescripcion: String? = null,

    @SerialName("padre_desconocido")
    val padreDesconocido: Boolean = false,

    @SerialName("corral_id")
    val corralId: String? = null,

    val estado: String = "vivo",

    @SerialName("motivo_ingreso")
    val motivoIngreso: String = "nacimiento",

    @SerialName("fecha_ingreso")
    val fechaIngreso: String? = null,

    @SerialName("fecha_baja")
    val fechaBaja: String? = null,

    @SerialName("causa_baja")
    val causaBaja: String? = null,

    val observaciones: String? = null,

    @SerialName("foto_url")
    val fotoUrl: String? = null,

    @SerialName("creado_por")
    val creadoPor: String? = null,

    @SerialName("creado_en")
    val creadoEn: String? = null
)
