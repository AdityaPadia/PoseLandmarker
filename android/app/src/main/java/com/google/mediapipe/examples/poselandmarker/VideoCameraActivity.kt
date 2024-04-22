package com.google.mediapipe.examples.poselandmarker

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.mediapipe.examples.poselandmarker.fragment.CameraFragment
import com.google.mediapipe.examples.poselandmarker.fragment.GalleryFragment
import kotlin.math.acos
import kotlin.math.sqrt



class VideoCameraActivity : AppCompatActivity(), DataTransfer, SyncInterface, MistakesCounterInterface {

    private lateinit var fragmentManager: FragmentManager
    private var videoVectorList = mutableListOf<LandmarkVector?>()
    private var livestreamVectorList = mutableListOf<LandmarkVector?>()
    private var exerciseName = ""
    private var uri = ""
    private var dataUri = ""
    private var pairs = mutableListOf<Pair<Int, Int>>()
    private var mistakesCounter = 0

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
                if (first[0] == '(') {
                    first = first.substring(1, first.length)
                    second = second.trim()
                }
                else {
                    first = first.substring(0, first.length)
                    second = second.trim()
                }


                Log.i("first", first)
                Log.i("second", second)
                val pair = Pair(first.toInt(), second.toInt())
                finalPairList.add(pair)
            }
        }

        return finalPairList
    }

    fun pauseVideo() {
        galleryFragment.pauseVideo()
    }
    fun playVideo() {
        galleryFragment.playVideo()
    }

    private fun showCameraSetupDialog() {
        val dialog = SetupCameraDialogFragment()
        dialog.show(supportFragmentManager, "Setup Camera Dialog Fragment")
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_camera)

        //Show Camera Setup Dialog
        showCameraSetupDialog()

        val args = intent.extras
        if (args != null) {
            uri = args.getString("uri").toString()
            dataUri = args.getString("dataUri").toString()
            exerciseName = args.getString("exerciseName").toString()
            val pairList = args.getString("pairList")

            Log.i("exerciseName", exerciseName.toString())
            Log.i("uri", uri.toString())
            Log.i("dataUri", dataUri.toString())
            Log.i("pairList", pairList.toString())
            val pairsList = this.parsePairlist(pairList)


            if (pairsList != null) {
                pairs = pairsList
            }
        }


        fragmentManager = supportFragmentManager

        val videoFragmentLayout = findViewById<ConstraintLayout>(R.id.videoFragmentLayout)
        val cameraFragmentLayout = findViewById<ConstraintLayout>(R.id.cameraFragmentLayout)

        Log.i("Video Camera Activity Pairs", pairs.toString())

        cameraFragment.setJointList(pairs)
        galleryFragment.setJointList(pairs)
        cameraFragment.setSyncInterface(this)



        replaceCameraFragment(cameraFragment, cameraFragmentLayout.id)
        replaceVideoFragment(galleryFragment, videoFragmentLayout.id)
    }

    private fun replaceCameraFragment(fragment: Fragment, id: Int) {
        val transaction: FragmentTransaction = fragmentManager.beginTransaction()
        transaction.replace(id, fragment)
        transaction.commit()
    }

    private fun replaceVideoFragment(fragment: Fragment, id: Int) {
        val bundle = Bundle()

        bundle.putString("uri", uri)
        bundle.putString("dataUri", dataUri)

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

    private fun radiansToDegrees(radians: Float): Float {
        return Math.toDegrees(radians.toDouble()).toFloat()
    }

    private fun angleBetweenDegrees() : List<Float> {

        val pairsLen = pairs.size
        val queueLen = 8
        val queues: MutableList<ArrayDeque<Float>> = mutableListOf()

        for (i in 0 until pairsLen) {
            queues.add(ArrayDeque(8))
        }

        if (videoVectorList.isNotEmpty() && livestreamVectorList.isNotEmpty())
        {
            val angleDegreeList = mutableListOf<Float>()

            for (vector in 0 until videoVectorList.size) {

                val videoVector = videoVectorList[vector] //Only use when video exists
                val livestreamVector = livestreamVectorList[vector]

                //Selecting the queue for the vector
                val queue = queues[vector]

                //Normalize the vectors
                val unitVideoVector = unitVector(videoVector)
                val unitLivestreamVector = unitVector(livestreamVector)

                val dotProduct =unitVideoVector.dot(unitLivestreamVector).coerceIn(-1.0f, 1.0f)
                val angleRadians = acos(dotProduct)
                val angleDegrees: Float = radiansToDegrees(angleRadians)

                if (queue.size >= 8) {
                    queue.removeFirst()
                    queue.add(angleDegrees)
                }
                else {
                    queue.add(angleDegrees)
                }

                Log.i("Queue", queue.toString())
//
                val smoothAngle = (queue.sum()/queue.size) as Float

                angleDegreeList.add(smoothAngle)
            }
            cameraFragment.setAngleList(angleDegreeList)
            return angleDegreeList
        }

        return mutableListOf()
    }

    override fun transferVideoLandmarkVector(landmarkVectorList: List<LandmarkVector?>) {
        //Set values
        val newVideoVectorList = mutableListOf<LandmarkVector?>()

        for (landmarkVector in landmarkVectorList) {
            newVideoVectorList.add(landmarkVector)
        }

        videoVectorList = newVideoVectorList
        Log.i("videoVectorList", videoVectorList.toString())
        val angleDegrees = angleBetweenDegrees()
    }

    override fun transferLivestreamLandmarkVector(landmarkVectorList: List<LandmarkVector?>) {

        //Set Values
        val newLivestreamVectorList = mutableListOf<LandmarkVector?>()

        for (landmarkVector in landmarkVectorList) {
            newLivestreamVectorList.add(landmarkVector)
        }

        livestreamVectorList = newLivestreamVectorList

        val angleRadians = angleBetweenDegrees()
    }

    override fun onVideoPause() {
        Log.i("Sync Interface", "Pause Video")

        if (galleryFragment.isVideoPlaying()) {
            mistakesCounter++
            Log.i("mistakesCounter", mistakesCounter.toString())
            pauseVideo()
        }

    }

    override fun onVideoPlay() {
        Log.i("Sync Interface", "Play Video")

        if (!galleryFragment.isVideoPlaying()) {
            playVideo()
        }
    }
    override fun processExerciseFinished() {
        Log.i("mistakesCounter", mistakesCounter.toString())
        val durationMs = galleryFragment.getVideoLength().toFloat()
        Log.i("mistakesCounterDurationSeconds", durationMs.toString())
        val durationMinutes = durationMs/60000f;
        Log.i("mistakesCounterDurationMinutes", durationMinutes.toString())
        val mistakesPerMinute = mistakesCounter / durationMinutes
        Intent(applicationContext, ExercisePerformanceActivity::class.java).also {
            it.putExtra("mistakesPerMinute", mistakesPerMinute.toString())
            it.putExtra("exerciseName", exerciseName)
            startActivity(it)
            finish()
        }
    }
}