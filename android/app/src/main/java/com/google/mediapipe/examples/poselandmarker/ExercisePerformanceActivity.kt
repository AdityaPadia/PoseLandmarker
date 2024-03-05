package com.google.mediapipe.examples.poselandmarker

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.RatingBar
import android.widget.TextView

class ExercisePerformanceActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_performance)

        val ratingBar = findViewById<RatingBar>(R.id.ratingBar)
        val btnGoHome = findViewById<Button>(R.id.btnGoHome)

        btnGoHome.setOnClickListener {
            Intent(applicationContext, HomeActivity::class.java).also {
                startActivity(it)
                finish()
            }
        }

        val args = intent.extras

        if (args != null) {
            val mistakesPerMinute = args.getString("mistakesPerMinute").toString()
            Log.i("mistakesPerMinute", mistakesPerMinute)
            Log.i("mistakesPerMinuteFloat", mistakesPerMinute.toFloat().toString())


            if (mistakesPerMinute.toFloat() <= 4) {
                ratingBar.rating = 5F
            } else if (mistakesPerMinute.toFloat() > 4 && mistakesPerMinute.toFloat() <= 8) {
                ratingBar.rating = 4F
            } else if (mistakesPerMinute.toFloat() > 8 && mistakesPerMinute.toFloat() <= 12) {
                ratingBar.rating = 3F
            } else if (mistakesPerMinute.toFloat() > 12 && mistakesPerMinute.toFloat() <= 16) {
                ratingBar.rating = 2F
            } else {
                ratingBar.rating = 1F
            }

            
            val tvExerciseCounter = findViewById<TextView>(R.id.tvMistakesCounter)
            tvExerciseCounter.text = mistakesPerMinute
        }
    }

    override fun onBackPressed() {
        finish()
    }
}