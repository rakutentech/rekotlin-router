package org.rekotlinrouter

import org.rekotlin.Action

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
                changeRouteAnimated = action.animated
        )
        is SetRouteSpecificData -> {
            state.routeSpecificState[action.route.routeString] = action.data
            state
        }
        else -> state
    }
}
