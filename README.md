# ReKotlin-Router [![Build Status](https://travis-ci.org/ReKotlin/rekotlin-router.svg?branch=master)](https://travis-ci.org/ReKotlin/rekotlin-router) [![License MIT](https://img.shields.io/badge/license-MIT-blue.svg?style=flat-square)](https://github.com/ReKotlin/rekotlin-router/blob/master/LICENSE.md) [ ![Download](https://api.bintray.com/packages/rekotlin/rekotlin-router/rekotlin-router/images/download.svg?version=0.2.0) ](https://bintray.com/rekotlin/rekotlin-router/rekotlin-router/0.2.0/link)

A declarative router for [ReKotlin](https://github.com/GeoThings/ReKotlin). Allows developers to declare routes in a similar manner as URLs are used on the web.

Using ReKotlinRouter you can navigate your app by defining the target location in the form of a URL-like sequence of identifiers:

```Kotlin
val route = Route("login", "repoList")
store.dispatch(SetRouteAction(action))
 
val nextRoute = route + RouteSegment("repoDetail", 123L) // Route with parameter
store.dispatch(SetRouteAction(nextRoute))
```

# About ReKotlinRouter

When building apps with ReKotlin you should aim to cause **all** state changes through actions - this includes changes to the navigation state.

This requires to store the current navigation state within the app state and to use actions to trigger changes to that state - both is provided ReKotlinRouter.

# Installation

This fork is currently not published

# Configuration

Extend your app state to include the navigation state:

```kotlin
data class AppState(
    override val navigationState: NavigationState,
    // other application states such as....
    val authenticationState: AuthenticationState,
    val repoListState: RepoListState
): StateType
```

After you've initialized your store, create an instance of `Router`, passing in a reference to the store and to the root `Routable`. Additionally you will need to provide a closure that describes how to access the `navigationState` of your application state:

```Kotlin
 router = Router(store,
                rootRoutable = /* your root routable */,
                stateTransform = { subscription ->
                    subscription.select { stateType ->
                        stateType.navigationState
                    }
                })
```

We'll discuss `Routable` in the next main section.

## Calling the Navigation Reducer

The `navigationReducer` is a function provided as part of `ReKotlinRouter`. You need to call it from within your top-level reducer. Here's a simple example from the specs:

```Kotlin

fun appReducer(action: Action, oldState: AppState?) : AppState {
    // if no state has been provided, create the default state
    val state = oldState ?: AppState() 

    return state.copy(
            navigationState = navigationReducer(action, state.navigationState),
             // other application state reducers such as....
            authenticationState = authenticationReducer(action, state.authenticationState),
            repoListState = repoListReducer(action, state.repoListState)
            )
}
```
The `navigationReducer` will all routing relevant actions, you should *never* have to inspect the `NavigationState` or any navigation related actions yourself

# Implementing `Routable`

ReKotlinRouter works with routes that are defined, similar to URLs, as a sequence of identifiers e.g. `["Home", "User", "UserDetail"]`. It uses `Routable`s to implement that interaction.

Each route segment is mapped to one responsible `Routable`. The `Routable` needs to be able to present a child, hide a child or replace a child with another child.

Here is the `Routable` interface with the methods you should implement:

```Kotlin
interface Routable {
    fun pushRouteSegment(RouteSegment: RouteSegment,
                         animated: Boolean,
                         completionHandler: () -> Unit): Routable

    fun popRouteSegment(RouteSegment: RouteSegment,
                        animated: Boolean,
                        completionHandler: () -> Unit)

    fun changeRouteSegment(from: RouteSegment,
                           to: RouteSegment,
                           animated: Boolean,
                           completionHandler: () -> Unit): Routable
}
```

As part of initializing `Router` you need to pass the first `Routable` as an argument. That root `Routable` will be responsible for the first route segment.

If e.g. you set the route of your application to `["Home"]`, your root `Routable` will be asked to present the view that corresponds to the identifier `"Home"`.

Whenever a `Routable` presents a new route segment, it needs to return a new `Routable` that will be responsible for managing the presented segment. If you want to navigate from `["Home"]` to `["Home", "Users"]` the `Routable` responsible for the `"Home"` segment will be asked to present the `"User"` segment.

If your navigation stack uses a modal presentation for this transition, the implementation of `Routable` for the `"Root"` segment might look like this:

```kotlin
class RootRoutable(val context: Context): Routable {
    override fun popRouteSegment(routeSegment: RouteSegment,
                                 animated: Boolean,
                                 completionHandler: () -> Unit) {
        TODO("not implemented")
    }

    override fun pushRouteSegment(routeSegment: RouteSegment,
                                  animated: Boolean,
                                  completionHandler: () -> Unit) =
        when(routeSegment.id) {
            loginRoute -> LoginRoutable(context)
            welcomeRoute ->  RoutableHelper.createWelcomeRoutable(context)
            else -> LoginRoutable(context)
        }
    

    override fun changeRouteSegment(from: RouteSegment,
                                    to: RouteSegment,
                                    animated: Boolean,
                                    completionHandler: () -> Unit): Routable {
       TODO("not implemented")
    }

}

object RoutableHelper {

     fun createWelcomeRoutable(context: Context): WelcomeRoutable {
        val welcomeIntent = Intent(context, WelcomeActivity::class.java)
        welcomeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(welcomeIntent)
        return WelcomeRoutable(context)
    }
}
```

# Changing the Current Route

Currently the only way to change the current application route is by using the `SetRouteAction` and providing an absolute route. Here's a brief example:

```kotlin
val routes = Route(loginRoute, repoListRoute)
val action = SetRouteAction(Route)
store.dispatch(action)
```
As development continues, support for changing individual route segments may be added.

## Bugs and Feedback

For bugs, feature requests, and discussion please use [GitHub Issues][issues].
For general usage questions please use the [mailing list][list] or [StackOverflow][so].

# Contributing

There's still a lot of work to do here! We would love to see you involved!

### Submitting patches

The best way to submit a patch is to [fork the project on github](https://help.github.com/articles/fork-a-repo/) then send us a
[pull request](https://help.github.com/articles/creating-a-pull-request/) via [github](https://github.com).

Before submitting the pull request, make sure all existing tests are passing, and add the new test if it is required.

### Code Formatting
Please format your code using ``kotlinFormatter.xml`` file from [here](docs/kotlinFormatter.xml).

Using this code formatter will help us maintain consistency in code style.

### New functionality
If you want to add new functionality, please file a new proposal issue first to make sure that it is not in progress already. If you have any questions, feel free to create a question issue.

You can find all the details on how to get started in the [Contributing Guide](/CONTRIBUTING.md).

## Compiling & Running tests

```
$ ./gradlew assemble # build
$ ./gradlew check # test
```

# Example Projects

- [GitHubExample](https://github.com/kmmraj/rekotlin-router-github-example): A real world example, involving authentication, network requests and navigation. Still WIP but should be the best example when you starting to adapt `ReKotlin` and `Rekotlin-Router` in your own app.
- [Router working with Fragments](https://github.com/ReKotlin/reKotlinFragmentExample): An example, that explains how to use the router along with fragments.

## Credits

- Many thanks to [Benjamin Encz](https://github.com/Ben-G) and other ReSwift contributors for buidling original [ReSwift](https://github.com/ReSwift/ReSwift) that we really enjoyed working with.
- Also huge thanks to [Dan Abramov](https://github.com/gaearon) for building [Redux](https://github.com/reactjs/redux) - all ideas in here and many implementation details were provided by his library.
