package com.maksimowiczm.foodyou.fooddiary.domain.usecase

import com.maksimowiczm.foodyou.common.domain.date.DateProvider
import com.maksimowiczm.foodyou.fooddiary.domain.entity.FoodDiaryEntry
import com.maksimowiczm.foodyou.fooddiary.domain.entity.FoodDiaryEntryId
import com.maksimowiczm.foodyou.fooddiary.domain.entity.ManualDiaryEntry
import com.maksimowiczm.foodyou.fooddiary.domain.entity.ManualDiaryEntryId
import com.maksimowiczm.foodyou.fooddiary.domain.repository.FoodDiaryEntryRepository
import com.maksimowiczm.foodyou.fooddiary.domain.repository.ManualDiaryEntryRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * Result of copying entries to next day. Contains IDs of created entries for potential undo.
 */
data class CopyResult(
    val foodEntryIds: List<FoodDiaryEntryId>,
    val manualEntryIds: List<ManualDiaryEntryId>,
) {
    val totalCount: Int get() = foodEntryIds.size + manualEntryIds.size
    val isEmpty: Boolean get() = totalCount == 0
}

/**
 * Use case for copying meal entries to the next day.
 */
class CopyToNextDayUseCase(
    private val foodEntryRepository: FoodDiaryEntryRepository,
    private val manualEntryRepository: ManualDiaryEntryRepository,
    private val dateProvider: DateProvider,
) {
    /**
     * Copies all entries from a meal on a given date to the next day.
     *
     * @param mealId The ID of the meal to copy entries from.
     * @param sourceDate The date to copy entries from.
     * @return CopyResult containing IDs of created entries.
     */
    suspend fun copyMealToNextDay(mealId: Long, sourceDate: LocalDate): CopyResult {
        val nextDate = sourceDate.plus(1, DateTimeUnit.DAY)
        val createdAt = dateProvider.now()

        // Copy food entries
        val foodEntries = foodEntryRepository.observeAll(mealId, sourceDate).first()
        val foodEntryIds = foodEntries.map { entry ->
            foodEntryRepository.insert(
                measurement = entry.measurement,
                mealId = mealId,
                date = nextDate,
                food = entry.food,
                createdAt = createdAt,
            )
        }

        // Copy manual entries
        val manualEntries = manualEntryRepository.observeAll(mealId, sourceDate).first()
        val manualEntryIds = manualEntries.map { entry ->
            manualEntryRepository.insert(
                name = entry.name,
                mealId = mealId,
                date = nextDate,
                nutritionFacts = entry.nutritionFacts,
                createdAt = createdAt,
            )
        }

        return CopyResult(foodEntryIds, manualEntryIds)
    }

    /**
     * Copies a single food entry to the next day.
     *
     * @param entry The food entry to copy.
     * @param sourceDate The date to copy from.
     * @return CopyResult containing the ID of the created entry.
     */
    suspend fun copyFoodEntryToNextDay(entry: FoodDiaryEntry, sourceDate: LocalDate): CopyResult {
        val nextDate = sourceDate.plus(1, DateTimeUnit.DAY)
        val createdAt = dateProvider.now()

        val newId = foodEntryRepository.insert(
            measurement = entry.measurement,
            mealId = entry.mealId,
            date = nextDate,
            food = entry.food,
            createdAt = createdAt,
        )

        return CopyResult(foodEntryIds = listOf(newId), manualEntryIds = emptyList())
    }

    /**
     * Copies a single manual entry to the next day.
     *
     * @param entry The manual entry to copy.
     * @param sourceDate The date to copy from.
     * @return CopyResult containing the ID of the created entry.
     */
    suspend fun copyManualEntryToNextDay(
        entry: ManualDiaryEntry,
        sourceDate: LocalDate,
    ): CopyResult {
        val nextDate = sourceDate.plus(1, DateTimeUnit.DAY)
        val createdAt = dateProvider.now()

        val newId = manualEntryRepository.insert(
            name = entry.name,
            mealId = entry.mealId,
            date = nextDate,
            nutritionFacts = entry.nutritionFacts,
            createdAt = createdAt,
        )

        return CopyResult(foodEntryIds = emptyList(), manualEntryIds = listOf(newId))
    }

    /**
     * Undoes a copy operation by deleting all created entries.
     *
     * @param copyResult The result of the copy operation to undo.
     */
    suspend fun undoCopy(copyResult: CopyResult) {
        copyResult.foodEntryIds.forEach { id ->
            foodEntryRepository.delete(id)
        }
        copyResult.manualEntryIds.forEach { id ->
            manualEntryRepository.delete(id)
        }
    }
}
