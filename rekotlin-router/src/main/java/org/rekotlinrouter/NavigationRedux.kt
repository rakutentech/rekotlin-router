package org.rekotlinrouter

import org.rekotlin.Action

// State

data class NavigationState(val route: Route = emptyList(),
                           val animated: Boolean = true)

@Suppress("unused") // part of public API
interface HasNavigationState {
    var navigationState: NavigationState
}

// Action

data class SetRouteAction(val route: Route, val animated: Boolean = true) : Action

// Reducer

/**
The navigationReducer handles the state slice concerned with storing the current navigation
information. Note, that this reducer is **not** a *top-level* reducer, you need to use it within
another reducer and pass in the relevant state slice. Take a look at the test cases to see an
example set up.
 */
fun navigationReducer(action: Action, oldState: NavigationState?): NavigationState {
    val state = oldState ?: NavigationState()

    return when (action) {
        is SetRouteAction -> state.copy(
                route = action.route,
                animated = action.animated
        )
        else -> state
    }
}
