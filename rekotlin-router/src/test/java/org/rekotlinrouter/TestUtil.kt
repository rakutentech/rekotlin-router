/**
 * Created by Mohanraj Karatadipalayam on 28/09/17.
 */

package org.rekotlinrouter

import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldNotBeNull

fun simpleRoute(vararg segments: String): Route = segments.map { RouteSegment(it) }
fun route(vararg segments: Pair<String, Any?>): Route = segments.map { RouteSegment(it.first, it.second) }

infix fun RouteSegment?.shouldHaveArgs(args: Any) {
    this.shouldNotBeNull()
    this.args.shouldNotBeNull()
    this.args shouldEqual args
}

infix fun RouteSegment?.shouldHaveId(id: String) {
    this.shouldNotBeNull()
    this.id shouldEqual id
}