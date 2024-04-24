package com.google.mediapipe.examples.poselandmarker

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.checkerframework.common.subtyping.qual.Bottom


class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val tvWelcomeBack = findViewById<TextView>(R.id.tvWelcomeBack)
        val sharedPref: SharedPreferences = getSharedPreferences("userprefs", Context.MODE_PRIVATE)
        val name = sharedPref.getString("name", "")

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.home_activity

        bottomNav.setOnItemSelectedListener {
            when(it.itemId) {
                R.id.home_activity -> {
                    true
                }
                R.id.profile_activity -> {
                    Intent(this, ProfileActivity::class.java).also {
                        startActivity(it)
                    }
                    overridePendingTransition(com.google.android.material.R.anim.m3_side_sheet_enter_from_right, com.google.android.material.R.anim.m3_side_sheet_exit_to_left)
                    finish()
                    true
                }
                else -> false
            }
        }

        // "null" check for display name to prevent "Hello null!"
        if (name != "null") {
            tvWelcomeBack.text = "Welcome $name!"
        } else {
            tvWelcomeBack.text = "Welcome!"
        }


        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()

        val bundle1 = Bundle()
        val bundle2 = Bundle()
        val bundle3 = Bundle()
        val bundle4 = Bundle()
        val bundle5 = Bundle()
        val bundle6 = Bundle()
        val bundle7 = Bundle()

        val exercise1 = Exercise(
            1,
            "Isotonic Flexion",
            "Low-impact movement for strengthening muscles and joints",
            "android.resource://$packageName/${R.raw.isotonic_flexion}",
            "isotonicFlexion.txt",
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
            "Shoulder-strengthening exercise with free-hand movement",
            "android.resource://$packageName/${R.raw.isotonic_scaption}",
            "isotonicScaption.txt",
            R.raw.shoulders,
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
            "Hip Internal Rotation",
            "Strengthens hip muscles, improves range of motion",
            "android.resource://$packageName/${R.raw.hip_internal_rotation}",
            "hipInternalRotation.txt",
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
            "Short Arc Quad",
            "Strengthens quadriceps, improves knee stability",
            "android.resource://$packageName/${R.raw.short_arc_quad}",
            "shortArcQuad.txt",
            R.raw.legs,
            listOf(
                Pair(24, 26),
                Pair(23, 25),
                Pair(26, 28),
                Pair(25, 27),
            ),
            null
        )

        val exercise5 = Exercise(
            5,
            "Neural Glide Median Bias",
            "Targeted neural mobilization for median nerve",
            "android.resource://$packageName/${R.raw.neural_glide_median_bias}",
            "neuralGlideMedianBias.txt",
            R.raw.arms,
            listOf(
                Pair(12, 14),
                Pair(14, 16),
                Pair(16, 22),
                Pair(16, 20),
            ),
            null
        )

        val exercise6 = Exercise(
            6,
            "Neural Glide Median Bias and Head Tilt",
            "Targeted neural mobilization for median nerve",
            "android.resource://$packageName/${R.raw.neural_glide_with_ulnar_bias_and_head_tilt}",
            "neuralGlideMedianBiasAndHeadTilt.txt",
            R.raw.arms,
            listOf(
                Pair(12, 14),
                Pair(14, 16),
                Pair(16, 22),
                Pair(16, 20),
                Pair(0, 2),
                Pair(2, 7),
                Pair(0, 5),
                Pair(5, 8),
            ),
            null
        )

        val exercise7 = Exercise(
            7,
            "Shoulder Flexion with Opposite Arm Isometric Hold",
            "Strengthening shoulder stabilizers with isometric contractions",
            "android.resource://$packageName/${R.raw.shoulder_flexion_with_opposite_arm_isometric_hold}",
            "shoulderFlexion.txt",
            R.raw.shoulders,
            listOf(
                Pair(12, 14),
                Pair(11, 13),
                Pair(13, 15),
                Pair(14, 16)
            ),
            null
        )

        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)


        bundle1.putString("exerciseName", exercise1.name)
        bundle1.putString("exerciseDescription", exercise1.description)
        bundle1.putString("uri", exercise1.uri)
        bundle1.putString("dataUri", exercise1.dataUri)
        bundle1.putString("pairs", exercise1.pairs.toString())
        bundle1.putString("imageResID", exercise1.resID.toString())


        val fragment1 = ItemFragment()
        fragment1.arguments = bundle1
        fragmentTransaction.add(R.id.fragmentContainer, fragment1)

        bundle2.putString("exerciseName", exercise2.name)
        bundle2.putString("exerciseDescription", exercise2.description)
        bundle2.putString("uri", exercise2.uri)
        bundle2.putString("dataUri", exercise2.dataUri)
        bundle2.putString("pairs", exercise2.pairs.toString())
        bundle2.putString("imageResID", exercise2.resID.toString())


        val fragment2 = ItemFragment()
        fragment2.arguments = bundle2
        fragmentTransaction.add(R.id.fragmentContainer, fragment2)


        bundle3.putString("exerciseName", exercise3.name)
        bundle3.putString("exerciseDescription", exercise3.description)
        bundle3.putString("uri", exercise3.uri)
        bundle3.putString("dataUri", exercise3.dataUri)
        bundle3.putString("pairs", exercise3.pairs.toString())
        bundle3.putString("imageResID", exercise3.resID.toString())

        val fragment3 = ItemFragment()
        fragment3.arguments = bundle3
        fragmentTransaction.add(R.id.fragmentContainer, fragment3)

        bundle4.putString("exerciseName", exercise4.name)
        bundle4.putString("exerciseDescription", exercise4.description)
        bundle4.putString("uri", exercise4.uri)
        bundle4.putString("dataUri", exercise4.dataUri)
        bundle4.putString("pairs", exercise4.pairs.toString())
        bundle4.putString("imageResID", exercise4.resID.toString())

        val fragment4 = ItemFragment()
        fragment4.arguments = bundle4
        fragmentTransaction.add(R.id.fragmentContainer, fragment4)

        bundle5.putString("exerciseName", exercise5.name)
        bundle5.putString("exerciseDescription", exercise5.description)
        bundle5.putString("uri", exercise5.uri)
        bundle5.putString("dataUri", exercise5.dataUri)
        bundle5.putString("pairs", exercise5.pairs.toString())
        bundle5.putString("imageResID", exercise5.resID.toString())

        val fragment5 = ItemFragment()
        fragment5.arguments = bundle5
        fragmentTransaction.add(R.id.fragmentContainer, fragment5)

        bundle6.putString("exerciseName", exercise6.name)
        bundle6.putString("exerciseDescription", exercise6.description)
        bundle6.putString("uri", exercise6.uri)
        bundle6.putString("dataUri", exercise6.dataUri)
        bundle6.putString("pairs", exercise6.pairs.toString())
        bundle6.putString("imageResID", exercise6.resID.toString())

        val fragment6 = ItemFragment()
        fragment6.arguments = bundle6
        fragmentTransaction.add(R.id.fragmentContainer, fragment6)

        bundle7.putString("exerciseName", exercise7.name)
        bundle7.putString("exerciseDescription", exercise7.description)
        bundle7.putString("uri", exercise7.uri)
        bundle7.putString("dataUri", exercise7.dataUri)
        bundle7.putString("pairs", exercise7.pairs.toString())
        bundle7.putString("imageResID", exercise7.resID.toString())

        val fragment7 = ItemFragment()
        fragment7.arguments = bundle7
        fragmentTransaction.add(R.id.fragmentContainer, fragment7)


        fragmentTransaction.commit()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}