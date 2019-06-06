package org.rekotlinrouter


typealias Route = List<RouteSegment>

data class RouteSegment(val id: String, val args: Any? = null)


interface Routable {

    fun pushRouteSegment(routeSegment: RouteSegment,
                         animated: Boolean = false,
                         completionHandler: () -> Unit = {}): Routable

    fun popRouteSegment(routeSegment: RouteSegment,
                        animated: Boolean = false,
                        completionHandler: () -> Unit = {})

    fun changeRouteSegment(from: RouteSegment,
                           to: RouteSegment,
                           animated: Boolean = false,
                           completionHandler: () -> Unit = {}): Routable
}
