package com.example.compassdigitallevel

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.compassdigitallevel.ui.theme.CompassDigitalLevelTheme

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
    private var rotationMatrix = FloatArray(9)
    private var orientation = FloatArray(3)

    private var gyroValues = FloatArray(3)

    private var rPin: Float = 0f
    private var roll: Float = 0f
    private var pitch: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        setContent {
            SensorUI(rPin, roll, pitch)
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
                Sensor.TYPE_ACCELEROMETER -> gravity = event.values
                Sensor.TYPE_MAGNETIC_FIELD -> geomagnetic = event.values
                Sensor.TYPE_GYROSCOPE -> gyroValues = event.values
            }

            if (gravity != null && geomagnetic != null) {
                val R = FloatArray(9)
                val I = FloatArray(9)
                if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                    SensorManager.getOrientation(R, orientation)
                    rPin = Math.toDegrees(orientation[0].toDouble()).toFloat()
                }
            }

            if (gyroValues.isNotEmpty()) {

                roll = gyroValues[0] * 180 / Math.PI.toFloat()
                pitch = gyroValues[1] * 180 / Math.PI.toFloat()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
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
                    .rotate(rotate)
            )

            Spacer(modifier = Modifier.height(20.dp))


            Text(
                text = "Roll:" + roll.toString() + " Pitch: " + pitch.toString(),
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
}
