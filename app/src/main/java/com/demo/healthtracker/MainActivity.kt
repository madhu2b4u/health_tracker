package com.demo.healthtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SocialDistance
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.demo.healthtracker.bloodoxygen.BloodOxygenScreen
import com.demo.healthtracker.bloodpressure.BloodPressureScreen
import com.demo.healthtracker.bmi.BmiScreen
import com.demo.healthtracker.distance.DistanceScreen
import com.demo.healthtracker.heartrate.HeartRateScreen
import com.demo.healthtracker.mindfullness.MindfulnessScreen
import com.demo.healthtracker.respiratoryrate.RespiratoryScreen
import com.demo.healthtracker.service.HealthDataMonitor
import com.demo.healthtracker.service.ui.HealthDataOverview
import com.demo.healthtracker.service2.HealthDataMonitorV2
import com.demo.healthtracker.service2.HealthOverviewScreen2
import com.demo.healthtracker.sleep.SleepScreen
import com.demo.healthtracker.steps.StepsScreen
import com.demo.healthtracker.ui.theme.HealthTrackerTheme
import com.demo.healthtracker.workout.WorkoutScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var healthDataMonitor: HealthDataMonitor

    @Inject
    lateinit var healthDataMonitorV2: HealthDataMonitorV2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            healthDataMonitor.startMonitoring()
            healthDataMonitorV2.startMonitoring()

            HealthTrackerTheme {
                var isPermissionGranted by remember { mutableStateOf(false) }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (!isPermissionGranted) {
                        HealthPermissionsScreen(
                            onPermissionsGranted = {
                                isPermissionGranted = true
                            }
                        )
                    } else {
                        HealthAppNavigation()
                    }
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        healthDataMonitor.stopMonitoring()
        healthDataMonitorV2.stopMonitoring()
    }

    override fun onResume() {
        super.onResume()
        healthDataMonitor.startMonitoring()
        healthDataMonitorV2.startMonitoring()
    }

    override fun onPause() {
        super.onPause()
        healthDataMonitor.stopMonitoring()
        healthDataMonitorV2.stopMonitoring()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthAppNavigation() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()

    val currentTitle = when (currentBackStackEntry?.destination?.route) {
        "home" -> "Health Tracker"
        "steps" -> "Steps Tracking"
        "sleep" -> "Sleep Tracking"
        "heartrate" -> "Heart Rate"
        else -> "Health Tracker"
    }

    val showBackButton = currentBackStackEntry?.destination?.route != "home"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentTitle) },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") {
                HomeScreen(navController)
            }
            composable("steps") {
                StepsScreen()
            }
            composable("sleep") {
                SleepScreen()
            }
            composable("heartrate") {
                HeartRateScreen()
            }
            composable("bmi") {
                BmiScreen()
            }
            composable("bloodoxygen") {
                BloodOxygenScreen()
            }
            composable("bloodpressure") {
                BloodPressureScreen()
            }

            composable("respiratory") {
                RespiratoryScreen()
            }

            composable("workout") {
                WorkoutScreen()
            }
            composable("distance") {
                DistanceScreen()
            }
            composable("mindfullwellness") {
                MindfulnessScreen()
            }
            composable("overview") {
                HealthDataOverview()
            }
            composable("overview2") {
                HealthOverviewScreen2()
            }
        }
    }
}

data class HealthFeature(
    val title: String,
    val icon: ImageVector,
    val route: String
)

data class HealthCategory(
    val category: String,
    val features: List<HealthFeature>
)

@Composable
fun HomeScreen(navController: NavController) {
    val healthCategories = listOf(
        HealthCategory(
            "Overview",
            listOf(
                HealthFeature("Health Overview", Icons.Default.Dashboard, "overview"),
                HealthFeature("Health Overview V2", Icons.Default.Dashboard, "overview2")
            )
        ),
        HealthCategory(
            "Activity",
            listOf(
                HealthFeature("Steps Tracking", Icons.AutoMirrored.Filled.DirectionsWalk, "steps"),
                HealthFeature("Workouts", Icons.Default.FitnessCenter, "workout"),
                HealthFeature("Distance", Icons.Default.SocialDistance, "distance")
            )
        ),
        HealthCategory(
            "Vitals",
            listOf(
                HealthFeature("Heart Rate", Icons.Default.Favorite, "heartrate"),
                HealthFeature("Blood Pressure", Icons.Default.MonitorHeart, "bloodpressure"),
                HealthFeature("Blood Oxygen", Icons.Default.Bloodtype, "bloodoxygen"),
                HealthFeature("Respiratory Rate", Icons.Default.Air, "respiratory")
            )
        ),
        HealthCategory(
            "Body Measurements",
            listOf(
                HealthFeature("BMI Tracking", Icons.Default.Scale, "bmi")
            )
        ),
        HealthCategory(
            "Sleep & Wellness",
            listOf(
                HealthFeature("Sleep Tracking", Icons.Default.Bedtime, "sleep"),
                HealthFeature("Mindful Wellness", Icons.Default.SelfImprovement, "mindfullwellness")
            )
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        healthCategories.forEach { category ->
            item {
                CategoryHeader(category.category)
            }

            items(category.features) { feature ->
                FeatureButton(
                    icon = feature.icon,
                    text = feature.title,
                    onClick = { navController.navigate(feature.route) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun CategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun FeatureButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}