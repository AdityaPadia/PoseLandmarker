/*
 * Copyright 2023 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.mediapipe.examples.poselandmarker.fragment

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.mediapipe.examples.poselandmarker.DataTransfer
import com.google.mediapipe.examples.poselandmarker.LandmarkVector
import com.google.mediapipe.examples.poselandmarker.MainViewModel
import com.google.mediapipe.examples.poselandmarker.PoseLandmarkerHelper
import com.google.mediapipe.examples.poselandmarker.R
import com.google.mediapipe.examples.poselandmarker.databinding.FragmentGalleryBinding
import com.google.mediapipe.tasks.components.containers.Landmark
import com.google.mediapipe.tasks.vision.core.RunningMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class GalleryFragment : Fragment(), PoseLandmarkerHelper.LandmarkerListener {

    enum class MediaType {
        IMAGE,
        VIDEO,
        UNKNOWN
    }

    private var _fragmentGalleryBinding: FragmentGalleryBinding? = null
    private val fragmentGalleryBinding
        get() = _fragmentGalleryBinding!!
    private lateinit var poseLandmarkerHelper: PoseLandmarkerHelper
    private val viewModel: MainViewModel by activityViewModels()
    private var jointPairsList = listOf<Pair<Int, Int>>()
    private var mediaUri = ""
    private var detectionComplete = false
    private var isVideoPaused = false
    private var isVideoFinished = false
    private var hasVideoBeenPaused = false
    private var isAudioPlaying = true
    private var systemClockTime:Long = 0
    private var videoStartTimeMs:Long = 0
    private var videoElapsedTimeMs: Long = 0
    private var videoPauseStartTimeMs: Long = 0
    private var totalPausedDurationMs: Long = 0
    private var resultIndex: Int = 0
    private var lastResultIndex: Int = 0
    private var mediaPlayer: MediaPlayer = MediaPlayer()


    /** Blocking ML operations are performed using this executor */
    private lateinit var backgroundExecutor: ScheduledExecutorService

    private val getContent =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            // Handle the returned Uri
            uri?.let { mediaUri ->
                Log.i("mediaUrigetContent", mediaUri.toString())
                when (val mediaType = loadMediaType(mediaUri)) {
                    MediaType.IMAGE -> runDetectionOnImage(mediaUri)
                    MediaType.VIDEO -> customRunDetectionOnVideo(mediaUri)
                    MediaType.UNKNOWN -> {
                        updateDisplayView(mediaType)
                        Toast.makeText(
                            requireContext(),
                            "Unsupported data type.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentGalleryBinding =
            FragmentGalleryBinding.inflate(inflater, container, false)


        return fragmentGalleryBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        mediaUri = arguments?.getString("uri").toString()

        super.onViewCreated(view, savedInstanceState)

        initBottomSheetControls()

        //TODO : Add run detection on video here
//        fragmentGalleryBinding.fabGetContent.visibility = View.GONE
        Log.i("mediaUri", Uri.parse(mediaUri).toString())
        Log.i("mediaUriType", loadMediaType(Uri.parse(mediaUri)).toString())



        when (val mediaType = loadMediaType(Uri.parse(mediaUri))) {
            MediaType.IMAGE -> runDetectionOnImage(Uri.parse(mediaUri))
            MediaType.VIDEO -> runDetectionOnVideo(Uri.parse(mediaUri))
            MediaType.UNKNOWN -> {
                updateDisplayView(mediaType)
                Toast.makeText(
                    requireContext(),
                    "Unsupported data type.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


        fragmentGalleryBinding.fabGetContent.setOnClickListener {
            getContent.launch(arrayOf("image/*", "video/*"))
        }


//        runDetectionOnVideo(Uri.parse("content://com.android.providers.media.documents/document/video%3A290079"))
    }

    override fun onPause() {
        fragmentGalleryBinding.overlay.clear()
        if (fragmentGalleryBinding.videoView.isPlaying) {
            fragmentGalleryBinding.videoView.stopPlayback()
        }
        fragmentGalleryBinding.videoView.visibility = View.GONE
        super.onPause()
    }

    private fun initBottomSheetControls() {
        // init bottom sheet settings
        fragmentGalleryBinding.bottomSheetLayout.detectionThresholdValue.text =
            String.format(
                Locale.US, "%.2f", viewModel.currentMinPoseDetectionConfidence
            )
        fragmentGalleryBinding.bottomSheetLayout.trackingThresholdValue.text =
            String.format(
                Locale.US, "%.2f", viewModel.currentMinPoseTrackingConfidence
            )
        fragmentGalleryBinding.bottomSheetLayout.presenceThresholdValue.text =
            String.format(
                Locale.US, "%.2f", viewModel.currentMinPosePresenceConfidence
            )

        // When clicked, lower detection score threshold floor
        fragmentGalleryBinding.bottomSheetLayout.detectionThresholdMinus.setOnClickListener {
            if (viewModel.currentMinPoseDetectionConfidence >= 0.2) {
                viewModel.setMinPoseDetectionConfidence(viewModel.currentMinPoseDetectionConfidence - 0.1f)
                updateControlsUi()
            }
        }

        // When clicked, raise detection score threshold floor
        fragmentGalleryBinding.bottomSheetLayout.detectionThresholdPlus.setOnClickListener {
            if (viewModel.currentMinPoseDetectionConfidence <= 0.8) {
                viewModel.setMinPoseDetectionConfidence(viewModel.currentMinPoseDetectionConfidence + 0.1f)
                updateControlsUi()
            }
        }

        // When clicked, lower pose tracking score threshold floor
        fragmentGalleryBinding.bottomSheetLayout.trackingThresholdMinus.setOnClickListener {
            if (viewModel.currentMinPoseTrackingConfidence >= 0.2) {
                viewModel.setMinPoseTrackingConfidence(
                    viewModel.currentMinPoseTrackingConfidence - 0.1f
                )
                updateControlsUi()
            }
        }

        // When clicked, raise pose tracking score threshold floor
        fragmentGalleryBinding.bottomSheetLayout.trackingThresholdPlus.setOnClickListener {
            if (viewModel.currentMinPoseTrackingConfidence <= 0.8) {
                viewModel.setMinPoseTrackingConfidence(
                    viewModel.currentMinPoseTrackingConfidence + 0.1f
                )
                updateControlsUi()
            }
        }

        // When clicked, lower pose presence score threshold floor
        fragmentGalleryBinding.bottomSheetLayout.presenceThresholdMinus.setOnClickListener {
            if (viewModel.currentMinPosePresenceConfidence >= 0.2) {
                viewModel.setMinPosePresenceConfidence(
                    viewModel.currentMinPosePresenceConfidence - 0.1f
                )
                updateControlsUi()
            }
        }

        // When clicked, raise pose presence score threshold floor
        fragmentGalleryBinding.bottomSheetLayout.presenceThresholdPlus.setOnClickListener {
            if (viewModel.currentMinPosePresenceConfidence <= 0.8) {
                viewModel.setMinPosePresenceConfidence(
                    viewModel.currentMinPosePresenceConfidence + 0.1f
                )
                updateControlsUi()
            }
        }

        // When clicked, change the underlying hardware used for inference. Current options are CPU
        // GPU, and NNAPI
        fragmentGalleryBinding.bottomSheetLayout.spinnerDelegate.setSelection(
            viewModel.currentDelegate,
            false
        )
        fragmentGalleryBinding.bottomSheetLayout.spinnerDelegate.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    p0: AdapterView<*>?,
                    p1: View?,
                    p2: Int,
                    p3: Long
                ) {

                    viewModel.setDelegate(p2)
                    updateControlsUi()
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /* no op */
                }
            }

        // When clicked, change the underlying model used for object detection
        fragmentGalleryBinding.bottomSheetLayout.spinnerModel.setSelection(
            viewModel.currentModel,
            false
        )
        fragmentGalleryBinding.bottomSheetLayout.spinnerModel.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    p0: AdapterView<*>?,
                    p1: View?,
                    p2: Int,
                    p3: Long
                ) {
                    poseLandmarkerHelper.currentModel = p2
                    updateControlsUi()
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /* no op */
                }
            }
    }

    // Update the values displayed in the bottom sheet. Reset detector.
    private fun updateControlsUi() {
        if (fragmentGalleryBinding.videoView.isPlaying) {
            fragmentGalleryBinding.videoView.stopPlayback()
        }
        fragmentGalleryBinding.videoView.visibility = View.GONE
        fragmentGalleryBinding.imageResult.visibility = View.GONE
        fragmentGalleryBinding.overlay.clear()
        fragmentGalleryBinding.bottomSheetLayout.detectionThresholdValue.text =
            String.format(
                Locale.US, "%.2f", viewModel.currentMinPoseDetectionConfidence
            )
        fragmentGalleryBinding.bottomSheetLayout.trackingThresholdValue.text =
            String.format(
                Locale.US, "%.2f", viewModel.currentMinPoseTrackingConfidence
            )
        fragmentGalleryBinding.bottomSheetLayout.presenceThresholdValue.text =
            String.format(
                Locale.US, "%.2f", viewModel.currentMinPosePresenceConfidence
            )

        fragmentGalleryBinding.overlay.clear()
        fragmentGalleryBinding.tvPlaceholder.visibility = View.VISIBLE
    }

    // Load and display the image.
    private fun runDetectionOnImage(uri: Uri) {
        setUiEnabled(false)
        backgroundExecutor = Executors.newSingleThreadScheduledExecutor()
        updateDisplayView(MediaType.IMAGE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(
                requireActivity().contentResolver,
                uri
            )
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(
                requireActivity().contentResolver,
                uri
            )
        }
            .copy(Bitmap.Config.ARGB_8888, true)
            ?.let { bitmap ->
                fragmentGalleryBinding.imageResult.setImageBitmap(bitmap)

                // Run pose landmarker on the input image
                backgroundExecutor.execute {

                    poseLandmarkerHelper =
                        PoseLandmarkerHelper(
                            context = requireContext(),
                            runningMode = RunningMode.IMAGE,
                            minPoseDetectionConfidence = viewModel.currentMinPoseDetectionConfidence,
                            minPoseTrackingConfidence = viewModel.currentMinPoseTrackingConfidence,
                            minPosePresenceConfidence = viewModel.currentMinPosePresenceConfidence,
                            currentDelegate = viewModel.currentDelegate
                        )

                    poseLandmarkerHelper.detectImage(bitmap)?.let { result ->
                        activity?.runOnUiThread {
                            fragmentGalleryBinding.overlay.setResults(
                                result.results[0],
                                bitmap.height,
                                bitmap.width,
                                RunningMode.IMAGE
                            )

                            setUiEnabled(true)
                            fragmentGalleryBinding.bottomSheetLayout.inferenceTimeVal.text =
                                String.format("%d ms", result.inferenceTime)
                        }
                    } ?: run { Log.e(TAG, "Error running pose landmarker.") }

                    poseLandmarkerHelper.clearPoseLandmarker()
                }
            }
    }

//    fun isVideoFinished() : Boolean {
//        fragmentGalleryBinding.videoView.setOnCompletionListener {
//            isVideoFinished = true
//        }
//        return isVideoFinished
//    }

    fun isVideoPlaying() : Boolean {
        return (fragmentGalleryBinding.videoView.isPlaying && detectionComplete)
    }

    fun pauseVideo() {
        if (fragmentGalleryBinding.videoView.isPlaying) {
            isVideoPaused = true
            videoPauseStartTimeMs = SystemClock.uptimeMillis()
            Log.i("videoPauseStartTimeMs", videoPauseStartTimeMs.toString())
            fragmentGalleryBinding.videoView.pause()
//            videoElapsedTimeMs = SystemClock.uptimeMillis() - videoStartTimeMs
//            lastResultIndex = resultIndex
            Toast.makeText(
                requireContext(),
                "Posture out of sync.",
                Toast.LENGTH_SHORT
            ).show()
        }
        val afd = resources.openRawResourceFd(R.raw.postureoutofsync)
        mediaPlayer.reset()
        mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        mediaPlayer.prepare()
        mediaPlayer.start()
        afd.close()

        val resid = R.raw.postureoutofsync
        val mediaPlayer = MediaPlayer.create(activity, resid)
        isAudioPlaying = true
        mediaPlayer?.start()
    }

    fun playVideo() {
        if (isVideoPaused) {
            mediaPlayer.setOnCompletionListener {
                totalPausedDurationMs += SystemClock.uptimeMillis() - videoPauseStartTimeMs
                videoStartTimeMs = SystemClock.uptimeMillis() - videoElapsedTimeMs
                fragmentGalleryBinding.videoView.start()
                isVideoPaused = false
            }
        }
//        isVideoPaused = false

    }

    private fun runDetectionOnVideo(uri: Uri) {
        Log.d("runDetectionOnVideo", "Entered the function")
        setUiEnabled(false)
        updateDisplayView(MediaType.VIDEO)

        Log.d("runDetectionOnVideo", "Step 2")
        Log.d("mediaUri2", Uri.parse(mediaUri).toString())

        with(fragmentGalleryBinding.videoView) {
            setVideoURI(Uri.parse(mediaUri))
            // mute the audio
//            setOnPreparedListener { it.setVolume(0f, 0f) }
            requestFocus()
        }

        Log.d("runDetectionOnVideo", "Step 3")

        backgroundExecutor = Executors.newSingleThreadScheduledExecutor()

        Log.d("runDetectionOnVideo", "Step 4")
        backgroundExecutor.execute {

            poseLandmarkerHelper =
                PoseLandmarkerHelper(
                    context = requireContext(),
                    runningMode = RunningMode.VIDEO,
                    minPoseDetectionConfidence = viewModel.currentMinPoseDetectionConfidence,
                    minPoseTrackingConfidence = viewModel.currentMinPoseTrackingConfidence,
                    minPosePresenceConfidence = viewModel.currentMinPosePresenceConfidence,
                    currentDelegate = viewModel.currentDelegate
                )

            activity?.runOnUiThread {
                fragmentGalleryBinding.videoView.visibility = View.GONE
                fragmentGalleryBinding.progress.visibility = View.VISIBLE
            }

            Log.d("runDetectionOnVideo", "Step 5")

            //TODO : load resultBundle and display results
            poseLandmarkerHelper.detectVideoFile(Uri.parse(mediaUri), VIDEO_INTERVAL_MS)
                ?.let { resultBundle ->
                    detectionComplete = true
                    activity?.runOnUiThread { displayVideoResult(resultBundle) }
                }
                ?: run { Log.e(TAG, "Error running pose landmarker.") }

            poseLandmarkerHelper.clearPoseLandmarker()
        }
    }

    private fun customRunDetectionOnVideo(uri: Uri) {
        Log.d("runDetectionOnVideo", "Entered the function")
        setUiEnabled(false)
        updateDisplayView(MediaType.VIDEO)

        Log.d("runDetectionOnVideo", "Step 2")
        Log.d("mediaUri2", Uri.parse(mediaUri).toString())

        with(fragmentGalleryBinding.videoView) {
            setVideoURI(Uri.parse(mediaUri))
            // mute the audio
//            setOnPreparedListener { it.setVolume(0f, 0f) }
            requestFocus()
        }

        Log.d("runDetectionOnVideo", "Step 3")

        backgroundExecutor = Executors.newSingleThreadScheduledExecutor()

        Log.d("runDetectionOnVideo", "Step 4")
        backgroundExecutor.execute {

            poseLandmarkerHelper =
                PoseLandmarkerHelper(
                    context = requireContext(),
                    runningMode = RunningMode.VIDEO,
                    minPoseDetectionConfidence = viewModel.currentMinPoseDetectionConfidence,
                    minPoseTrackingConfidence = viewModel.currentMinPoseTrackingConfidence,
                    minPosePresenceConfidence = viewModel.currentMinPosePresenceConfidence,
                    currentDelegate = viewModel.currentDelegate
                )

            activity?.runOnUiThread {
                fragmentGalleryBinding.videoView.visibility = View.GONE
                fragmentGalleryBinding.progress.visibility = View.VISIBLE
            }

            Log.d("runDetectionOnVideo", "Step 5")

            //TODO : load resultBundle and display results
            poseLandmarkerHelper.customDetectVideoFile(Uri.parse(mediaUri), VIDEO_INTERVAL_MS)
                ?.let { customResultBundle ->
                    detectionComplete = true
                    activity?.runOnUiThread { customDisplayVideoResult(customResultBundle) }
                }
                ?: run { Log.e(TAG, "Error running pose landmarker.") }

            poseLandmarkerHelper.clearPoseLandmarker()
        }
    }

    fun setJointList(jointPairs : List<Pair<Int, Int>>) {
        jointPairsList = jointPairs
        Log.i("jointList", jointPairsList.toString())
    }

    //Function that returns the angle of a joint during video
     private fun returnVideoVector(
        landmarkIndex1: Int,
        landmarkIndex2: Int,
        landmarkList: List<Landmark>
    ): LandmarkVector {
//        Log.i("Landmark List", landmarkList.toString())

        val landmark1 = landmarkList[landmarkIndex1]
        val landmark2 = landmarkList[landmarkIndex2]

        return LandmarkVector(
            landmark1.x() - landmark2.x(),
            landmark1.y() - landmark2.y(),
            landmark1.z() - landmark2.z()
        )

    }

    // Setup and display the video.
    private fun displayVideoResult(result: PoseLandmarkerHelper.ResultBundle) {

        fragmentGalleryBinding.videoView.visibility = View.VISIBLE
        fragmentGalleryBinding.progress.visibility = View.GONE

        fragmentGalleryBinding.videoView.start()
        var videoStartTimeMs = SystemClock.uptimeMillis()
        var videoPausedTimeMs: Long = 0
        var videoElapsedTimeMs: Long = 0
        backgroundExecutor.scheduleAtFixedRate(
            {
                activity?.runOnUiThread {

                    Log.i("isVideoPausedInLoop", isVideoPaused.toString())

                    if (!isVideoPaused) {
                        videoElapsedTimeMs = SystemClock.uptimeMillis() - videoStartTimeMs - totalPausedDurationMs
                        Log.i("videoElapsedTimeMs", videoElapsedTimeMs.toString())
                    } else {

                    }

                    val resultIndex =
                        videoElapsedTimeMs.div(VIDEO_INTERVAL_MS).toInt()

                    if (resultIndex >= result.results.size || fragmentGalleryBinding.videoView.visibility == View.GONE) {
                    // The video playback has finished so we stop drawing bounding boxes
                    Log.i("resultIndex", "shutdown")
                    Log.i("resultIndexSize", result.results.size.toString())
                    backgroundExecutor.shutdown()
                } else {

                    val dataTransferInterface : DataTransfer = activity as DataTransfer
                    if (result.results[resultIndex].worldLandmarks()[0].isNotEmpty())
                    {
                        val videoVectorList = mutableListOf<LandmarkVector>()

                        for (pair in jointPairsList) {
                            val landmark1 = pair.first
                            val landmark2 = pair.second
                            val videoVector = returnVideoVector(landmark1,landmark2, result.results[resultIndex].worldLandmarks()[0])
                            videoVectorList.add(videoVector)
                        }
                        dataTransferInterface.transferVideoLandmarkVector(videoVectorList)
                    }

                    fragmentGalleryBinding.overlay.setResults(
                        result.results[resultIndex],
                        result.inputImageHeight,
                        result.inputImageWidth,
                        RunningMode.VIDEO
                    )

                    setUiEnabled(true)

                    fragmentGalleryBinding.bottomSheetLayout.inferenceTimeVal.text =
                        String.format("%d ms", result.inferenceTime)
                }
                }
            },
            0,
            VIDEO_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        )

        Log.i("isVideoPaused", "Out of Loop")

        isVideoFinished = true
    }

    private fun customDisplayVideoResult(result: PoseLandmarkerHelper.CustomResultBundle) {

        fragmentGalleryBinding.videoView.visibility = View.VISIBLE
        fragmentGalleryBinding.progress.visibility = View.GONE

        fragmentGalleryBinding.videoView.start()
        var videoStartTimeMs = SystemClock.uptimeMillis()
        var videoPausedTimeMs: Long = 0
        var videoElapsedTimeMs: Long = 0
        backgroundExecutor.scheduleAtFixedRate(
            {
                activity?.runOnUiThread {

                    Log.i("isVideoPausedInLoop", isVideoPaused.toString())

                    if (!isVideoPaused) {
                        videoElapsedTimeMs = SystemClock.uptimeMillis() - videoStartTimeMs - totalPausedDurationMs
                        Log.i("videoElapsedTimeMs", videoElapsedTimeMs.toString())
                    } else {

                    }

                    val resultIndex =
                        videoElapsedTimeMs.div(VIDEO_INTERVAL_MS).toInt()

                    if (resultIndex >= result.results.size || fragmentGalleryBinding.videoView.visibility == View.GONE) {
                        // The video playback has finished so we stop drawing bounding boxes
                        Log.i("resultIndex", "shutdown")
                        Log.i("resultIndexSize", result.results.size.toString())
                        backgroundExecutor.shutdown()
                    } else {

                        val dataTransferInterface : DataTransfer = activity as DataTransfer
                        if (result.results[resultIndex].worldLandmarks()[0].isNotEmpty())
                        {
                            val videoVectorList = mutableListOf<LandmarkVector>()

                            for (pair in jointPairsList) {
                                val landmark1 = pair.first
                                val landmark2 = pair.second
                                val videoVector = returnVideoVector(landmark1,landmark2, result.results[resultIndex].worldLandmarks()[0])
                                videoVectorList.add(videoVector)
                            }
                            dataTransferInterface.transferVideoLandmarkVector(videoVectorList)
                        }

                        fragmentGalleryBinding.overlay.customSetResults(
                            result.results[resultIndex],
                            result.inputImageHeight,
                            result.inputImageWidth,
                            RunningMode.VIDEO
                        )

                        setUiEnabled(true)

                        fragmentGalleryBinding.bottomSheetLayout.inferenceTimeVal.text =
                            String.format("%d ms", result.inferenceTime)
                    }
                }
            },
            0,
            VIDEO_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        )

        Log.i("isVideoPaused", "Out of Loop")

        isVideoFinished = true
    }

    private fun updateDisplayView(mediaType: MediaType) {
        fragmentGalleryBinding.imageResult.visibility =
            if (mediaType == MediaType.IMAGE) View.VISIBLE else View.GONE
        fragmentGalleryBinding.videoView.visibility =
            if (mediaType == MediaType.VIDEO) View.VISIBLE else View.GONE
        fragmentGalleryBinding.tvPlaceholder.visibility =
            if (mediaType == MediaType.UNKNOWN) View.VISIBLE else View.GONE
    }

    // Check the type of media that user selected.
    private fun loadMediaType(uri: Uri): MediaType {
        val mimeType = context?.contentResolver?.getType(uri)
        mimeType?.let {
            if (mimeType.startsWith("image")) return MediaType.IMAGE
            if (mimeType.startsWith("video")) return MediaType.VIDEO
        }

        return MediaType.UNKNOWN
    }

    private fun setUiEnabled(enabled: Boolean) {
        fragmentGalleryBinding.fabGetContent.isEnabled = enabled
        fragmentGalleryBinding.bottomSheetLayout.detectionThresholdMinus.isEnabled =
            enabled
        fragmentGalleryBinding.bottomSheetLayout.detectionThresholdPlus.isEnabled =
            enabled
        fragmentGalleryBinding.bottomSheetLayout.trackingThresholdMinus.isEnabled =
            enabled
        fragmentGalleryBinding.bottomSheetLayout.trackingThresholdPlus.isEnabled =
            enabled
        fragmentGalleryBinding.bottomSheetLayout.presenceThresholdMinus.isEnabled =
            enabled
        fragmentGalleryBinding.bottomSheetLayout.presenceThresholdPlus.isEnabled =
            enabled
        fragmentGalleryBinding.bottomSheetLayout.spinnerDelegate.isEnabled =
            enabled
    }

    private fun classifyingError() {
        activity?.runOnUiThread {
            fragmentGalleryBinding.progress.visibility = View.GONE
            setUiEnabled(true)
            updateDisplayView(MediaType.UNKNOWN)
        }
    }

    override fun onError(error: String, errorCode: Int) {
        classifyingError()
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            if (errorCode == PoseLandmarkerHelper.GPU_ERROR) {
                fragmentGalleryBinding.bottomSheetLayout.spinnerDelegate.setSelection(
                    PoseLandmarkerHelper.DELEGATE_CPU,
                    false
                )
            }
        }
    }

    override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
        // no-op

    }

    companion object {
        private const val TAG = "GalleryFragment"

        // Value used to get frames at specific intervals for inference (e.g. every 300ms)
        private const val VIDEO_INTERVAL_MS = 300L
    }
}
