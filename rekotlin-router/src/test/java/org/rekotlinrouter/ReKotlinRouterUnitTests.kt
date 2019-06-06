package org.rekotlinrouter


import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class ReKotlinRouterSpec {

    private val mainSegmentId = "MainSegment"
    private val counterSegmentId = "CounterSegment"
    private val statsSegmentId = "StatsSegment"
    private val infoSegmentId = "InfoSegment"

    @Test
    fun `should calculate transitions from an empty route to a multi segment route`() {

        // Given
        val oldRoute: Route = emptyList()
        val newRoute = simpleRoute(mainSegmentId, statsSegmentId)

        // When
        val routingActions = Router.routingActionsForTransitionFrom(oldRoute, newRoute)

        // Then
        var action1Correct = false
        var action2Correct = false

        routingActions.forEach { routingAction ->
            when (routingAction) {
                is push -> {
                    if (routingAction.responsibleRoutableIndex == 0 && routingAction.segmentToBePushed.id == mainSegmentId) {
                        action1Correct = true
                    }
                    if (routingAction.responsibleRoutableIndex == 1 && routingAction.segmentToBePushed.id == statsSegmentId) {
                        action2Correct = true
                    }
                }
            }
        }
        assertThat(action1Correct).isTrue()
        assertThat(action2Correct).isTrue()
        assertThat(routingActions.count()).isEqualTo(2)
    }

    @Test
    fun `should generate a change action on the last common subroute`() {

        // Given
        val oldRoute = simpleRoute(mainSegmentId, counterSegmentId)
        val newRoute = simpleRoute(mainSegmentId, statsSegmentId)

        // When
        val routingActions = Router.routingActionsForTransitionFrom(oldRoute, newRoute)

        // Then
        var controllerIndex: Int = -1
        var toBeReplaced = ""
        var new = ""
        routingActions.forEach { routingAction ->

            when (routingAction) {
                is change -> {
                    controllerIndex = routingAction.responsibleRoutableIndex
                    toBeReplaced = routingAction.segmentToBeReplaced.id
                    new = routingAction.newSegment.id
                }
            }
        }
        assertThat(controllerIndex).isEqualTo(1)
        assertThat(toBeReplaced).isEqualTo(counterSegmentId)
        assertThat(new).isEqualTo(statsSegmentId)
    }

    @Test
    fun `should generate a Change action on the last common subroute, also for routes of different length`() {

        // Given
        val oldRoute = simpleRoute(mainSegmentId, counterSegmentId)
        val newRoute = simpleRoute(mainSegmentId, statsSegmentId, infoSegmentId)

        // When
        val routingActions = Router.routingActionsForTransitionFrom(oldRoute, newRoute)

        // Then
        var action1Correct = false
        var action2Correct = false

        routingActions.forEach { routingAction ->
            when (routingAction) {
                is change -> {
                    if (routingAction.responsibleRoutableIndex == 1
                            && routingAction.segmentToBeReplaced.id == counterSegmentId
                            && routingAction.newSegment.id == statsSegmentId) {
                        action1Correct = true
                    }
                }
                is push -> {
                    if (routingAction.responsibleRoutableIndex == 2 && routingAction.segmentToBePushed.id == infoSegmentId) {
                        action2Correct = true
                    }

                }

            }
        }

        assertThat(action1Correct).isTrue()
        assertThat(action2Correct).isTrue()
        assertThat(routingActions.count()).isEqualTo(2)
    }

    @Test
    fun `should generate a Change action on root when root element changes`() {

        // Given
        val oldRoute = simpleRoute(mainSegmentId)
        val newRoute = simpleRoute(statsSegmentId)

        // When
        val routingActions = Router.routingActionsForTransitionFrom(oldRoute, newRoute)

        // Then
        var controllerIndex: Int = -1
        var toBeReplaced = ""
        var new = ""
        routingActions.forEach { routingAction ->

            when (routingAction) {
                is change -> {
                    controllerIndex = routingAction.responsibleRoutableIndex
                    toBeReplaced = routingAction.segmentToBeReplaced.id
                    new = routingAction.newSegment.id
                }
            }
        }
        assertThat(controllerIndex).isEqualTo(0)
        assertThat(routingActions.count()).isEqualTo(1)
        assertThat(toBeReplaced).isEqualTo(mainSegmentId)
        assertThat(new).isEqualTo(statsSegmentId)
    }

    @Test
    fun `should generate a pop action followed by a change action on root when whole route changes`() {
        // Given
        val oldRoute = simpleRoute(mainSegmentId, counterSegmentId)
        val newRoute = simpleRoute(statsSegmentId)

        // When
        val routingActions = Router.routingActionsForTransitionFrom(oldRoute, newRoute)

        // Then
        var action1Correct = false
        var action2Correct = false

        routingActions.forEach { routingAction ->
            when (routingAction) {
                is pop -> {
                    if (routingAction.responsibleRoutableIndex == 1
                            && routingAction.segmentToBePopped.id == counterSegmentId) {
                        action1Correct = true
                    }
                }
                is change -> {
                    if (routingAction.responsibleRoutableIndex == 0
                            && routingAction.segmentToBeReplaced.id == mainSegmentId
                            && routingAction.newSegment.id == statsSegmentId) {
                        action2Correct = true
                    }
                }
            }
        }

        assertThat(action1Correct).isTrue()
        assertThat(action2Correct).isTrue()
        assertThat(routingActions.count()).isEqualTo(2)
    }

    @Test
    fun `should calculate no actions for transition from empty route to empty route`() {
        // Given
        val oldRoute: Route = emptyList()
        val newRoute: Route = emptyList()
        // When
        val routingActions = Router.routingActionsForTransitionFrom(oldRoute, newRoute)

        // Then
        assertThat(routingActions.count()).isEqualTo(0)
    }

    @Test
    fun `should calculate no actions for transitions between identical, non-empty routes`() {

        // Given
        val oldRoute = simpleRoute(mainSegmentId, counterSegmentId)
        val newRoute = simpleRoute(mainSegmentId, counterSegmentId)

        // When
        val routingActions = Router.routingActionsForTransitionFrom(oldRoute, newRoute)

        // Then
        assertThat(routingActions.count()).isEqualTo(0)
    }

    @Test
    fun `should calculate transitions with multiple pops`() {

        // Given
        val oldRoute = simpleRoute(mainSegmentId, statsSegmentId, counterSegmentId)
        val newRoute = simpleRoute(mainSegmentId)

        // When
        val routingActions = Router.routingActionsForTransitionFrom(oldRoute, newRoute)

        // Then
        var action1Correct = false
        var action2Correct = false
        routingActions.forEach { routingAction ->
            when (routingAction) {
                is pop -> {
                    if (routingAction.responsibleRoutableIndex == 2 && routingAction.segmentToBePopped.id == counterSegmentId) {
                        action1Correct = true
                    }
                    if (routingAction.responsibleRoutableIndex == 1 && routingAction.segmentToBePopped.id == statsSegmentId) {
                        action2Correct = true
                    }
                }
            }
        }
        assertThat(action1Correct).isTrue()
        assertThat(action2Correct).isTrue()
        assertThat(routingActions.count()).isEqualTo(2)
    }

    @Test
    fun `should calculate transitions with multiple pushes`() {

        // Given
        val oldRoute = simpleRoute(mainSegmentId)
        val newRoute = simpleRoute(mainSegmentId, statsSegmentId, counterSegmentId)

        // When
        val routingActions = Router.routingActionsForTransitionFrom(oldRoute, newRoute)

        // Then
        var action1Correct = false
        var action2Correct = false
        routingActions.forEach { routingAction ->
            when (routingAction) {
                is push -> {
                    if (routingAction.responsibleRoutableIndex == 1 && routingAction.segmentToBePushed.id == statsSegmentId) {
                        action1Correct = true
                    }
                    if (routingAction.responsibleRoutableIndex == 2 && routingAction.segmentToBePushed.id == counterSegmentId) {
                        action2Correct = true
                    }

                }
            }
        }
        assertThat(action1Correct).isTrue()
        assertThat(action2Correct).isTrue()
        assertThat(routingActions.count()).isEqualTo(2)
    }
}
