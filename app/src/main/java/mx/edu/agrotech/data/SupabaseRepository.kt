package mx.edu.agrotech.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mx.edu.agrotech.config.SupabaseClientProvider
import mx.edu.agrotech.models.Perfil
import mx.edu.agrotech.models.Rancho

data class DatosUsuario(
    val perfil: Perfil,
    val ranchos: List<Rancho>
)

@Serializable
data class NuevoRancho(
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
    val unidadPeso: String = "kg"
)

@Serializable
data class ActualizacionRancho(
    val nombre: String,
    val localidad: String? = null,
    val municipio: String? = null,
    val estado: String? = null,
    val pais: String = "México",
    val direccion: String? = null,

    @SerialName("zona_horaria")
    val zonaHoraria: String = "America/Mexico_City",

    @SerialName("moneda_preferida")
    val monedaPreferida: String = "MXN"
)

class SupabaseRepository {

    private val client = SupabaseClientProvider.client

    private fun obtenerUsuarioId(): String {
        return client.auth.currentUserOrNull()?.id
            ?: throw Exception("No hay una sesión activa.")
    }

    suspend fun obtenerDatosUsuario(): DatosUsuario? {

        val usuarioId = client.auth.currentUserOrNull()?.id
            ?: return null

        val perfil = client.from("perfiles")
            .select {
                filter {
                    eq("perfil_id", usuarioId)
                }
            }
            .decodeList<Perfil>()
            .firstOrNull()
            ?: return null

        val ranchos = client.from("ranchos")
            .select {
                filter {
                    eq("propietario_id", usuarioId)
                }
            }
            .decodeList<Rancho>()

        return DatosUsuario(
            perfil = perfil,
            ranchos = ranchos
        )
    }

    suspend fun obtenerRanchos(): List<Rancho> {

        val usuarioId = obtenerUsuarioId()

        return client.from("ranchos")
            .select {
                filter {
                    eq("propietario_id", usuarioId)
                }
            }
            .decodeList<Rancho>()
    }

    // Consultar todos los ranchos para el panel de asociación
    suspend fun obtenerTodosLosRanchos(): List<Rancho> {
        return client.from("ranchos")
            .select()
            .decodeList<Rancho>()
    }
    suspend fun registrarRancho(
        nombre: String,
        localidad: String?,
        municipio: String?,
        estado: String?,
        direccion: String? = null,
        monedaPreferida: String = "MXN"
    ) {

        val usuarioId = obtenerUsuarioId()

        val nuevoRancho = NuevoRancho(
            propietarioId = usuarioId,
            nombre = nombre,
            localidad = localidad,
            municipio = municipio,
            estado = estado,
            direccion = direccion,
            monedaPreferida = monedaPreferida
        )

        client.from("ranchos").insert(nuevoRancho)
    }

    suspend fun actualizarRancho(
        ranchoId: String,
        nombre: String,
        localidad: String?,
        municipio: String?,
        estado: String?,
        direccion: String? = null,
        monedaPreferida: String = "MXN"
    ) {

        val usuarioId = obtenerUsuarioId()

        val datos = ActualizacionRancho(
            nombre = nombre,
            localidad = localidad,
            municipio = municipio,
            estado = estado,
            direccion = direccion,
            monedaPreferida = monedaPreferida
        )

        client.from("ranchos").update(datos) {
            filter {
                eq("rancho_id", ranchoId)
                eq("propietario_id", usuarioId)
            }
        }
    }

    suspend fun cambiarContrasena(
        contrasenaActual: String,
        nuevaContrasena: String
    ) {

        client.auth.updateUser {
            password = nuevaContrasena
            currentPassword = contrasenaActual
        }
    }

}

