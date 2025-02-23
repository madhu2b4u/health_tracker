package com.demo.healthtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.SocialDistance
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.demo.healthtracker.sleep.SleepScreen
import com.demo.healthtracker.steps.StepsScreen
import com.demo.healthtracker.ui.theme.HealthTrackerTheme
import com.demo.healthtracker.workout.WorkoutScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
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
        }
    }
}

@Composable
fun HomeScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FeatureButton(
            icon = Icons.AutoMirrored.Filled.DirectionsWalk,
            text = "Steps Tracking",
            onClick = { navController.navigate("steps") }
        )

        FeatureButton(
            icon = Icons.Default.Bedtime,
            text = "Sleep Tracking",
            onClick = { navController.navigate("sleep") }
        )

        FeatureButton(
            icon = Icons.Default.Favorite,
            text = "Heart Rate",
            onClick = { navController.navigate("heartrate") }
        )

        FeatureButton(
            icon = Icons.Default.Scale,
            text = "BMI Tracking",
            onClick = { navController.navigate("bmi") }
        )

        FeatureButton(
            icon = Icons.Default.Bloodtype,
            text = "Blood Oxygen",
            onClick = { navController.navigate("bloodoxygen") }
        )

        FeatureButton(
            icon = Icons.Default.MonitorHeart,
            text = "Blood Pressure",
            onClick = { navController.navigate("bloodpressure") }
        )

        FeatureButton(
            icon = Icons.Default.MonitorHeart,
            text = "Respiratory Rate",
            onClick = { navController.navigate("respiratory") }
        )

        FeatureButton(
            icon = Icons.Default.FitnessCenter,
            text = "Workouts",
            onClick = { navController.navigate("workout") }
        )
        FeatureButton(
            icon = Icons.Default.SocialDistance,
            text = "Distance",
            onClick = { navController.navigate("distance") }
        )

        FeatureButton(
            icon = Icons.Default.SocialDistance,
            text = "MindFull Wellness",
            onClick = { navController.navigate("mindfullwellness") }
        )
    }
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