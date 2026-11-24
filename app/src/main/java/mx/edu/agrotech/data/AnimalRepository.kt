package mx.edu.agrotech.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.auth.auth
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mx.edu.agrotech.config.SupabaseClientProvider
import mx.edu.agrotech.models.AnimalSupabase
import mx.edu.agrotech.models.Raza
import mx.edu.agrotech.models.PesajeSupabase
import io.github.jan.supabase.postgrest.query.Order

@Serializable
data class NuevoAnimal(
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

    val observaciones: String? = null,

    @SerialName("creado_por")
    val creadoPor: String? = null
)

@Serializable
data class NuevoPesaje(
    @SerialName("animal_id")
    val animalId: String,

    @SerialName("rancho_id")
    val ranchoId: String,

    val fecha: String,

    @SerialName("peso_kg")
    val pesoKg: Double,

    val observaciones: String? = null,

    @SerialName("registrado_por")
    val registradoPor: String
)

class AnimalRepository {

    private val client = SupabaseClientProvider.client

    // Consultar animales de un rancho
    suspend fun obtenerAnimales(ranchoId: String): List<AnimalSupabase> {
        return client.from("animales")
            .select {
                filter {
                    eq("rancho_id", ranchoId)
                }
            }
            .decodeList<AnimalSupabase>()
    }

    // Consultar catálogo de razas
    suspend fun obtenerRazas(): List<Raza> {
        return client.from("razas")
            .select()
            .decodeList<Raza>()
    }

    // Registrar un animal
    suspend fun registrarAnimal(
        ranchoId: String,
        arete: String,
        sexo: String,
        siniiga: String? = null,
        razaId: Long? = null,
        fechaNacimiento: String? = null,
        observaciones: String? = null
    ) {
        require(arete.isNotBlank()) {
            "El arete es obligatorio."
        }

        require(sexo == "H" || sexo == "M") {
            "El sexo debe ser H o M."
        }

        val usuarioId = client.auth.currentUserOrNull()?.id
            ?: throw Exception("No hay una sesión activa.")

        val nuevoAnimal = NuevoAnimal(
            ranchoId = ranchoId,
            arete = arete.trim(),
            sexo = sexo,
            siniiga = siniiga?.trim()?.takeIf { it.isNotEmpty() },
            razaId = razaId,
            fechaNacimiento = fechaNacimiento,
            observaciones = observaciones,
            creadoPor = usuarioId
        )

        client.from("animales").insert(nuevoAnimal)
    }

    // Consultar historial de pesajes de un animal
    suspend fun obtenerPesajes(animalId: String): List<PesajeSupabase> {
        return client.from("pesajes")
            .select {
                filter {
                    eq("animal_id", animalId)
                }
                order("fecha", Order.DESCENDING)
            }
            .decodeList<PesajeSupabase>()
    }

    // Registrar un nuevo pesaje
    suspend fun registrarPesaje(
        animalId: String,
        ranchoId: String,
        fecha: String,
        pesoKg: Double,
        observaciones: String? = null
    ) {
        require(pesoKg > 0) {
            "El peso debe ser mayor que cero."
        }

        val usuarioId = client.auth.currentUserOrNull()?.id
            ?: throw Exception("No hay una sesión activa.")

        val nuevoPesaje = NuevoPesaje(
            animalId = animalId,
            ranchoId = ranchoId,
            fecha = fecha,
            pesoKg = pesoKg,
            observaciones = observaciones,
            registradoPor = usuarioId
        )

        client.from("pesajes").insert(nuevoPesaje)
    }

}
