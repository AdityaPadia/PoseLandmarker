package com.google.mediapipe.examples.poselandmarker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.RatingBar
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlin.math.max


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
            val exerciseName = args.getString("exerciseName").toString()

            Log.i("exerciseName", exerciseName)
            Log.i("mistakesPerMinute", mistakesPerMinute)
            Log.i("mistakesPerMinuteFloat", mistakesPerMinute.toFloat().toString())

            val mistakesPerMinuteFloat = max(mistakesPerMinute.toFloat(), 0.0f);

            if (mistakesPerMinuteFloat <= 4) {
                ratingBar.rating = 5F
            } else if (mistakesPerMinuteFloat > 4 && mistakesPerMinuteFloat <= 8) {
                ratingBar.rating = 4F
            } else if (mistakesPerMinuteFloat > 8 && mistakesPerMinuteFloat <= 12) {
                ratingBar.rating = 3F
            } else if (mistakesPerMinuteFloat > 12 && mistakesPerMinuteFloat <= 16) {
                ratingBar.rating = 2F
            } else {
                ratingBar.rating = 1F
            }

            //Display Mistakes Per Minute On Screen
//            val tvExerciseCounter = findViewById<TextView>(R.id.tvMistakesCounter)
//            tvExerciseCounter.text = mistakesPerMinuteFloat.toString()

            uploadPerformanceDataToFirestore(ratingBar.rating.toInt(), exerciseName)
        }
    }

    private fun uploadPerformanceDataToFirestore(rating: Int, exerciseName : String) {
        val exerciseCollectionRef = Firebase.firestore.collection("Exercise Performance Collection")

        val currentFirebaseUser = FirebaseAuth.getInstance().currentUser
        val uid = currentFirebaseUser!!.uid

        val data = hashMapOf(
            "exerciseName" to exerciseName,
            "performance" to rating,
            "timestamp" to Timestamp.now(),
            "user_id" to uid
        )

        exerciseCollectionRef
            .add(data)
            .addOnSuccessListener { documentReference ->
                Log.d("Firestore Upload", "DocumentSnapshot written with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore Upload", "Error adding document", e)
            }
    }

    override fun onBackPressed() {
        finish()
    }
}