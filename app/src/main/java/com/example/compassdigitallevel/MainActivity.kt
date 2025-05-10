package com.example.compassdigitallevel

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.compassdigitallevel.ui.theme.CompassDigitalLevelTheme
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.sqrt


// IMPORTANT NOTE: Z and X Rotation are the primary rotators

class MainActivity : ComponentActivity() {
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    private var gyroscope: Sensor? = null

    // Used chatgpt to find a way to rotate pin to reflect the gyroscope,
    // I know how to rotate the image, but I don't know how should the pin/image rotates
    // relative to the gyroscope

    private var gravity: FloatArray? = null
    private var geomagnetic: FloatArray? = null

    private var gyroValues = FloatArray(3)

    private var rPin = mutableFloatStateOf(0f)
    private var roll = mutableFloatStateOf(0f)
    private var pitch = mutableFloatStateOf(0f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        setContent {
            CompassDigitalLevelTheme{

                SensorUI(rPin.value, roll.value, pitch.value)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(sensorEventListener, magnetometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(sensorEventListener, gyroscope, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(sensorEventListener)
    }

    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event == null) return

            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> gravity = event.values.clone()
                Sensor.TYPE_MAGNETIC_FIELD -> geomagnetic = event.values.clone()
                Sensor.TYPE_GYROSCOPE -> gyroValues = event.values.clone()
            }

            if (gravity != null && geomagnetic != null) {
                val R = FloatArray(9)
                val I = FloatArray(9)
                if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(R, orientation)
                    val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    rPin.value = (azimuth + 360) % 360 // Normalize to [0, 360)
                    println("Compass rotation: ${rPin.value}")
                }
            }


            gravity?.let { g ->
                val norm = sqrt((g[0] * g[0] + g[1] * g[1] + g[2] * g[2]).toDouble())
                val gx = g[0] / norm
                val gy = g[1] / norm
                val gz = g[2] / norm

                pitch.value = Math.toDegrees(asin(-gx)).toFloat()
                roll.value = Math.toDegrees(atan2(gy, gz)).toFloat()
            }

        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }
}

@Composable
fun SensorUI(rotate: Float, roll: Float, pitch: Float) {
    val compassImage: Painter = painterResource(id = R.drawable.compass_needle)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = compassImage,
            contentDescription = "Compass Needle",
            modifier = Modifier
                .size(200.dp)
                .rotate(-rotate) // Negative for correct compass direction
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Roll: %.2f°, Pitch: %.2f°".format(roll, pitch),
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 30.sp,
            modifier = Modifier.padding(30.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
        SensorUI(rotate = 50f, roll = 5f, pitch = 7f)
}

