package org.rekotlinrouter

/**
 * Created by Mohanraj Karatadipalayam on 28/09/17.
 */

import android.os.Handler
import android.os.Looper
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import org.rekotlin.Action
import org.rekotlin.StateType
import org.rekotlin.Store
import java.util.concurrent.TimeUnit

class FakeAppState : StateType {
    var navigationState = NavigationState()
}

fun appReducer(action: Action, state: FakeAppState?): FakeAppState {
    val fakeAppState = FakeAppState()
    fakeAppState.navigationState = navigationReducer(action, state?.navigationState)
    return fakeAppState
}

class MockRoutable : Routable {

    var callsToPushRouteSegment: Array<Pair<RouteSegment, Boolean>> = emptyArray()
    var callsToPopRouteSegment: Array<Pair<RouteSegment, Boolean>> = emptyArray()
    var callsToChangeRouteSegment: Array<Triple<RouteSegment, RouteSegment, Boolean>> = emptyArray()

    override fun pushRouteSegment(routeSegment: RouteSegment, animated: Boolean, completionHandler: () -> Unit): Routable {
        callsToPushRouteSegment = callsToPushRouteSegment.plus(Pair(routeSegment, animated))
        completionHandler()
        return MockRoutable()
    }

    override fun popRouteSegment(routeSegment: RouteSegment, animated: Boolean, completionHandler: () -> Unit) {
        callsToPopRouteSegment = callsToPopRouteSegment.plus(Pair(routeSegment, animated))
        completionHandler()
    }

    override fun changeRouteSegment(from: RouteSegment, to: RouteSegment, animated: Boolean, completionHandler: () -> Unit): Routable {
        callsToChangeRouteSegment = callsToChangeRouteSegment.plus(Triple(from, to, animated))
        completionHandler()
        return MockRoutable()
    }
}


@PrepareForTest(Looper::class)
@RunWith(PowerMockRunner::class)
class RoutingCallTest {

    var store: Store<FakeAppState> = Store(reducer = ::appReducer, state = FakeAppState())

    @Before
    @PrepareForTest(Looper::class, Handler::class, Router::class)
    fun initTest() {
        store = Store(reducer = ::appReducer, state = FakeAppState())
        AndroidMockUtil.mockMainThreadHandler()
    }

    @Test
    fun should_not_request_the_main_activity_when_no_route_is_provided() {

        class FakeRootRoutable : Routable {
            var pushRouteIsCalled = false

            override fun pushRouteSegment(routeSegment: RouteSegment, animated: Boolean, completionHandler: () -> Unit): Routable {
                pushRouteIsCalled = true
                return MockRoutable()
            }

            override fun popRouteSegment(routeSegment: RouteSegment, animated: Boolean, completionHandler: () -> Unit) {}

            override fun changeRouteSegment(from: RouteSegment, to: RouteSegment, animated: Boolean, completionHandler: () -> Unit) = this
        }

        // Given
        val routable = FakeRootRoutable()
        // When
        Router(store, routable) { subscription ->
            subscription.select { stateType -> stateType.navigationState }
        }
        // Then
        assertThat(routable.pushRouteIsCalled).isFalse()
    }

    @Test
    fun should_request_the_root_with_identifier_when_an_initial_route_is_provided() {

        // Given

        // The below syntax is not supported by powermock 😟
        // store.dispatch(SetRouteAction(arrayOf("MainActivity")))
        // https://github.com/powermock/powermock/issues/779
        // Until then
        val actionArray = simpleRoute("MainActivity")
        val action = SetRouteAction(actionArray)
        store.dispatch(action)

        class FakeRootRoutable(val calledWithIdentifier: (RouteSegment?) -> Any) : Routable {
            var pushRouteIsCalled = false

            override fun pushRouteSegment(routeSegment: RouteSegment,
                                          animated: Boolean,
                                          completionHandler: () -> Unit): Routable {
                calledWithIdentifier(routeSegment)
                completionHandler()
                pushRouteIsCalled = true
                return MockRoutable()
            }

            override fun popRouteSegment(routeSegment: RouteSegment, animated: Boolean, completionHandler: () -> Unit) {}

            override fun changeRouteSegment(from: RouteSegment, to: RouteSegment, animated: Boolean, completionHandler: () -> Unit) = this
        }

        // Then

        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            Runnable {
                var isRootElementCalled = false
                val rootRoutable = FakeRootRoutable { rootSegment: RouteSegment? ->
                    {
                        isRootElementCalled = rootSegment?.id.equals("MainActivity")
                    }
                }

                Router(store = store, rootRoutable = rootRoutable) { subscription ->
                    subscription.select { stateType -> stateType.navigationState }
                }
                println("The value of isRootElementCalled is $isRootElementCalled")
                assertThat(isRootElementCalled).isTrue()
            }
        }
    }

    @Test
    fun should_call_push_on_the_root_for_a_route_with_two_elements() {

        val actionArray = simpleRoute("MainActivity", "SecondActivity")
        val action = SetRouteAction(actionArray)
        store.dispatch(action)

        class FakeChildRoutable(var calledWithIdentifier: (RouteSegment?) -> Any) : Routable {
            var pushRouteIsCalled = false

            override fun pushRouteSegment(routeSegment: RouteSegment,
                                          animated: Boolean,
                                          completionHandler: () -> Unit): Routable {
                calledWithIdentifier(routeSegment)
                completionHandler()
                pushRouteIsCalled = true
                return MockRoutable()
            }

            override fun popRouteSegment(routeSegment: RouteSegment, animated: Boolean, completionHandler: () -> Unit) {}

            override fun changeRouteSegment(from: RouteSegment, to: RouteSegment, animated: Boolean, completionHandler: () -> Unit) = this
        }

        class FakeRootRoutable(var injectedRoutable: Routable) : Routable {
            var pushRouteIsCalled = false
            var rootRoutableIsCorrect = false
            var routeRootElementIdentifier = RouteSegment("")

            override fun pushRouteSegment(routeSegment: RouteSegment,
                                          animated: Boolean,
                                          completionHandler: () -> Unit): Routable {
                completionHandler()
                pushRouteIsCalled = true

                rootRoutableIsCorrect = routeSegment.id == "MainActivity"
                return injectedRoutable
            }

            override fun popRouteSegment(routeSegment: RouteSegment, animated: Boolean, completionHandler: () -> Unit) {}

            override fun changeRouteSegment(from: RouteSegment, to: RouteSegment, animated: Boolean, completionHandler: () -> Unit) = this
        }

        // Then

        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            object : Runnable {


                override fun run() {
                    var isChildIdentifierCorrect = false
                    val fakeChildRoutable = FakeChildRoutable { calledWithIdentifier: RouteSegment? ->
                        {
                            isChildIdentifierCorrect = calledWithIdentifier?.id.equals("SecondActivity")
                        }
                    }

                    val fakeRootRoutable = FakeRootRoutable(injectedRoutable = fakeChildRoutable)

                    Router(store = store, rootRoutable = fakeRootRoutable) { subscription ->
                        subscription.select { stateType -> stateType.navigationState }
                    }
                    println("The value of isIdentifierCorrect is $isChildIdentifierCorrect")
                    println("The value of rootRouteElementIdentifier is ${fakeRootRoutable.routeRootElementIdentifier}")
                    // Then
                    // Assert
                    assertThat(isChildIdentifierCorrect && fakeRootRoutable.rootRoutableIsCorrect).isTrue()
                }
            }
        }
    }
}

@PrepareForTest(Looper::class, Handler::class)
@RunWith(PowerMockRunner::class)
class RoutingSpecificDataTest {

    var store: Store<FakeAppState> = Store(reducer = ::appReducer, state = null)

    @Before
    @PrepareForTest(Looper::class)
    fun initTest() {
        store = Store(reducer = ::appReducer, state = null)
        AndroidMockUtil.mockMainThreadHandler()
    }

    @Test
    fun should_allow_accessing_the_data_when_providing_the_expected_type() {

        //Given

        val actionArray = simpleRoute("MainActivity", "SecondActivity")
        val actionData = SetRouteSpecificData(route = actionArray, data = "UserID_10")
        // When
        store.dispatch(actionData)

        // Then
        val data: String? = store.state.navigationState.getRouteSpecificState(actionArray)

        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            Runnable {
                // Assert
                assertThat(data).isEqualTo("UserID_10")
            }
        }
    }

    @PrepareForTest(Looper::class, Handler::class, Router::class)
    @RunWith(PowerMockRunner::class)
    class RoutingAnimationTest {

        var store: Store<FakeAppState> = Store(reducer = ::appReducer, state = null)
        var mockRoutable: MockRoutable = MockRoutable()
        var router: Router<FakeAppState>? = null

        @Before
        @PrepareForTest(Looper::class, Handler::class, Router::class)
        fun initTest() {
            AndroidMockUtil.mockMainThreadHandler()
            store = Store(reducer = ::appReducer, state = null)
            mockRoutable = MockRoutable()

            router = Router(store = store,
                    rootRoutable = mockRoutable) { subscription ->
                subscription.select { stateType ->
                    stateType.navigationState
                }
            }
        }

        @Test
        fun should_request_animation_when_dispatch_route_change_with_animate_as_true() {
            //Given
            val actionArray = simpleRoute("MainActivity", "SecondActivity")
            val action = SetRouteAction(actionArray, animated = true)

            // When
            store.dispatch(action)

            // Then
            await().atMost(5, TimeUnit.SECONDS).untilAsserted {
                object : Runnable {
                    @PrepareForTest(Looper::class, Handler::class)
                    override fun run() {
                        // Assert
                        assertThat(mockRoutable.callsToPushRouteSegment.last().second).isTrue()
                    }
                }
            }
        }

        @Test
        fun should_not_request_animation_when_route_change_with_animate_is_false() {
            //Given
            val actionArray = simpleRoute("MainActivity", "SecondActivity")
            val action = SetRouteAction(actionArray, animated = false)
            // When
            store.dispatch(action)

            // Then

            await().atMost(5, TimeUnit.SECONDS).untilAsserted {
                Runnable {
                    // Assert
                    assertThat(mockRoutable.callsToPushRouteSegment.last().second).isFalse()
                }
            }
        }

        @Test
        fun should_request_animation_by_default() {
            //Given

            val actionArray = simpleRoute("MainActivity", "SecondActivity")
            val action = SetRouteAction(actionArray)
            // When
            store.dispatch(action)

            // Then

            await().atMost(5, TimeUnit.SECONDS).untilAsserted {
                Runnable {
                    // Assert
                    assertThat(mockRoutable.callsToPushRouteSegment.last().second).isTrue()
                }
            }
        }
    }
}
