package mx.edu.agrotech.data

import java.time.LocalDate

enum class Sex(val label: String) { FEMALE("Hembra"), MALE("Macho") }
enum class Category(val label: String) { LAMBS("Crías"), EWES("Hembras"), RAMS("Sementales") }
data class Weighing(val date: LocalDate, val kg: Double)

class Animal(
    val id: String,
    val sex: Sex,
    val category: Category,
    val breed: String,
    val chip: String,
    val birth: LocalDate,
    val stage: String = "Sin asignar",
    val pen: String = "Sin asignar",
    val motherId: String? = null,
    val fatherId: String? = null,
    weights: List<Weighing> = emptyList(),
) {
    private val history = weights.toMutableList()
    val weights: List<Weighing> get() = history.toList()
    internal fun record(weighing: Weighing) {
        history.add(weighing)
        history.sortBy { it.date }
    }
}
