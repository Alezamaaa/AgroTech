package mx.edu.agrotech.data

import io.github.jan.supabase.postgrest.from
import mx.edu.agrotech.config.SupabaseClientProvider
import mx.edu.agrotech.models.Perfil
import mx.edu.agrotech.models.Rancho
import io.github.jan.supabase.auth.auth
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class DatosUsuario(
    val perfil: Perfil,
    val rancho: Rancho?
)

@Serializable
data class NuevoRancho(
    @SerialName("usuario_id")
    val usuarioId: String,

    val nombre: String,
    val localidad: String? = null,
    val municipio: String? = null,

    @SerialName("estado_region")
    val estadoRegion: String? = null
)

@Serializable
data class ActualizacionRancho(
    val nombre: String,
    val localidad: String,
    val municipio: String,
    @SerialName("estado_region")
    val estadoRegion: String
)
class SupabaseRepository {

    private val client = SupabaseClientProvider.client

    suspend fun obtenerDatosUsuario(): DatosUsuario? {

        // 1. Obtener el usuario autenticado
        val usuarioId = client.auth.currentUserOrNull()?.id
            ?: return null

        // 2. Consultar su perfil
        val perfil = client.from("perfiles")
            .select {
                filter {
                    eq("id", usuarioId)
                }
            }
            .decodeList<Perfil>()
            .firstOrNull()
            ?: return null

        // 3. Consultar su rancho
        val rancho = client.from("ranchos")
            .select {
                filter {
                    eq("usuario_id", usuarioId)
                }
            }
            .decodeList<Rancho>()
            .firstOrNull()

        // 4. Devolver perfil y rancho
        return DatosUsuario(
            perfil = perfil,
            rancho = rancho
        )
    }
    suspend fun registrarRancho(
        nombre: String,
        localidad: String?,
        municipio: String?,
        estadoRegion: String?
    ) {
        val usuarioId = client.auth.currentUserOrNull()?.id
            ?: throw Exception("No hay una sesión activa.")

        val nuevoRancho = NuevoRancho(
            usuarioId = usuarioId,
            nombre = nombre,
            localidad = localidad,
            municipio = municipio,
            estadoRegion = estadoRegion
        )

        client.from("ranchos").insert(nuevoRancho)
    }

    suspend fun actualizarRancho(
        nombre: String,
        localidad: String,
        municipio: String,
        estadoRegion: String
    ) {
        val usuarioId = client.auth.currentUserOrNull()?.id
            ?: throw Exception("No hay una sesión activa.")

        val datos = ActualizacionRancho(
            nombre = nombre,
            localidad = localidad,
            municipio = municipio,
            estadoRegion = estadoRegion
        )

        client.from("ranchos").update(datos) {
            filter {
                eq("usuario_id", usuarioId)
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
