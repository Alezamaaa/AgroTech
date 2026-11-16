package mx.edu.agrotech.data

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class DemoRepositoryTest {
    private val today = LocalDate.of(2026, 11, 15)
    private fun repository() = DemoRepository { today }

    @Test fun familyReferencesResolveAndInventoryHasThreeAnimals() {
        val repo = repository()
        val child = repo.find("101")!!
        assertEquals(3, repo.all().size)
        assertEquals(Sex.FEMALE, repo.find(child.motherId!!)!!.sex)
        assertEquals(Category.RAMS, repo.find(child.fatherId!!)!!.category)
        assertEquals(1, repo.count(Category.EWES))
    }

    @Test fun addsAnAnimalAndUpdatesCounts() {
        val repo = repository()
        val added = repo.add(" 102 ", Sex.MALE, Category.LAMBS, " Dorper ", "", today.minusDays(5))
        assertEquals("102", added.id)
        assertEquals("Dorper", added.breed)
        assertEquals(4, repo.all().size)
        assertEquals(2, repo.count(Category.LAMBS))
    }

    @Test fun duplicateAreteCannotModifyInventory() {
        val repo = repository()
        assertThrows(IllegalArgumentException::class.java) {
            repo.add(" 080 ", Sex.FEMALE, Category.EWES, "Dorper", "", today)
        }
        assertEquals(3, repo.all().size)
    }

    @Test fun rejectsFutureBirthAndSexCategoryMismatch() {
        val repo = repository()
        assertThrows(IllegalArgumentException::class.java) {
            repo.add("102", Sex.FEMALE, Category.LAMBS, "Dorper", "", today.plusDays(1))
        }
        assertThrows(IllegalArgumentException::class.java) {
            repo.add("102", Sex.FEMALE, Category.RAMS, "Dorper", "", today)
        }
        assertThrows(IllegalArgumentException::class.java) {
            repo.add("102", Sex.MALE, Category.EWES, "Dorper", "", today)
        }
    }

    @Test fun weightHistorySortsByDateInsteadOfEntryOrder() {
        val repo = repository()
        repo.addWeight("101", today.minusDays(10), 8.5)
        repo.addWeight("101", today.minusDays(20), 5.5)
        val history = repo.find("101")!!.weights
        assertEquals(listOf(3.2, 5.5, 8.5, 10.4), history.map { it.kg })
    }

    @Test fun rejectsInvalidWeightsAndDatesWithoutChangingHistory() {
        val repo = repository()
        val invalidValues = listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, 501.0)
        invalidValues.forEach { amount ->
            assertThrows(IllegalArgumentException::class.java) { repo.addWeight("101", today.minusDays(1), amount) }
        }
        listOf(today.plusDays(1), today.minusDays(32), today).forEach { date ->
            assertThrows(IllegalArgumentException::class.java) { repo.addWeight("101", date, 5.0) }
        }
        assertEquals(2, repo.find("101")!!.weights.size)
    }

    @Test fun returnedCollectionsCannotMutateRepository() {
        val repo = repository()
        val snapshot = repo.all().toMutableList()
        snapshot.clear()
        val weights = repo.find("101")!!.weights.toMutableList()
        weights.clear()
        assertEquals(3, repo.all().size)
        assertEquals(2, repo.find("101")!!.weights.size)
    }
}
