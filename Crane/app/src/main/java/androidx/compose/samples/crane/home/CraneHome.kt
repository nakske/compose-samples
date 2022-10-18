/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.samples.crane.home

import android.location.Location
import android.os.Bundle
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.BackdropScaffold
import androidx.compose.material.BackdropValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberBackdropScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.samples.crane.R
import androidx.compose.samples.crane.base.CraneDrawer
import androidx.compose.samples.crane.base.CraneTabBar
import androidx.compose.samples.crane.base.CraneTabs
import androidx.compose.samples.crane.base.ExploreSection
import androidx.compose.samples.crane.data.ExploreModel
import androidx.compose.samples.crane.ui.BottomSheetShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.MapView
import com.google.android.libraries.maps.model.MarkerOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.ktx.awaitMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

typealias OnExploreItemClicked = (ExploreModel) -> Unit

enum class CraneScreen {
   /* Fly,*/ Sleep, Eat, Here
}

@Composable
fun CraneHome(
    widthSize: WindowWidthSizeClass,
    onExploreItemClicked: OnExploreItemClicked,
    onDateSelectionClicked: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel
) {
    val scaffoldState = rememberScaffoldState()
    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier.statusBarsPadding(),
        drawerContent = {
            CraneDrawer()
        }
    ) { contentPadding ->
        val scope = rememberCoroutineScope()
        CraneHomeContent(
            modifier = modifier.padding(contentPadding),
            widthSize = widthSize,
            onExploreItemClicked = onExploreItemClicked,
            onDateSelectionClicked = onDateSelectionClicked,
            openDrawer = {
                scope.launch {
                    scaffoldState.drawerState.open()
                }
            },
            viewModel = viewModel
        )
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalAnimationApi::class)
@Composable
fun CraneHomeContent(
    widthSize: WindowWidthSizeClass,
    onExploreItemClicked: OnExploreItemClicked,
    onDateSelectionClicked: () -> Unit,
    openDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel
) {
    val suggestedDestinations by viewModel.suggestedDestinations.observeAsState()

    val onPeopleChanged: (Int) -> Unit = { viewModel.updatePeople(it) }
    var tabSelected by remember { mutableStateOf(CraneScreen.Here) }

    BackdropScaffold(
        modifier = modifier,
        scaffoldState = rememberBackdropScaffoldState(BackdropValue.Revealed),
        frontLayerShape = BottomSheetShape,
        frontLayerScrimColor = Color.Unspecified,
        appBar = {
            HomeTabBar(openDrawer, tabSelected, onTabSelected = { tabSelected = it })
        },
        backLayerContent = {
            SearchContent(
                widthSize,
                tabSelected,
                viewModel,
                onPeopleChanged,
                onDateSelectionClicked,
                onExploreItemClicked
            )
        },
        frontLayerContent = {
            AnimatedContent(
                targetState = tabSelected,
                transitionSpec = {
                    val direction = if (initialState.ordinal < targetState.ordinal)
                        AnimatedContentScope.SlideDirection.Left else AnimatedContentScope
                        .SlideDirection.Right

                    slideIntoContainer(
                        towards = direction,
                        animationSpec = tween(ANIMATED_CONTENT_ANIMATION_DURATION)
                    ) with
                        slideOutOfContainer(
                            towards = direction,
                            animationSpec = tween(ANIMATED_CONTENT_ANIMATION_DURATION)
                        ) using SizeTransform(
                        clip = false,
                        sizeAnimationSpec = { _, _ ->
                            tween(ANIMATED_CONTENT_ANIMATION_DURATION, easing = EaseInOut)
                        }
                    )
                }
            ) { targetState ->
                when (targetState) {
                    /*CraneScreen.Fly -> {
                        suggestedDestinations?.let { destinations ->
                            ExploreSection(
                                widthSize = widthSize,
                                title = stringResource(R.string.explore_flights_by_destination),
                                exploreList = destinations,
                                onItemClicked = onExploreItemClicked
                            )
                        }
                    }*/
                    CraneScreen.Sleep -> {
                        ExploreSection(
                            widthSize = widthSize,
                            title = stringResource(R.string.explore_properties_by_destination),
                            exploreList = viewModel.hotels,
                            onItemClicked = onExploreItemClicked
                        )
                    }
                    CraneScreen.Eat -> {
                        ExploreSection(
                            widthSize = widthSize,
                            title = stringResource(R.string.explore_restaurants_by_destination),
                            exploreList = viewModel.restaurants,
                            onItemClicked = onExploreItemClicked
                        )
                    }
                    CraneScreen.Here -> {
                        GetCurrentLocation()
                    }
                }
            }
        }
    )
}

@Composable
private fun HomeTabBar(
    openDrawer: () -> Unit,
    tabSelected: CraneScreen,
    onTabSelected: (CraneScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    CraneTabBar(
        modifier = modifier
            .wrapContentWidth()
            .sizeIn(maxWidth = 500.dp),
        onMenuClicked = openDrawer
    ) { tabBarModifier ->
        CraneTabs(
            modifier = tabBarModifier,
            titles = CraneScreen.values().map { it.name },
            tabSelected = tabSelected,
            onTabSelected = { newTab -> onTabSelected(CraneScreen.values()[newTab.ordinal]) }
        )
    }
}

private const val ANIMATED_CONTENT_ANIMATION_DURATION = 600
@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun SearchContent(
    widthSize: WindowWidthSizeClass,
    tabSelected: CraneScreen,
    viewModel: MainViewModel,
    onPeopleChanged: (Int) -> Unit,
    onDateSelectionClicked: () -> Unit,
    onExploreItemClicked: OnExploreItemClicked
) {
    // Reading datesSelected State from here instead of passing the String from the ViewModel
    // to cause a recomposition when the dates change.
    val selectedDates = viewModel.calendarState.calendarUiState.value.selectedDatesFormatted
    AnimatedContent(
        targetState = tabSelected,
        transitionSpec = {
            fadeIn(
                animationSpec = tween(ANIMATED_CONTENT_ANIMATION_DURATION, easing = EaseIn)
            ).with(
                fadeOut(
                    animationSpec = tween(ANIMATED_CONTENT_ANIMATION_DURATION, easing = EaseOut)
                )
            ).using(
                SizeTransform(
                    sizeAnimationSpec = { _, _ ->
                        tween(ANIMATED_CONTENT_ANIMATION_DURATION, easing = EaseInOut)
                    }
                )
            )
        },
    ) { targetState ->
        when (targetState) {
           /* CraneScreen.Fly -> FlySearchContent(
                widthSize = widthSize,
                datesSelected = selectedDates,
                searchUpdates = FlySearchContentUpdates(
                    onPeopleChanged = onPeopleChanged,
                    onToDestinationChanged = { viewModel.toDestinationChanged(it) },
                    onDateSelectionClicked = onDateSelectionClicked,
                    onExploreItemClicked = onExploreItemClicked
                )
            )*/
            CraneScreen.Sleep -> SleepSearchContent(
                widthSize = widthSize,
                datesSelected = selectedDates,
                sleepUpdates = SleepSearchContentUpdates(
                    onPeopleChanged = onPeopleChanged,
                    onDateSelectionClicked = onDateSelectionClicked,
                    onExploreItemClicked = onExploreItemClicked
                )
            )
            CraneScreen.Eat -> EatSearchContent(
                widthSize = widthSize,
                datesSelected = selectedDates,
                eatUpdates = EatSearchContentUpdates(
                    onPeopleChanged = onPeopleChanged,
                    onDateSelectionClicked = onDateSelectionClicked,
                    onExploreItemClicked = onExploreItemClicked
                )
            )
            CraneScreen.Here -> HereSearchContent(
                widthSize = widthSize,
                datesSelected = selectedDates,
                searchUpdates = HereSearchContentUpdates(
                    onPeopleChanged = onPeopleChanged,
                    onToDestinationChanged = { viewModel.toDestinationChanged(it) },
                    onDateSelectionClicked = onDateSelectionClicked,
                    onExploreItemClicked = onExploreItemClicked
                )
            )
        }
    }
}

data class FlySearchContentUpdates(
    val onPeopleChanged: (Int) -> Unit,
    val onToDestinationChanged: (String) -> Unit,
    val onDateSelectionClicked: () -> Unit,
    val onExploreItemClicked: OnExploreItemClicked
)

data class SleepSearchContentUpdates(
    val onPeopleChanged: (Int) -> Unit,
    val onDateSelectionClicked: () -> Unit,
    val onExploreItemClicked: OnExploreItemClicked
)

data class EatSearchContentUpdates(
    val onPeopleChanged: (Int) -> Unit,
    val onDateSelectionClicked: () -> Unit,
    val onExploreItemClicked: OnExploreItemClicked
)

data class HereSearchContentUpdates(
    val onPeopleChanged: (Int) -> Unit,
    val onToDestinationChanged: (String) -> Unit,
    val onDateSelectionClicked: () -> Unit,
    val onExploreItemClicked: OnExploreItemClicked
)



private  var locations: Location?=null
private  lateinit var  mapView: MapView

@Composable
fun GetCurrentLocation() {
    mapView = rememberMapViewWithLifeCycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        AndroidView(
            {mapView}
        ) { mapView ->
            CoroutineScope(Dispatchers.Main).launch {
                val map = mapView.awaitMap()
                map.uiSettings.isZoomControlsEnabled = true
                if (locations !=null){
                    val destination = com.google.android.libraries.maps.model.LatLng(locations!!.latitude, locations!!.longitude)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(destination, 6f))
                    val markerOptions =  MarkerOptions()
                        .title("Your location")
                        .position(destination)
                    map.addMarker(markerOptions)
                } else {
                    val destination = com.google.android.libraries.maps.model.LatLng(51.145529, 5.740863)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(destination, 6f))
                    val markerOptions =  MarkerOptions()
                        .title("Static location")
                        .position(destination)
                    map.addMarker(markerOptions)
                }
            }
        }
    }
}

@Composable
fun rememberMapViewWithLifeCycle(): MapView {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            id = R.id.map_frame
        }
    }
    val lifeCycleObserver = rememberMapLifecycleObserver(mapView)
    val lifeCycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifeCycle) {
        lifeCycle.addObserver(lifeCycleObserver)
        onDispose {
            lifeCycle.removeObserver(lifeCycleObserver)
        }
    }

    return mapView
}

@Composable
fun rememberMapLifecycleObserver(mapView: MapView): LifecycleEventObserver =
    remember(mapView) {
        LifecycleEventObserver { _, event ->
            when(event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> throw IllegalStateException()
            }
        }
    }