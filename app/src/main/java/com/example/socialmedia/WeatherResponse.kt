package com.example.socialmedia

data class WeatherResponse(
    val main: Main,
    val wind: Wind
)

data class Main(
    val temp: Double
)

data class Wind(
    val speed: Double
)
