# MCAssignment2
Weather App
The Weather App is an Android application that allows users to get the weather forecast for a specific date and location. It retrieves weather data from an API and stores it locally in a Room database for offline access.

Features
Get weather forecast by specifying a date (YYYY-MM-DD) and location.
Display minimum and maximum temperature for the specified date and location.
Store weather data locally for offline access.
Handle past dates by fetching historical weather data.
Requirements
Android device or emulator with API level 21 or higher.
Internet connection for fetching weather data from the API.
Location permissions for retrieving weather data for a specific location.
Installation
Clone this repository to your local machine:

bash
Copy code
git clone https://github.com/JayshilShah/MCAssignment2
Open the project in Android Studio.

Build and run the application on your device or emulator.

Usage
Launch the Weather App on your Android device or emulator.

Enter the desired date (YYYY-MM-DD) and location in the respective text fields.

Tap on the "Get Weather" button to fetch the weather forecast for the specified date and location.

The minimum and maximum temperatures for the specified date and location will be displayed.

If the device is offline or if there is an error fetching data from the API, the app will retrieve weather data from the local database (if available).

Dependencies
Kotlin: Official programming language for Android development.
Room Persistence Library: Provides an abstraction layer over SQLite to store data locally.
Retrofit: HTTP client for making network requests.
kotlinx.serialization: Library for serializing and deserializing JSON data.
Material Components for Android: Official Material Design components for Android.
Coroutines: Kotlin library for asynchronous programming.