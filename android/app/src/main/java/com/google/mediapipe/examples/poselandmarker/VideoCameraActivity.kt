package com.google.mediapipe.examples.poselandmarker

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.mediapipe.examples.poselandmarker.fragment.CameraFragment
import com.google.mediapipe.examples.poselandmarker.fragment.GalleryFragment
import kotlin.math.acos
import kotlin.math.sqrt

class VideoCameraActivity : AppCompatActivity(), DataTransfer {

    private lateinit var fragmentManager: FragmentManager
    private var videoVectorList = mutableListOf<LandmarkVector?>()
    private var livestreamVectorList = mutableListOf<LandmarkVector?>()
    private var uri = ""
    private var pairs = mutableListOf<Pair<Int, Int>>()

    private val cameraFragment = CameraFragment()
    private val galleryFragment = GalleryFragment()

    private fun parsePairlist(pairList : String?) : MutableList<Pair<Int, Int>> {
        var finalPairList = mutableListOf<Pair<Int, Int>>()
        if (pairList != null)
        {
            val cleanedString = pairList.trim('[', ']')
            Log.i("cleanedString", cleanedString)


            // Split the string into individual pairs
            val pairStrings = cleanedString.split("), (")
            Log.i("pairStrings", pairStrings.toString())

            for (pairString in pairStrings) {
                Log.i("pairString", pairStrings.size.toString())
                var (first, second) = pairString.split(",").map {
                    it.replace("(", "")
                    it.replace(")", "")
                }
                first = first.substring(1, first.length)
                second = second.trim()

                Log.i("first", first)
                Log.i("second", second)
                val pair = Pair(first.toInt(), second.toInt())
                finalPairList.add(pair)
            }

//            finalPairList = pairStrings.map { pairString ->
//                var (first, second) = pairString.split(",").map {
//                    it.replace("(", "")
//                    it.replace(")", "")
//                }
//                first = first.substring(1, first.length)
//                second = second.trim()
//                Pair(first.toInt(), second.toInt())
//            }.toList()
        }

        return finalPairList
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_camera)

        val args = intent.extras
        if (args != null) {
            uri = args.getString("uri").toString()
            val pairList = args.getString("pairList")
            Log.i("pairList", pairList.toString())
            val pairsList = this.parsePairlist(pairList)


            if (pairsList != null) {
                pairs = pairsList
            }
        }


        fragmentManager = supportFragmentManager

        val videoFragmentLayout = findViewById<ConstraintLayout>(R.id.videoFragmentLayout)
        val cameraFragmentLayout = findViewById<ConstraintLayout>(R.id.cameraFragmentLayout)


//        Pair of relevant joint IDs that need to be monitored
//        pairs = mutableListOf(
//            Pair(13, 15),
//            Pair(14, 16)
//        )

        Log.i("Video Camera Activity Pairs", pairs.toString())

        cameraFragment.setJointList(pairs)
        galleryFragment.setJointList(pairs)

        replaceCameraFragment(cameraFragment, cameraFragmentLayout.id, pairs)
        replaceVideoFragment(galleryFragment, videoFragmentLayout.id, pairs, uri)
    }

    private fun replaceCameraFragment(fragment: Fragment, id: Int, jointPairs : List<Pair<Int, Int>>) {
        val transaction: FragmentTransaction = fragmentManager.beginTransaction()
        transaction.replace(id, fragment)
        transaction.commit()
    }

    private fun replaceVideoFragment(fragment: Fragment, id: Int, jointPairs : List<Pair<Int, Int>>, uri: String) {
        val bundle = Bundle()
        bundle.putString("uri", uri)
        fragment.arguments = bundle
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