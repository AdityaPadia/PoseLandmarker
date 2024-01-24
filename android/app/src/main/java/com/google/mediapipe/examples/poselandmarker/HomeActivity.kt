package com.google.mediapipe.examples.poselandmarker

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.room.Room

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()

        val bundle1 = Bundle()
        val bundle2 = Bundle()

        val exercise1 = Exercise(
            1,
            "Arms",
            "Arm Workout Description",
            "android.resource://$packageName/${R.raw.arms}",
            listOf(
                Pair(12, 14),
                Pair(11, 13),
                Pair(13, 15),
                Pair(14, 16)
            ),
            null
            )

        val exercise2 = Exercise(
            2,
            "Legs and Hips",
            "Legs and Hips Description",
            "android.resource://$packageName/${R.raw.legsandhips}",
            listOf(
                Pair(23, 24),
                Pair(24, 26),
                Pair(23, 25),
                Pair(26, 28),
                Pair(25, 27),
            ),
            null
        )


        bundle1.putString("exerciseName", exercise1.name)
        bundle1.putString("exerciseDescription", exercise1.description)
        bundle1.putString("uri", exercise1.uri)
        bundle1.putString("pairs", exercise1.pairs.toString())


        val fragment1 = ItemFragment()
        fragment1.arguments = bundle1
        fragmentTransaction.add(R.id.fragmentContainer, fragment1)

        bundle2.putString("exerciseName", exercise2.name)
        bundle2.putString("exerciseDescription", exercise2.description)
        bundle2.putString("uri", exercise2.uri)
        bundle2.putString("pairs", exercise2.pairs.toString())


        val fragment2 = ItemFragment()
        fragment2.arguments = bundle2
        fragmentTransaction.add(R.id.fragmentContainer, fragment2)

        fragmentTransaction.commit()
    }
}