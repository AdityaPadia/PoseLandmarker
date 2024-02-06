package com.google.mediapipe.examples.poselandmarker

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.room.Room

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val tvWelcomeBack = findViewById<TextView>(R.id.tvWelcomeBack)
        val sharedPref: SharedPreferences = getSharedPreferences("userprefs", Context.MODE_PRIVATE)
        val name = sharedPref.getString("name", "")

        tvWelcomeBack.text = "Welcome $name!"

        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()

        val bundle1 = Bundle()
        val bundle2 = Bundle()
        val bundle3 = Bundle()
        val bundle4 = Bundle()
        val bundle5 = Bundle()

        val exercise1 = Exercise(
            1,
            "Isotonic Flexion",
            "Shoulder and Arm Strengthening Exercise",
            "android.resource://$packageName/${R.raw.isotonic_flexsion}",
            R.raw.arms,
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
            "Isotonic Scaption",
            "Shoulder and Arm Strengthening Exercise",
            "android.resource://$packageName/${R.raw.isotonic_scaption}",
            R.raw.arms,
            listOf(
                Pair(12, 14),
                Pair(11, 13),
                Pair(13, 15),
                Pair(14, 16)
            ),
            null
        )

        val exercise3 = Exercise(
            3,
            "Legs and Hips",
            "Legs and Hips Description",
            "android.resource://$packageName/${R.raw.legsandhips}",
            R.raw.legs,
            listOf(
                Pair(23, 24),
                Pair(24, 26),
                Pair(23, 25),
                Pair(26, 28),
                Pair(25, 27),
            ),
            null
        )


        val exercise4 = Exercise(
            4,
            "Hip Internal Rotation",
            "Hip Internal Rotation",
            "android.resource://$packageName/${R.raw.hip_internal_rotation}",
            R.raw.legs,
            listOf(
                Pair(23, 24),
                Pair(24, 26),
                Pair(23, 25),
                Pair(26, 28),
                Pair(25, 27),
            ),
            null
        )

        val exercise5 = Exercise(
            5,
            "Short Arc Quad",
            "Leg Strengthening Exercise",
            "android.resource://$packageName/${R.raw.short_arc_quad}",
            R.raw.legs,
            listOf(
                Pair(24, 26),
                Pair(23, 25),
                Pair(26, 28),
                Pair(25, 27),
            ),
            null
        )

        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)


        bundle1.putString("exerciseName", exercise1.name)
        bundle1.putString("exerciseDescription", exercise1.description)
        bundle1.putString("uri", exercise1.uri)
        bundle1.putString("pairs", exercise1.pairs.toString())
        bundle1.putString("imageResID", exercise1.resID.toString())


        val fragment1 = ItemFragment()
        fragment1.arguments = bundle1
        fragmentTransaction.add(R.id.fragmentContainer, fragment1)

        bundle2.putString("exerciseName", exercise2.name)
        bundle2.putString("exerciseDescription", exercise2.description)
        bundle2.putString("uri", exercise2.uri)
        bundle2.putString("pairs", exercise2.pairs.toString())
        bundle2.putString("imageResID", exercise2.resID.toString())


        val fragment2 = ItemFragment()
        fragment2.arguments = bundle2
        fragmentTransaction.add(R.id.fragmentContainer, fragment2)

        bundle4.putString("exerciseName", exercise3.name)
        bundle4.putString("exerciseDescription", exercise3.description)
        bundle4.putString("uri", exercise3.uri)
        bundle4.putString("pairs", exercise3.pairs.toString())
        bundle4.putString("imageResID", exercise4.resID.toString())

        val fragment4 = ItemFragment()
        fragment4.arguments = bundle4
        fragmentTransaction.add(R.id.fragmentContainer, fragment4)


        bundle5.putString("exerciseName", exercise4.name)
        bundle5.putString("exerciseDescription", exercise4.description)
        bundle5.putString("uri", exercise4.uri)
        bundle5.putString("pairs", exercise4.pairs.toString())
        bundle5.putString("imageResID", exercise4.resID.toString())

        val fragment5 = ItemFragment()
        fragment5.arguments = bundle5
        fragmentTransaction.add(R.id.fragmentContainer, fragment5)

        bundle3.putString("exerciseName", exercise5.name)
        bundle3.putString("exerciseDescription", exercise5.description)
        bundle3.putString("uri", exercise5.uri)
        bundle3.putString("pairs", exercise5.pairs.toString())
        bundle3.putString("imageResID", exercise5.resID.toString())

        val fragment3 = ItemFragment()
        fragment3.arguments = bundle3
        fragmentTransaction.add(R.id.fragmentContainer, fragment3)

        fragmentTransaction.commit()
    }
}