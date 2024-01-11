package com.google.mediapipe.examples.poselandmarker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Gallery
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.mediapipe.examples.poselandmarker.fragment.CameraFragment
import com.google.mediapipe.examples.poselandmarker.fragment.GalleryFragment
import com.google.mediapipe.tasks.components.containers.Landmark
import org.w3c.dom.Text
import kotlin.math.acos
import kotlin.math.sqrt

class VideoCameraActivity : AppCompatActivity(), DataTransfer {

    private lateinit var fragmentManager: FragmentManager
    private var videoVector : LandmarkVector? = null
    private var livestreamVector : LandmarkVector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_camera)

        fragmentManager = supportFragmentManager

        val cameraFragment = CameraFragment()
        val galleryFragment = GalleryFragment()

        val videoFragmentLayout = findViewById<ConstraintLayout>(R.id.videoFragmentLayout)
        val cameraFragmentLayout = findViewById<ConstraintLayout>(R.id.cameraFragmentLayout)


        //Pair of relevant joint IDs that need to be monitored
        val pairs = listOf(
            Pair(13, 15)
        )

        Log.i("Pairs", pairs.toString())

        cameraFragment.setJointList(pairs)
        galleryFragment.setJointList(pairs)

        replaceFragment(cameraFragment, cameraFragmentLayout.id, pairs)
        replaceFragment(galleryFragment, videoFragmentLayout.id, pairs)
    }

    private fun replaceFragment(fragment: Fragment, id: Int, jointPairs : List<Pair<Int, Int>>) {
        val transaction: FragmentTransaction = fragmentManager.beginTransaction()
        transaction.replace(id, fragment)
        transaction.commit()
    }

    //Function that takes a LandmarkVector as input and returns its unit vector
    private fun unitVector(landmark : LandmarkVector?) : LandmarkVector {

        if (landmark == null)
        {
            return LandmarkVector(0f,0f,0f)
        }

        val x = landmark.x
        val y = landmark.y
        val z = landmark.z

        val magnitude = sqrt(x*x + y*y + z*z)

        val normalizedX = x/magnitude
        val normalizedY = y/magnitude
        val normalizedZ = z/magnitude

        return LandmarkVector(normalizedX, normalizedY, normalizedZ)
    }

    private fun LandmarkVector.dot(other: LandmarkVector) : Float {
        return x * other.x + y * other.y + z * other.z
    }

    fun radiansToDegrees(radians: Float): Float {
        return Math.toDegrees(radians.toDouble()).toFloat()
    }

    private fun angleBetweenRadians() : Float {

        //Normalize the vectors
        val unitVideoVector = unitVector(videoVector)
        val unitLivestreamVector = unitVector(livestreamVector)

        val dotProduct =unitVideoVector.dot(unitLivestreamVector).coerceIn(-1.0f, 1.0f)
        val angleRadians = acos(dotProduct)
        val angleDegrees: Float = radiansToDegrees(angleRadians)
//        Log.i("Angle Degrees", angleDegrees.toString())

        //Use UI Thread for real-time UI updates
        this.runOnUiThread {
            val tvAngle = findViewById<TextView>(R.id.tvAngle)
            tvAngle.text = angleDegrees.toString()

        }

        return angleRadians
    }

    override fun transferVideoLandmarkVector(landmarkVector: LandmarkVector?) {
//        Log.i("VideoVector", landmarkVector.toString())
        //Set values
        videoVector = landmarkVector
        val angleRadians = angleBetweenRadians()
    }

    override fun transferLivestreamLandmarkVector(landmarkVector: LandmarkVector?) {
//        Log.i("LivestreamVector", landmarkVector.toString())
        livestreamVector = landmarkVector
        val angleRadians = angleBetweenRadians()
    }
}