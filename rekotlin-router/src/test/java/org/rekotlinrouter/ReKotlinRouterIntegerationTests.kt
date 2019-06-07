package org.rekotlinrouter

import android.os.Handler
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldNotBeNull
import org.junit.Before
import org.junit.Test
import org.mockito.Matchers.any
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.rekotlin.Action
import org.rekotlin.StateType
import org.rekotlin.Store
import org.rekotlin.Subscription

class FakeAppState(var navigationState: NavigationState = NavigationState()) : StateType

fun appReducer(action: Action, state: FakeAppState?): FakeAppState {
    val fakeAppState = FakeAppState()
    fakeAppState.navigationState = navigationReducer(action, state?.navigationState)
    return fakeAppState
}

fun selectNavigationState(subscription: Subscription<FakeAppState>): Subscription<NavigationState> =
        subscription.select { stateType -> stateType.navigationState }

class FakeRoutable(
        private val pop: (RouteSegment, Boolean, () -> Unit) -> Unit = { _, _, _ -> },
        private val change: (RouteSegment, RouteSegment, Boolean, () -> Unit) -> Unit = { _, _, _, _ -> },
        private val push: (RouteSegment, Boolean, () -> Unit) -> Unit = { _, _, _ -> },
        private val childRoutable: Routable? = null
) : Routable {
    override fun popRouteSegment(routeSegment: RouteSegment, animated: Boolean, completionHandler: () -> Unit) =
            pop(routeSegment, animated, completionHandler)

    override fun changeRouteSegment(from: RouteSegment, to: RouteSegment, animated: Boolean, completionHandler: () -> Unit): Routable {
        change(from, to, animated, completionHandler)
        return childRoutable ?: this
    }

    override fun pushRouteSegment(routeSegment: RouteSegment, animated: Boolean, completionHandler: () -> Unit): Routable {
        push(routeSegment, animated, completionHandler)
        return childRoutable ?: this
    }
}

fun fakeTestHandler(): Handler {
    val fakeHandler = mock(android.os.Handler::class.java)
    `when`(fakeHandler.post(any(Runnable::class.java))).thenAnswer { invocation ->
        (invocation.arguments[0] as Runnable).run()
        null
    }
    return fakeHandler
}

class RoutingCallTest {

    private val store: Store<FakeAppState> = Store(reducer = ::appReducer, state = FakeAppState())
    private val fakeHandler: Handler = fakeTestHandler()

    @Test
    fun `should not push route segment when no route is dispatched`() {
        // Given
        var pushRouteCalled = false
        val routable = FakeRoutable(push = { _, _, _ -> pushRouteCalled = true })

        // When
        Router(store, routable, ::selectNavigationState, fakeHandler)

        // Then
        pushRouteCalled.shouldBeFalse()
    }

    @Test
    fun `should push route segment with identifier to root when an initial route is dispatched`() {
        // Given
        var pushedSegment: RouteSegment? = null
        val routable = FakeRoutable(push = { segment, _, _ -> pushedSegment = segment })

        val action = SetRouteAction(Route("root"))
        store.dispatch(action)

        // when
        Router(store, routable, ::selectNavigationState, fakeHandler)

        // Then
        pushedSegment.shouldNotBeNull()
        pushedSegment!!.id shouldEqual "root"
    }

    @Test
    fun `should push root and child segment when a set route 2 segments is dispatched`() {

        // Given
        val action = SetRouteAction(Route("root", "child"))
        store.dispatch(action)

        var rootSegment: RouteSegment? = null
        var childSegment: RouteSegment? = null

        val child = FakeRoutable(
                push = { segment, _, _ -> childSegment = segment }
        )

        val root = FakeRoutable(
                push = { segment, _, _ -> rootSegment = segment },
                childRoutable = child
        )

        // When
        Router(store, root, ::selectNavigationState, fakeHandler)

        // Then
        rootSegment shouldHaveId "root"
        childSegment shouldHaveId "child"
    }
}

class RouteArgsSpec {
    private val store: Store<FakeAppState> = Store(reducer = ::appReducer, state = null)

    @Test
    fun `should pass route args to push when set via SetRouteAction`() {
        //Given
        var pushedSegment: RouteSegment? = null
        val routable = FakeRoutable(
                push = { segment, _, _ -> pushedSegment = segment }
        )
        Router(store, routable, ::selectNavigationState, fakeTestHandler())

        // When
        val action = SetRouteAction(Route("main" to 1))
        store.dispatch(action)

        // Then
        pushedSegment shouldHaveArgs 1
    }

    @Test
    fun `should pass route args to segment routables for longer route`() {
        //Given
        var rootSegment: RouteSegment? = null
        var childSegment: RouteSegment? = null
        var grandChildSegment: RouteSegment? = null

        val grandChildRoutable = FakeRoutable(
                push = { segment, _, _ -> grandChildSegment = segment }
        )
        val childRoutable = FakeRoutable(
                push = { segment, _, _ -> childSegment = segment },
                childRoutable = grandChildRoutable
        )
        val routable = FakeRoutable(
                push = { segment, _, _ -> rootSegment = segment },
                childRoutable = childRoutable
        )

        Router(store, routable, ::selectNavigationState, fakeTestHandler())

        // When
        val action = SetRouteAction(Route(
                "root" to 1,
                "child" to 2,
                "grandchild" to 3
        ))
        store.dispatch(action)

        // Then
        rootSegment shouldHaveArgs 1
        childSegment shouldHaveArgs 2
        grandChildSegment shouldHaveArgs 3
    }
}

class RoutingAnimationTest {

    private val store: Store<FakeAppState> = Store(reducer = ::appReducer, state = null)
    private val animated: MutableList<Boolean> = mutableListOf()

    @Before
    fun setup() {
        val routable = FakeRoutable(
                push = { _, a, _ -> animated.add(a) }
        )

        Router(store, routable, ::selectNavigationState, fakeTestHandler())
    }


    @Test
    fun `should push animated when dispatch route change with animate as true`() {
        //Given
        val actionArray = Route("root", "child")
        val action = SetRouteAction(actionArray, animated = true)

        // When
        store.dispatch(action)

        // Then
        animated.forEach { it.shouldBeTrue() }
    }

    @Test
    fun `should not push animation when route change with animate is false`() {
        //Given
        val actionArray = Route("root", "child")
        val action = SetRouteAction(actionArray, animated = false)
        // When
        store.dispatch(action)

        // Then
        animated.forEach { it.shouldBeFalse() }
    }

    @Test
    fun `should push animation by default`() {
        //Given

        val actionArray = Route("root", "child")
        val action = SetRouteAction(actionArray)
        // When
        store.dispatch(action)

        // Then
        animated.forEach { it.shouldBeTrue() }
    }
}
