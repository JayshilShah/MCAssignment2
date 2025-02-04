package com.example.weather

import android.R.attr.value
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.collections.first as first
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Database
import androidx.room.RoomDatabase
import android.app.Application
import androidx.room.Room

@Entity(tableName = "weather_data")
data class WeatherData(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,
    val location: String,
    val tempMin: Double,
    val tempMax: Double
)

@Dao
interface WeatherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertWeather(weatherData: WeatherData)

    @Query("SELECT * FROM weather_data WHERE date = :date AND location = :location")
    fun getWeatherByDateAndLocation(date: String, location: String): WeatherData
}

@Database(entities = [WeatherData::class], version = 1, exportSchema = false)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao
}

class WeatherRepository(private val weatherDao: WeatherDao) {
    fun insertWeather(weatherData: WeatherData) {
        weatherDao.insertWeather(weatherData)
    }

    fun getWeatherByDateAndLocation(date: String, location: String): WeatherData {
        return weatherDao.getWeatherByDateAndLocation(date, location)
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var database: WeatherDatabase
    private lateinit var repository: WeatherRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = Room.databaseBuilder(applicationContext, WeatherDatabase::class.java, "weather_database").build()
        repository = WeatherRepository(database.weatherDao())
        setContent {
            WeatherAppContent(repository)
        }
    }
}

@Composable
fun WeatherAppContent(repository: WeatherRepository) {
    var date by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var tempMin by remember { mutableStateOf("") }
    var tempMax by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Weather Forecast",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        Column(
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            TextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Date(YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                getWeather(date, location, repository) { minTemp, maxTemp ->
                    // Update UI with response data
                    tempMin = minTemp
                    tempMax = maxTemp
                    error = ""
                }
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Get Weather")
        }
        Spacer(modifier = Modifier.height(16.dp))
        WeatherLayout(location, tempMin, tempMax, error)
    }
}

private fun getWeather(
    date: String,
    location: String,
    repository: WeatherRepository,
    onResult: (String, String) -> Unit
) {
    // Using Coroutines to perform the API call asynchronously
    CoroutineScope(Dispatchers.IO).launch {
        try {
            if(isInternetAvailable()){
                val url = URL("https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/timeline/$location/$date?key=MQVU6RBPNQ6NUGXF9HA2D5H28")
                val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                // Read response
                val inputStream = connection.inputStream
                val response = inputStream.bufferedReader().use(BufferedReader::readText)
                inputStream.close()

                // Parse response JSON and convert it to WeatherResponse object
                val weatherResponse = parseWeatherResponse(response)

                if (weatherResponse != null) {
                    val (minTemp, maxTemp) = weatherResponse
                    onResult(minTemp.toString(), maxTemp.toString())
                    // Save data to database
                    val weatherData = WeatherData(date = date, location = location, tempMin = minTemp, tempMax = maxTemp)
                    repository.insertWeather(weatherData)
                } else {
                    onResult("", "")
                }
            } else {
                // Fetch data from database
                val weatherData = repository.getWeatherByDateAndLocation(date, location)
                if (weatherData != null) {
                    onResult(weatherData.tempMin.toString(), weatherData.tempMax.toString())
                } else {
                    onResult("", "")
                }
            }
        } catch (e: Exception) {
            // Handle errors
            // Fetch data from database
            val weatherData = repository.getWeatherByDateAndLocation(date, location)
            if (weatherData != null) {
                onResult(weatherData.tempMin.toString(), weatherData.tempMax.toString())
            } else {
                onResult("", "")
            }
        }
    }
}

private fun isInternetAvailable(): Boolean {
    // Implement your logic to check internet connectivity
    // For example, you can use ConnectivityManager to check network availability
    return true
}

// Parse the JSON response
fun parseWeatherResponse(jsonString: String): Pair<Double, Double>? {
    return try {
        val jsonObject = Json.decodeFromString<JsonObject>(jsonString)
        val daysArray = jsonObject["days"]?.jsonArray
        val firstDay = daysArray?.firstOrNull()?.jsonObject
        val tempMin = firstDay?.get("tempmin")?.jsonPrimitive?.double ?: return null
        val tempMax = firstDay.get("tempmax")?.jsonPrimitive?.double ?: return null
        val min = (tempMin - 32) * 5 / 9
        val max = (tempMax - 32) * 5 / 9
        Pair(min, max)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun WeatherLayout(
    location: String,
    tempMin: String,
    tempMax: String,
    error: String
) {
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Location: $location", style = MaterialTheme.typography.bodyMedium)
        Text(text = "Minimum Temperature: $tempMin Degree Celsius", style = MaterialTheme.typography.bodyMedium)
        Text(text = "Maximum Temperature: $tempMax Degree Celsius", style = MaterialTheme.typography.bodyMedium)
        if (error.isNotEmpty()) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun DefaultPreview() {
//    WeatherAppContent()
//}




