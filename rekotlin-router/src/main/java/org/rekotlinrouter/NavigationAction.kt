package org.rekotlinrouter

import org.rekotlin.Action

data class SetRouteAction(val route: Route, val animated: Boolean = true) : Action
data class SetRouteSpecificData(val route: Route, val data: Any) : Action
