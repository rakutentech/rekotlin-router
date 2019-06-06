package org.rekotlinrouter

val Route.routeString get(): String = this.map { it.id }.joinToString(separator = "/")

data class NavigationState(val route: Route = arrayListOf(),
                           val routeSpecificState: HashMap<String, Any> = HashMap(),
                           val changeRouteAnimated: Boolean = true) {

    fun <T> getRouteSpecificState(givenRoutes: Route): T? {
        val routeString = givenRoutes.routeString

        return routeSpecificState[routeString] as? T
    }
}

@Suppress("unused") // part of public API
interface HasNavigationState {
    var navigationState: NavigationState
}
