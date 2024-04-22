package com.google.mediapipe.examples.poselandmarker

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ProfileActivity : AppCompatActivity() {

    private var exerciseNames = HashSet<String>()
    private lateinit var barChart : BarChart
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.profile_activity
        barChart = findViewById<BarChart>(R.id.barChart)
        val signOutButton = findViewById<Button>(R.id.signOutButton)


        signOutButton.setOnClickListener {
            //Clear Shared Preferences
            getSharedPreferences("userprefs", Context.MODE_PRIVATE).edit().clear().apply()

            //Sign Out
            try {
                Firebase.auth.signOut()
            } catch (e: Exception) {
                Log.i("Sign Out", "Sign out failed")
            }




            //Go to Main Activity
            Intent(this, MainActivity::class.java).also {
                startActivity(it)
            }
            finish()
            true
        }


        val tvName = findViewById<TextView>(R.id.tvName)
        val sharedPref: SharedPreferences = getSharedPreferences("userprefs", Context.MODE_PRIVATE)
        val name = sharedPref.getString("name", "")

        // "null" check for display name to prevent "Hello null!"
        if (name != "null") {
            tvName.text = "Hello $name!"
        }
        else {
            tvName.text = "Hello!"
        }


        val rgDifficulty = findViewById<RadioGroup>(R.id.rgDifficulty)
        val rbEasy = findViewById<RadioButton>(R.id.rbEasy)
        val rbMedium = findViewById<RadioButton>(R.id.rbMedium)
        val rbHard = findViewById<RadioButton>(R.id.rbHard)

        val difficulty = sharedPref.getInt("difficulty", 0)

        if (difficulty == 0 || difficulty == 1) { //If not set or easy -> set to easy
            rgDifficulty.check(rbEasy.id)
        }
        else if (difficulty == 2) {
            rgDifficulty.check(rbMedium.id)
        }
        else if (difficulty == 3) {
            rgDifficulty.check(rbHard.id)
        }



        //Set the difficulty in shared preferences to maintain a global state of the application
        // 1 : Easy
        // 2 : Medium
        // 3 : Hard

        rgDifficulty.setOnCheckedChangeListener { _, id ->
            when (id) {
                rbEasy.id -> {
                    sharedPref.edit().remove("difficulty")
                    sharedPref.edit().putInt("difficulty", 1).apply()
                    Toast.makeText(this, "Difficulty Set to Easy", Toast.LENGTH_SHORT).show()

                }
                rbMedium.id -> {
                    sharedPref.edit().remove("difficulty")
                    sharedPref.edit().putInt("difficulty", 2).apply()
                    Toast.makeText(this, "Difficulty Set to Medium", Toast.LENGTH_SHORT).show()
                }
                rbHard.id -> {
                    sharedPref.edit().remove("difficulty")
                    sharedPref.edit().putInt("difficulty", 3).apply()
                    Toast.makeText(this, "Difficulty Set to Hard", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Log.i("RadioGroup Error", "id does not match existing ids")
                }
            }
        }

        bottomNav.setOnItemSelectedListener {
            when(it.itemId) {
                R.id.home_activity -> {
                    Intent(this, HomeActivity::class.java).also {
                        startActivity(it)
                    }
                    finish()
                    overridePendingTransition(com.google.android.material.R.anim.m3_side_sheet_enter_from_left, com.google.android.material.R.anim.m3_side_sheet_exit_to_right)
                    true
                }
                R.id.profile_activity -> {
                    true
                }
                else -> false
            }
        }



        getUsersExercises()

    }

    //Go to Firebase and get all the different exercises that the user has performed
    private fun getUsersExercises() {
        val exerciseCollectionRef = Firebase.firestore.collection("Exercise Performance Collection")
        val currentFirebaseUser = FirebaseAuth.getInstance().currentUser
        val uid = currentFirebaseUser!!.uid
        val llYourProgression = findViewById<LinearLayout>(R.id.llYourProgression)

        exerciseCollectionRef
            .whereEqualTo("user_id", uid)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val exerciseName = document.data["exercise_name"].toString()
                    exerciseName?.let {
                        if (exerciseName != "null") {
                            exerciseNames.add(exerciseName)
                        }
                    }
                }

                Log.i("getUsersExercise", exerciseNames.toString())

                //Update the ScrollView
                for (exerciseName in exerciseNames) {
                    val tvExercise = TextView(this)
                    tvExercise.text = exerciseName
                    tvExercise.textSize = 18F
                    tvExercise.setPadding(30, 30, 30, 30)
                    tvExercise.background = getDrawable(R.drawable.your_progression_textview_background)

                    tvExercise.setOnClickListener {
                        getDataForThisExercise(exerciseName)
                    }

                    llYourProgression.addView(tvExercise)
                }

            }
            .addOnFailureListener { exception ->
                Log.w("getUsersExercise ", exception)
            }
    }

    private fun getDataForThisExercise(exerciseName: String) {
        val exerciseCollectionRef = Firebase.firestore.collection("Exercise Performance Collection")
        val currentFirebaseUser = Firebase.auth.currentUser
        val uid = currentFirebaseUser!!.uid
        val barEntries: ArrayList<BarEntry> = ArrayList()

        exerciseCollectionRef
            .whereEqualTo("user_id", uid)
            .whereEqualTo("exerciseName", exerciseName)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { documents ->
                var i = 0
                for (document in documents) {

                    val performance = document.data["performance"] as Long
                    val timestamp = document.data["timestamp"] as Timestamp

                    barEntries.add(
                        BarEntry(
                            i++.toFloat(),
                            performance.toFloat()
                        )
                    )
                }

                Log.i("getThisUserExercise", barEntries.toString())
                val barDataSet =  BarDataSet(barEntries)
                barDataSet.setDrawValues(false)
                barChart.data = BarData(barDataSet)
                barChart.animateY(1000)
                barChart.description.text = exerciseName
            }
            .addOnFailureListener { exception ->
                Log.w("getThisUserExercise ", exception)
            }


    }
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}