package me.rhunk.snapenhance.ui.setup

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import me.rhunk.snapenhance.SharedContextHolder
import me.rhunk.snapenhance.ui.AppMaterialTheme
import me.rhunk.snapenhance.ui.setup.screens.SetupScreen
import me.rhunk.snapenhance.ui.setup.screens.impl.MappingsScreen
import me.rhunk.snapenhance.ui.setup.screens.impl.PickLanguageScreen
import me.rhunk.snapenhance.ui.setup.screens.impl.SaveFolderScreen


class SetupActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val setupContext = SharedContextHolder.remote(this).apply {
            activity = this@SetupActivity
        }
        val requirements = intent.getIntExtra("requirements", Requirements.FIRST_RUN)

        fun hasRequirement(requirement: Int) = requirements and requirement == requirement

        val requiredScreens = mutableListOf<SetupScreen>()

        with(requiredScreens) {
            val isFirstRun = hasRequirement(Requirements.FIRST_RUN)
            if (isFirstRun || hasRequirement(Requirements.LANGUAGE)) {
                add(PickLanguageScreen().apply { route = "language" })
            }
            if (isFirstRun || hasRequirement(Requirements.SAVE_FOLDER)) {
                add(SaveFolderScreen().apply { route = "saveFolder" })
            }
            if (isFirstRun || hasRequirement(Requirements.MAPPINGS)) {
                add(MappingsScreen().apply { route = "mappings" })
            }
        }

        // If there are no required screens, we can just finish the activity
        if (requiredScreens.isEmpty()) {
            finish()
            return
        }

        requiredScreens.forEach { screen ->
            screen.context = setupContext
            screen.init()
        }

        setContent {
            val navController = rememberNavController()
            val canGoNext = remember { mutableStateOf(false) }

            fun nextScreen() {
                if (!canGoNext.value) return
                requiredScreens.firstOrNull()?.onLeave()
                if (requiredScreens.size > 1) {
                    canGoNext.value = false
                    requiredScreens.removeFirst()
                    navController.navigate(requiredScreens.first().route)
                } else {
                    finish()
                }
            }

            AppMaterialTheme {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val alpha: Float by animateFloatAsState(if (canGoNext.value) 1f else 0f,
                                label = "NextButton"
                            )

                            FilledIconButton(
                                onClick = { nextScreen() },
                                modifier = Modifier.padding(50.dp)
                                    .width(60.dp)
                                    .height(60.dp)
                                    .alpha(alpha)
                            ) {
                                Icon(
                                    imageVector = if (requiredScreens.size <= 1 && canGoNext.value) {
                                        Icons.Default.Check
                                    } else {
                                        Icons.Default.ArrowForwardIos
                                    },
                                    contentDescription = null
                                )
                            }
                        }
                    },
                ) {
                    Column(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.background)
                            .fillMaxSize()
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = requiredScreens.first().route
                        ) {
                            requiredScreens.forEach { screen ->
                                screen.allowNext = { canGoNext.value = it }
                                composable(screen.route) {
                                    BackHandler(true) {}
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        screen.Content()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}