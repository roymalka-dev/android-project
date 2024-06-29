package com.example.socialmedia

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherFragment : Fragment() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var temperatureTextView: TextView
    private val apiKey = "820b8897d03bfd06499570b01db95605"
    private val rishonLezionLatitude = 31.9716 // Latitude of Rishon LeZion
    private val rishonLezionLongitude = 34.7894 // Longitude of Rishon LeZion

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_weather, container, false)
        temperatureTextView = view.findViewById(R.id.temperatureTextView)
        Log.d("WeatherFragment", "onCreateView called")
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("WeatherFragment", "onViewCreated called")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        fetchWeather(rishonLezionLatitude, rishonLezionLongitude)
    }

    private fun fetchWeather(latitude: Double, longitude: Double) {
        Log.d("WeatherFragment", "Fetching weather for lat: $latitude, lon: $longitude")
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(OpenWeatherApi::class.java)
        api.getCurrentWeather(latitude, longitude, apiKey).enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                if (response.isSuccessful) {
                    val weatherResponse = response.body()
                    val temp = weatherResponse?.main?.temp ?: "N/A"
                    val windSpeed = weatherResponse?.wind?.speed ?: "N/A"
                    Log.d("WeatherFragment", "Weather fetched: $temp°C, Wind speed: $windSpeed m/s")
                    temperatureTextView.text = "Temperature in Rishon LeZion: $temp°C\nWind Speed: $windSpeed m/s"
                } else {
                    Log.e("WeatherFragment", "Error fetching weather data: ${response.errorBody()?.string()}")
                    temperatureTextView.text = "Error fetching weather data"
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                Log.e("WeatherFragment", "Failed to fetch weather data", t)
                temperatureTextView.text = "Failed to fetch weather data"
            }
        })
    }
}
