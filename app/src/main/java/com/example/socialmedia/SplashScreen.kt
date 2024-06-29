package com.example.socialmedia


import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashScreen : AppCompatActivity() {

    private lateinit var logoImageView: ImageView
    private lateinit var appNameTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        logoImageView = findViewById(R.id.logoImageView)
        appNameTextView = findViewById(R.id.appNameTextView)

        // Animation for logoImageView (move from left to center)
        ObjectAnimator.ofFloat(logoImageView, "translationX", -500f, 0f).apply {
            duration = 1500
            interpolator = DecelerateInterpolator()
            start()
        }

        // Animation for appNameTextView (move from bottom to its position)
        ValueAnimator.ofInt(0, 1).apply {
            addUpdateListener { valueAnimator ->
                val fractionAnim = valueAnimator.animatedFraction
                appNameTextView.alpha = fractionAnim
                appNameTextView.translationY = -appNameTextView.height * (1 - fractionAnim)
            }
            duration = 1500
            interpolator = DecelerateInterpolator()
            start()
        }

        // Delay for 2 seconds after animations complete, then navigate to LoginSignupActivity
        Handler().postDelayed({
            val intent = Intent(this@SplashScreen, MainActivity::class.java)
            startActivity(intent)
            finish() // Finish the splash screen activity
        }, 2000) // 2000 milliseconds = 2 seconds delay
    }
}
