package com.maksimowiczm.foodyou.app.ui.home.meals.card

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maksimowiczm.foodyou.app.ui.home.shared.HomeState
import com.maksimowiczm.foodyou.fooddiary.domain.entity.MealsCardsLayout
import foodyou.app.generated.resources.Res
import foodyou.app.generated.resources.action_undo
import foodyou.app.generated.resources.message_copied_to_tomorrow
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun MealsCards(
    homeState: HomeState,
    onAdd: (epochDay: Long, mealId: Long) -> Unit,
    onQuickAdd: (epochDay: Long, mealId: Long) -> Unit,
    onEditEntry: (foodEntryId: Long?, manualEntryId: Long?) -> Unit,
    onLongClick: (mealId: Long) -> Unit,
    contentPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val viewModel: MealsCardsViewModel = koinViewModel()
    val diaryMeals = viewModel.diaryMeals.collectAsStateWithLifecycle().value
    val layout by viewModel.layout.collectAsStateWithLifecycle()

    val copiedMessage = stringResource(Res.string.message_copied_to_tomorrow)
    val undoLabel = stringResource(Res.string.action_undo)

    LaunchedEffect(viewModel) {
        viewModel.copyEvent.collectLatest { event ->
            when (event) {
                is CopyEvent.Copied -> {
                    val result = snackbarHostState.showSnackbar(
                        message = copiedMessage,
                        actionLabel = undoLabel,
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onUndoCopy(event.result)
                    }
                }
            }
        }
    }

    LaunchedEffect(homeState.selectedDate, viewModel) { viewModel.setDate(homeState.selectedDate) }

    when (layout) {
        MealsCardsLayout.Horizontal ->
            HorizontalMealsCards(
                meals = diaryMeals,
                onAdd = { mealId -> onAdd(homeState.selectedDate.toEpochDays(), mealId) },
                onQuickAdd = { mealId -> onQuickAdd(homeState.selectedDate.toEpochDays(), mealId) },
                onEditEntry = { model ->
                    val foodEntry = model as? FoodMealEntryModel
                    val manualEntry = model as? ManualMealEntryModel
                    onEditEntry(foodEntry?.id?.value, manualEntry?.id?.value)
                },
                onDeleteEntry = viewModel::onDeleteEntry,
                onCopyMeal = viewModel::onCopyMealToNextDay,
                onCopyEntry = viewModel::onCopyEntryToNextDay,
                onLongClick = onLongClick,
                shimmer = homeState.shimmer,
                contentPadding = contentPadding,
                modifier = modifier,
            )

        MealsCardsLayout.Vertical ->
            VerticalMealsCards(
                meals = diaryMeals,
                onAdd = { mealId -> onAdd(homeState.selectedDate.toEpochDays(), mealId) },
                onQuickAdd = { mealId -> onQuickAdd(homeState.selectedDate.toEpochDays(), mealId) },
                onEditEntry = { model ->
                    val foodEntry = model as? FoodMealEntryModel
                    val manualEntry = model as? ManualMealEntryModel
                    onEditEntry(foodEntry?.id?.value, manualEntry?.id?.value)
                },
                onDeleteEntry = viewModel::onDeleteEntry,
                onCopyMeal = viewModel::onCopyMealToNextDay,
                onCopyEntry = viewModel::onCopyEntryToNextDay,
                onLongClick = onLongClick,
                shimmer = homeState.shimmer,
                contentPadding = contentPadding,
                modifier = modifier,
            )
    }
}

