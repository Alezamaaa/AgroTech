package mx.edu.agrotech.data

import java.time.LocalDate

/** Datos ficticios. No abre archivos, redes ni bases de datos. */
class DemoRepository(private val today: () -> LocalDate = { LocalDate.now() }) {
    private val animals = mutableListOf<Animal>()

    init {
        val date = today()
        animals += Animal("080", Sex.FEMALE, Category.EWES, "Dorper", "", date.minusYears(2),
            "Lactancia", "Corral 1", weights = listOf(Weighing(date.minusDays(31), 52.0), Weighing(date, 54.0)))
        animals += Animal("019", Sex.MALE, Category.RAMS, "Dorper", "", date.minusYears(3),
            "Semental", "Corral 2", weights = listOf(Weighing(date, 86.0)))
        animals += Animal("101", Sex.FEMALE, Category.LAMBS, "Dorper", "", date.minusDays(31),
            "Lactante", "Corral 1", "080", "019",
            listOf(Weighing(date.minusDays(31), 3.2), Weighing(date, 10.4)))
    }

    fun all(): List<Animal> = animals.toList()
    fun find(id: String): Animal? = animals.find { it.id == id }
    fun count(category: Category): Int = animals.count { it.category == category }

    fun add(id: String, sex: Sex, category: Category, breed: String, chip: String, birth: LocalDate): Animal {
        val cleanId = id.trim()
        require(cleanId.isNotEmpty() && cleanId.length <= 30) { "Escribe un arete de hasta 30 caracteres." }
        require(find(cleanId) == null) { "Ese arete ya está registrado en el rancho." }
        require(breed.isNotBlank()) { "Escribe la raza del animal." }
        require(!birth.isAfter(today())) { "El nacimiento no puede estar en el futuro." }
        require(category != Category.EWES || sex == Sex.FEMALE) { "La categoría Hembras requiere sexo Hembra." }
        require(category != Category.RAMS || sex == Sex.MALE) { "La categoría Sementales requiere sexo Macho." }
        return Animal(cleanId, sex, category, breed.trim(), chip.trim(), birth).also { animals += it }
    }

    fun addWeight(id: String, date: LocalDate, kg: Double) {
        val animal = requireNotNull(find(id)) { "No se encontró el animal." }
        require(!date.isBefore(animal.birth) && !date.isAfter(today())) { "Elige una fecha entre el nacimiento y hoy." }
        require(kg.isFinite() && kg > 0 && kg <= 500) { "Captura un peso mayor a 0 y de hasta 500 kg para esta demostración." }
        require(animal.weights.none { it.date == date }) { "Ya hay un pesaje en esa fecha. Elige otra fecha." }
        animal.record(Weighing(date, kg))
    }
}
