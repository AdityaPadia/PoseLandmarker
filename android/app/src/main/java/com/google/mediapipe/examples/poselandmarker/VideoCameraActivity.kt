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
    private var videoVectorList = mutableListOf<LandmarkVector?>()
    private var livestreamVectorList = mutableListOf<LandmarkVector?>()

    private val cameraFragment = CameraFragment()
    private val galleryFragment = GalleryFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_camera)

        fragmentManager = supportFragmentManager



        val videoFragmentLayout = findViewById<ConstraintLayout>(R.id.videoFragmentLayout)
        val cameraFragmentLayout = findViewById<ConstraintLayout>(R.id.cameraFragmentLayout)


        //Pair of relevant joint IDs that need to be monitored
        val pairs = listOf(
            Pair(13, 15),
            Pair(14, 16)
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

    private fun angleBetweenDegrees() : List<Float> {



        if (videoVectorList.isNotEmpty() && livestreamVectorList.isNotEmpty())
        {
            val angleDegreeList = mutableListOf<Float>()

            for (vector in 0 until videoVectorList.size) {
                val videoVector = videoVectorList[vector] //Only use when video exists
                val livestreamVector = livestreamVectorList[vector]

                //Normalize the vectors
                val unitVideoVector = unitVector(videoVector)
                val unitLivestreamVector = unitVector(livestreamVector)

                val dotProduct =unitVideoVector.dot(unitLivestreamVector).coerceIn(-1.0f, 1.0f)
                val angleRadians = acos(dotProduct)
                val angleDegrees: Float = radiansToDegrees(angleRadians)

                angleDegreeList.add(angleDegrees)

            }

            cameraFragment.setAngleList(angleDegreeList)
            return angleDegreeList
        }

        return mutableListOf()
    }

    override fun transferVideoLandmarkVector(landmarkVectorList: List<LandmarkVector?>) {
//        Log.i("VideoVector", landmarkVector.toString())

        //Set values
        val newVideoVectorList = mutableListOf<LandmarkVector?>()

        for (landmarkVector in landmarkVectorList) {
//            val videoVector = landmarkVector
            newVideoVectorList.add(landmarkVector)
        }

        videoVectorList = newVideoVectorList
        Log.i("videoVectorList", videoVectorList.toString())
        val angleDegrees = angleBetweenDegrees()
    }

    override fun transferLivestreamLandmarkVector(landmarkVectorList: List<LandmarkVector?>) {
//        Log.i("LivestreamVector", landmarkVector.toString())

        //Set Values
        val newLivestreamVectorList = mutableListOf<LandmarkVector?>()

        for (landmarkVector in landmarkVectorList) {
            newLivestreamVectorList.add(landmarkVector)
        }

        livestreamVectorList = newLivestreamVectorList
//        Log.i("livestreamVectorList", livestreamVectorList.toString())

        val angleRadians = angleBetweenDegrees()

//        livestreamVector = landmarkVector
//        val angleRadians = angleBetweenRadians()
    }
}