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
import androidx.core.view.marginRight
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ProfileActivity : AppCompatActivity() {

    private var exerciseNames = HashSet<String>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.profile_activity

        val signOutButton = findViewById<Button>(R.id.signOutButton)

        signOutButton.setOnClickListener {
            //Clear Shared Preferences
            getSharedPreferences("userprefs", Context.MODE_PRIVATE).edit().clear().apply()

            //Sign Out
            FirebaseAuth.getInstance().signOut()

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
        tvName.text = "Hello $name!"

        val rgDifficulty = findViewById<RadioGroup>(R.id.rgDifficulty)
        val rbEasy = findViewById<RadioButton>(R.id.rbEasy)
        val rbMedium = findViewById<RadioButton>(R.id.rbMedium)
        val rbHard = findViewById<RadioButton>(R.id.rbHard)



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
                    val tv = TextView(this)
                    tv.text = exerciseName
                    tv.textSize = 18F
                    tv.setPadding(30, 30, 30, 30)
                    tv.background = getDrawable(R.drawable.your_progression_textview_background)

                    llYourProgression.addView(tv)
                }

            }
            .addOnFailureListener { exception ->
                Log.w("getUsersExercise ", exception)
            }
    }


    //Go to Firebase and get all performance data of the user with this user UID
    private fun getUsersPerformanceData() {
        val exerciseCollectionRef = Firebase.firestore.collection("Exercise Performance Collection")
        val currentFirebaseUser = FirebaseAuth.getInstance().currentUser
        val uid = currentFirebaseUser!!.uid

        exerciseCollectionRef
            .whereEqualTo("user_id", uid)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    Log.d("getUsersPerformanceData", "${document.id} => ${document.data}")
                }
            }
            .addOnFailureListener { exception ->
                Log.w("getUsersPerformanceData ", exception)
            }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}