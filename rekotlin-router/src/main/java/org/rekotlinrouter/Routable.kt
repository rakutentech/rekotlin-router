package org.rekotlinrouter

//typealias Route = List<RouteSegment>

@Suppress("unused", "NOTHING_TO_INLINE")
data class Route(val segments: List<RouteSegment>) {
    // several convenience constructor aliases
    constructor(vararg segments: String) : this(segments.map{ RouteSegment(it)})
    constructor(vararg segments: Pair<String, Any?>) : this(segments.map { RouteSegment(it) })
    constructor(vararg segments: RouteSegment) : this(segments.asList())
    constructor() : this(emptyList())

    // convenience members and operators to make route look like a list
    inline val count: Int get() = segments.count()
    inline operator fun get(i: Int): RouteSegment = segments[i]
    inline operator fun minus(segment: RouteSegment) = Route(segments - segment)
    inline operator fun plus(segment: RouteSegment) = Route(segments + segment)
    inline operator fun contains(segment: RouteSegment) = segments.contains(segment)
    inline fun isEmpty() = segments.isEmpty()
}

data class RouteSegment(val id: String, val args: Any? = null) {
    constructor(segment: Pair<String, Any?>) : this(segment.first, segment.second)
}

interface Routable {
    fun pushRouteSegment(routeSegment: RouteSegment, animated: Boolean = false): Routable
    fun popRouteSegment(routeSegment: RouteSegment, animated: Boolean = false)
    fun changeRouteSegment(from: RouteSegment, to: RouteSegment, animated: Boolean = false): Routable
}
