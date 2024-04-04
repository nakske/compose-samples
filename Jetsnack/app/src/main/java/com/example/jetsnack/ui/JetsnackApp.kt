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

@file:OptIn(
    ExperimentalSharedTransitionApi::class, ExperimentalSharedTransitionApi::class,
    ExperimentalSharedTransitionApi::class
)

package com.example.jetsnack.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.example.jetsnack.ui.components.JetsnackScaffold
import com.example.jetsnack.ui.components.JetsnackSnackbar
import com.example.jetsnack.ui.components.rememberJetsnackScaffoldState
import com.example.jetsnack.ui.home.HomeSections
import com.example.jetsnack.ui.home.JetsnackBottomBar
import com.example.jetsnack.ui.home.addHomeGraph
import com.example.jetsnack.ui.home.sharedElementComposable
import com.example.jetsnack.ui.navigation.JetsnackNavController
import com.example.jetsnack.ui.navigation.MainDestinations
import com.example.jetsnack.ui.navigation.rememberJetsnackNavController
import com.example.jetsnack.ui.snackdetail.SnackDetail
import com.example.jetsnack.ui.theme.JetsnackTheme

@Preview
@Composable
fun JetsnackApp() {
    JetsnackTheme {
        val jetsnackNavController = rememberJetsnackNavController()
        SharedTransitionLayout {
            NavHost(
                navController = jetsnackNavController.navController,
                startDestination = MainDestinations.HOME_ROUTE
            ) {
                sharedElementComposable(
                    route = MainDestinations.HOME_ROUTE,
                    sharedTransitionScope = this@SharedTransitionLayout
                ) {
                    MainContainer(
                        onSnackSelected = jetsnackNavController::navigateToSnackDetail
                    )
                }
                sharedElementComposable(
                    sharedTransitionScope = this@SharedTransitionLayout,
                    "${MainDestinations.SNACK_DETAIL_ROUTE}/{${MainDestinations.SNACK_ID_KEY}}?origin={${MainDestinations.ORIGIN}}",
                    arguments = listOf(navArgument(MainDestinations.SNACK_ID_KEY) {
                        type = NavType.LongType
                    }),

                    ) { backStackEntry ->
                    val arguments = requireNotNull(backStackEntry.arguments)
                    val snackId = arguments.getLong(MainDestinations.SNACK_ID_KEY)
                    val origin = arguments.getString(MainDestinations.ORIGIN)
                    SnackDetail(
                        snackId,
                        origin = origin ?: "",
                        upPress = jetsnackNavController::upPress
                    )
                }
            }
        }

    }
}

@Composable
fun MainContainer(
    modifier: Modifier = Modifier,
    onSnackSelected: (Long, String, NavBackStackEntry) -> Unit
) {
    val jetsnackScaffoldState = rememberJetsnackScaffoldState()
    val nestedNavController = rememberJetsnackNavController()
    val navBackStackEntry by nestedNavController.navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val sharedTransitionScope = LocalSharedElementScopes.current.sharedTransitionScope
        ?: throw IllegalStateException("No SharedElementScope found")
    JetsnackScaffold(
        bottomBar = {
            with(sharedTransitionScope) {
                JetsnackBottomBar(
                    tabs = HomeSections.entries.toTypedArray(),
                    currentRoute = currentRoute ?: HomeSections.FEED.route,
                    navigateToRoute = nestedNavController::navigateToBottomBarRoute,
                    // todo fix this as it doesn't hide quick enough
                    modifier = Modifier.renderInSharedTransitionScopeOverlay(
                        zIndexInOverlay = 1f,
                    )
                )
            }

        },
        modifier = modifier,
        snackbarHost = {
            SnackbarHost(
                hostState = it,
                modifier = Modifier.systemBarsPadding(),
                snackbar = { snackbarData -> JetsnackSnackbar(snackbarData) }
            )
        },
        scaffoldState = jetsnackScaffoldState.scaffoldState,
    ) { paddingValues ->
        NavHost(
            navController = nestedNavController.navController,
            startDestination = HomeSections.FEED.route
        ) {
            addHomeGraph(
                onSnackSelected = onSnackSelected,
                modifier = Modifier.padding(paddingValues)
            )
        }

    }
}

val LocalSharedElementScopes = compositionLocalOf { SharedElementScopes() }

data class SharedElementScopes(
    val sharedTransitionScope: SharedTransitionScope? = null,
    val animatedVisibilityScope: AnimatedVisibilityScope? = null
)