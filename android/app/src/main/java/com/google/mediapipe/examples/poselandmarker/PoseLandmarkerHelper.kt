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
package com.google.mediapipe.examples.poselandmarker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.camera.core.ImageProxy
import com.google.gson.GsonBuilder
import com.google.gson.InstanceCreator
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.components.containers.Landmark
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import org.json.JSONObject
import java.lang.reflect.Type
import java.util.Optional


class PoseLandmarkerHelper(
    var minPoseDetectionConfidence: Float = DEFAULT_POSE_DETECTION_CONFIDENCE,
    var minPoseTrackingConfidence: Float = DEFAULT_POSE_TRACKING_CONFIDENCE,
    var minPosePresenceConfidence: Float = DEFAULT_POSE_PRESENCE_CONFIDENCE,
    // Change the model being used - lite (fastest results)
    var currentModel: Int = MODEL_POSE_LANDMARKER_LITE,
    var currentDelegate: Int = DELEGATE_CPU,
    var runningMode: RunningMode = RunningMode.IMAGE,
    val context: Context,
    // this listener is only used when running in RunningMode.LIVE_STREAM
    val poseLandmarkerHelperListener: LandmarkerListener? = null
) {

    // For this example this needs to be a var so it can be reset on changes.
    // If the Pose Landmarker will not change, a lazy val would be preferable.
    private var poseLandmarker: PoseLandmarker? = null

    init {
        setupPoseLandmarker()
    }

    fun clearPoseLandmarker() {
        poseLandmarker?.close()
        poseLandmarker = null
    }

    // Return running status of PoseLandmarkerHelper
    fun isClose(): Boolean {
        return poseLandmarker == null
    }

    // Initialize the Pose landmarker using current settings on the
    // thread that is using it. CPU can be used with Landmarker
    // that are created on the main thread and used on a background thread, but
    // the GPU delegate needs to be used on the thread that initialized the
    // Landmarker
    fun setupPoseLandmarker() {
        // Set general pose landmarker options
        val baseOptionBuilder = BaseOptions.builder()

        // Use the specified hardware for running the model. Default to CPU
        when (currentDelegate) {
            DELEGATE_CPU -> {
                baseOptionBuilder.setDelegate(Delegate.CPU)
            }
            DELEGATE_GPU -> {
                baseOptionBuilder.setDelegate(Delegate.GPU)
            }
        }

        val modelName =
            when (currentModel) {
                MODEL_POSE_LANDMARKER_FULL -> "pose_landmarker_full.task"
                MODEL_POSE_LANDMARKER_LITE -> "pose_landmarker_lite.task"
                MODEL_POSE_LANDMARKER_HEAVY -> "pose_landmarker_heavy.task"
                else -> "pose_landmarker_full.task"
            }

        baseOptionBuilder.setModelAssetPath(modelName)

        // Check if runningMode is consistent with poseLandmarkerHelperListener
        when (runningMode) {
            RunningMode.LIVE_STREAM -> {
                if (poseLandmarkerHelperListener == null) {
                    throw IllegalStateException(
                        "poseLandmarkerHelperListener must be set when runningMode is LIVE_STREAM."
                    )
                }
            }
            else -> {
                // no-op
            }
        }

        try {
            val baseOptions = baseOptionBuilder.build()
            // Create an option builder with base options and specific
            // options only use for Pose Landmarker.
            val optionsBuilder =
                PoseLandmarker.PoseLandmarkerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setMinPoseDetectionConfidence(minPoseDetectionConfidence)
                    .setMinTrackingConfidence(minPoseTrackingConfidence)
                    .setMinPosePresenceConfidence(minPosePresenceConfidence)
                    .setRunningMode(runningMode)

            // The ResultListener and ErrorListener only use for LIVE_STREAM mode.
            if (runningMode == RunningMode.LIVE_STREAM) {
                optionsBuilder
                    .setResultListener(this::returnLivestreamResult)
                    .setErrorListener(this::returnLivestreamError)
            }

            val options = optionsBuilder.build()
            poseLandmarker =
                PoseLandmarker.createFromOptions(context, options)
        } catch (e: IllegalStateException) {
            poseLandmarkerHelperListener?.onError(
                "Pose Landmarker failed to initialize. See error logs for " +
                        "details"
            )
            Log.e(
                TAG, "MediaPipe failed to load the task with error: " + e
                    .message
            )
        } catch (e: RuntimeException) {
            // This occurs if the model being used does not support GPU
            poseLandmarkerHelperListener?.onError(
                "Pose Landmarker failed to initialize. See error logs for " +
                        "details", GPU_ERROR
            )
            Log.e(
                TAG,
                "Image classifier failed to load model with error: " + e.message
            )
        }
    }

    // Convert the ImageProxy to MP Image and feed it to PoselandmakerHelper.
    fun detectLiveStream(
        imageProxy: ImageProxy,
        isFrontCamera: Boolean
    ) {
        if (runningMode != RunningMode.LIVE_STREAM) {
            throw IllegalArgumentException(
                "Attempting to call detectLiveStream" +
                        " while not using RunningMode.LIVE_STREAM"
            )
        }
        val frameTime = SystemClock.uptimeMillis()

        // Copy out RGB bits from the frame to a bitmap buffer
        val bitmapBuffer =
            Bitmap.createBitmap(
                imageProxy.width,
                imageProxy.height,
                Bitmap.Config.ARGB_8888
            )

        imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
        imageProxy.close()

        val matrix = Matrix().apply {
            // Rotate the frame received from the camera to be in the same direction as it'll be shown
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())

            // flip image if user use front camera
            if (isFrontCamera) {
                postScale(
                    -1f,
                    1f,
                    imageProxy.width.toFloat(),
                    imageProxy.height.toFloat()
                )
            }
        }
        val rotatedBitmap = Bitmap.createBitmap(
            bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
            matrix, true
        )

        // Convert the input Bitmap object to an MPImage object to run inference
        val mpImage = BitmapImageBuilder(rotatedBitmap).build()

        detectAsync(mpImage, frameTime)
    }

    // Run pose landmark using MediaPipe Pose Landmarker API
    @VisibleForTesting
    fun detectAsync(mpImage: MPImage, frameTime: Long) {
        poseLandmarker?.detectAsync(mpImage, frameTime)
        // As we're using running mode LIVE_STREAM, the landmark result will
        // be returned in returnLivestreamResult function
    }

    // Accepts the URI for a video file loaded from the user's gallery and attempts to run
    // pose landmarker inference on the video. This process will evaluate every
    // frame in the video and attach the results to a bundle that will be
    // returned.
    fun detectVideoFile(
        videoUri: Uri,
        inferenceIntervalMs: Long
    ): ResultBundle? {

        Log.d("runDetectionOnVideo", "Step 6")

        if (runningMode != RunningMode.VIDEO) {
            throw IllegalArgumentException(
                "Attempting to call detectVideoFile" +
                        " while not using RunningMode.VIDEO"
            )
        }

        Log.d("runDetectionOnVideo", "Step 7")

        // Inference time is the difference between the system time at the start and finish of the
        // process
        val startTime = SystemClock.uptimeMillis()

        var didErrorOccurred = false

        // Load frames from the video and run the pose landmarker.
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, videoUri)
        val videoLengthMs =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLong()

        // Note: We need to read width/height from frame instead of getting the width/height
        // of the video directly because MediaRetriever returns frames that are smaller than the
        // actual dimension of the video file.
        val firstFrame = retriever.getFrameAtTime(0)
        val width = firstFrame?.width
        val height = firstFrame?.height

        // If the video is invalid, returns a null detection result
        if ((videoLengthMs == null) || (width == null) || (height == null)) return null

        // Next, we'll get one frame every frameInterval ms, then run detection on these frames.
        val resultList = mutableListOf<PoseLandmarkerResult>()
        val customResultList = mutableListOf<CustomPoseLandmarkerResult>()
        val numberOfFrameToRead = videoLengthMs.div(inferenceIntervalMs)

        Log.d("runDetectionOnVideo", "Step 8")

        Log.d("DetectVideo", "Loop to start detection")
        for (i in 0..numberOfFrameToRead) {
            val timestampMs = i * inferenceIntervalMs // ms

            retriever
                .getFrameAtTime(
                    timestampMs * 1000, // convert from ms to micro-s
                    MediaMetadataRetriever.OPTION_CLOSEST
                )
                ?.let { frame ->
                    // Convert the video frame to ARGB_8888 which is required by the MediaPipe
                    val argb8888Frame =
                        if (frame.config == Bitmap.Config.ARGB_8888) frame
                        else frame.copy(Bitmap.Config.ARGB_8888, false)

                    // Convert the input Bitmap object to an MPImage object to run inference
                    val mpImage = BitmapImageBuilder(argb8888Frame).build()

                    // Run pose landmarker using MediaPipe Pose Landmarker API
                    poseLandmarker?.detectForVideo(mpImage, timestampMs)
                        ?.let { detectionResult ->
                            resultList.add(detectionResult)
                        } ?: {
                        didErrorOccurred = true
                        poseLandmarkerHelperListener?.onError(
                            "ResultBundle could not be returned" +
                                    " in detectVideoFile"
                        )
                    }
                }
                ?: run {
                    didErrorOccurred = true
                    poseLandmarkerHelperListener?.onError(
                        "Frame at specified time could not be" +
                                " retrieved when detecting in video."
                    )
                }
        }

        retriever.release()

        val inferenceTimePerFrameMs =
            (SystemClock.uptimeMillis() - startTime).div(numberOfFrameToRead)

        Log.d("Completed Results", "Completed Results")



        if (didErrorOccurred) {
            return null
        } else {
            Log.i("resultBundle", "Finished detection")
            val resultBundle = ResultBundle(resultList, inferenceTimePerFrameMs, height, width)

            Log.i("resultBundle", "Starting conversion")


            val customResultList = ArrayList<CustomPoseLandmarkerResult>()
            for (result in resultList) {

                val customResult = CustomPoseLandmarkerResult.create(
                    result.landmarks(),
                    result.worldLandmarks(),
                    result.segmentationMasks(),
                    result.timestampMs()
                )
                customResultList.add(customResult)
            }

            val customResultBundle = CustomResultBundle(customResultList, inferenceIntervalMs, height, width)

            Log.i("resultBundleCustom", "finished conversion to custom")
            Log.i("resultBundleCustom", customResultBundle.results.toString())

//            val gson = Gson()
//            val jsonString = gson.toJson(resultBundle)
//            Log.i("jsonString", jsonString.toString())
//            Log.i("deserialization", "converting from json string to resultBundle2")
//            val resultBundle2 = gson.fromJson(jsonString, ResultBundle::class.java)

            return resultBundle
        }
    }

    fun customDetectVideoFile(
        videoUri: Uri,
        inferenceIntervalMs: Long
    ): CustomResultBundle? {

        Log.d("runDetectionOnVideo", "Inside customDetectVideoFile")

        if (runningMode != RunningMode.VIDEO) {
            throw IllegalArgumentException(
                "Attempting to call detectVideoFile" +
                        " while not using RunningMode.VIDEO"
            )
        }

        Log.d("runDetectionOnVideo", "Step 7")

        // Inference time is the difference between the system time at the start and finish of the
        // process
        val startTime = SystemClock.uptimeMillis()

        var didErrorOccurred = false

        // Load frames from the video and run the pose landmarker.
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, videoUri)
        val videoLengthMs =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLong()

        // Note: We need to read width/height from frame instead of getting the width/height
        // of the video directly because MediaRetriever returns frames that are smaller than the
        // actual dimension of the video file.
        val firstFrame = retriever.getFrameAtTime(0)
        val width = firstFrame?.width
        val height = firstFrame?.height

        // If the video is invalid, returns a null detection result
        if ((videoLengthMs == null) || (width == null) || (height == null)) return null

        // Next, we'll get one frame every frameInterval ms, then run detection on these frames.
        val resultList = mutableListOf<PoseLandmarkerResult>()
        val customResultList = mutableListOf<CustomPoseLandmarkerResult>()
        val numberOfFrameToRead = videoLengthMs.div(inferenceIntervalMs)

        Log.d("runDetectionOnVideo", "Step 8")

        Log.d("DetectVideo", "Loop to start detection")
        for (i in 0..numberOfFrameToRead) {
            val timestampMs = i * inferenceIntervalMs // ms

            retriever
                .getFrameAtTime(
                    timestampMs * 1000, // convert from ms to micro-s
                    MediaMetadataRetriever.OPTION_CLOSEST
                )
                ?.let { frame ->
                    // Convert the video frame to ARGB_8888 which is required by the MediaPipe
                    val argb8888Frame =
                        if (frame.config == Bitmap.Config.ARGB_8888) frame
                        else frame.copy(Bitmap.Config.ARGB_8888, false)

                    // Convert the input Bitmap object to an MPImage object to run inference
                    val mpImage = BitmapImageBuilder(argb8888Frame).build()

                    // Run pose landmarker using MediaPipe Pose Landmarker API
                    poseLandmarker?.detectForVideo(mpImage, timestampMs)
                        ?.let { detectionResult ->
                            resultList.add(detectionResult)
                        } ?: {
                        didErrorOccurred = true
                        poseLandmarkerHelperListener?.onError(
                            "ResultBundle could not be returned" +
                                    " in detectVideoFile"
                        )
                    }
                }
                ?: run {
                    didErrorOccurred = true
                    poseLandmarkerHelperListener?.onError(
                        "Frame at specified time could not be" +
                                " retrieved when detecting in video."
                    )
                }
        }

        retriever.release()

        val inferenceTimePerFrameMs =
            (SystemClock.uptimeMillis() - startTime).div(numberOfFrameToRead)

        Log.d("Completed Results", "Completed Results")



        if (didErrorOccurred) {
            return null
        } else {
            Log.i("resultBundle", "Finished detection")
            val resultBundle = ResultBundle(resultList, inferenceTimePerFrameMs, height, width)

            Log.i("resultBundle", "Starting conversion")


            val customResultList = ArrayList<CustomPoseLandmarkerResult>()
            for (result in resultList) {

                val customResult = CustomPoseLandmarkerResult.create(
                    result.landmarks(),
                    result.worldLandmarks(),
                    result.segmentationMasks(),
                    result.timestampMs()
                )
                customResultList.add(customResult)
            }

            val customResultBundle = CustomResultBundle(customResultList, inferenceIntervalMs, height, width)
            val customResultBundleString = CustomResultBundle(customResultList, inferenceIntervalMs, height, width).toString()

            val gsonBuilder = GsonBuilder()
            gsonBuilder.registerTypeAdapter(CustomResultBundle::class.java, CustomResultBundleInstanceCreator())

            val gson = gsonBuilder.create()
            val jsonCustomResultBundle = gson.toJson(customResultBundle)

            longLog(jsonCustomResultBundle.toString())

            val customResultBundle2 = extractDataFromJsonString(jsonCustomResultBundle)
            return customResultBundle2
        }
    }

    fun extractDataFromJsonString(jsonCustomResultBundle: String?): CustomResultBundle? {

        val jsonObject = JSONObject(jsonCustomResultBundle)
        val resultsArray = jsonObject.getJSONArray("results")
        val inferenceTime = jsonObject.getLong("inferenceTime")
        val inputImageHeight = jsonObject.getInt("inputImageHeight")
        val inputImageWidth = jsonObject.getInt("inputImageWidth")
        val resultList = mutableListOf<CustomPoseLandmarkerResult>()

        Log.i("extractDataFromString", inferenceTime.toString())
        Log.i("extractDataFromString", inputImageHeight.toString())
        Log.i("extractDataFromString", inputImageWidth.toString())
        Log.i("extractDataFromString", resultsArray.length().toString())

        for (i in 0 until resultsArray.length()) {
            val resultObject = resultsArray.getJSONObject(i)
            Log.i("extractDataFromString", resultObject.toString())

            // Extract the "landmarks" array
            val landmarksArray = resultObject.getJSONArray("landmarks")
            val landmarks: MutableList<List<NormalizedLandmark>> =
                ArrayList<List<NormalizedLandmark>>()

            Log.i("extractDataFromString", landmarksArray.toString())

            // Iterate over the "landmarks" array and convert each inner array
            for (j in 0 until landmarksArray.length()) {
                val innerArray = landmarksArray.getJSONArray(j)
                Log.i("extractDataFromString", innerArray.toString())
                val innerList: MutableList<NormalizedLandmark> = ArrayList<NormalizedLandmark>()

                // Convert each inner array into a list of NormalizedLandmark objects
                for (k in 0 until innerArray.length()) {
                    val landmarkObject = innerArray.getJSONObject(k)
                    Log.i("extractDataFromString", landmarkObject.toString())
                    val x = landmarkObject.getDouble("x")
                    val y = landmarkObject.getDouble("y")
                    val z = landmarkObject.getDouble("z")
                    val landmark = NormalizedLandmark.create(x.toFloat(), y.toFloat(), z.toFloat())
                    innerList.add(landmark)
                }
                landmarks.add(innerList)
            }

            // Extract the "worldLandmarks" array
            val worldLandmarksArray = resultObject.getJSONArray("worldLandmarks")
            val worldLandmarks: MutableList<List<Landmark>> = ArrayList()
            Log.i("extractDataFromString", worldLandmarksArray.toString())

            // Iterate over the "worldLandmarks" array and convert each inner array
            for (j in 0 until worldLandmarksArray.length()) {
                val innerArray = worldLandmarksArray.getJSONArray(j)
                Log.i("extractDataFromString", innerArray.toString())
                val innerList: MutableList<Landmark> = ArrayList()

                // Convert each inner array into a list of Landmark objects
                for (k in 0 until innerArray.length()) {
                    val landmarkObject = innerArray.getJSONObject(k)
                    Log.i("extractDataFromString", landmarkObject.toString())
                    val x = landmarkObject.getDouble("x")
                    val y = landmarkObject.getDouble("y")
                    val z = landmarkObject.getDouble("z")
                    val landmark = Landmark.create(x.toFloat(), y.toFloat(), z.toFloat())
                    innerList.add(landmark)
                }
                worldLandmarks.add(innerList)
            }

            // Extract the "segmentationMasks" array if present
            var segmentationMasksData: Optional<List<MPImage?>> = Optional.empty()
//                if (resultObject.has("segmentationMasks")) {
//                val segmentationMasksArray = resultObject.getJSONObject("segmentationMasks")
//                val segmentationMasks: List<MPImage> = ArrayList()
//
//                // Convert each segmentation mask object into an MPImage object
//                for (j in 0 until segmentationMasksArray.length()) {
//                    val maskObject = segmentationMasksArray.getJSONObject(j)
//                    // Parse the MPImage properties and create an MPImage object
//                    // Add the MPImage object to the segmentationMasks list
//                }
//                Optional.of(segmentationMasks)
//            } else {
//                Optional.empty()
//            }

            Log.i("extractDataFromString", segmentationMasksData.toString())

            // Extract the "timestampMs" value
            val timestampMs = resultObject.getLong("timestampMs")
            Log.i("extractDataFromString", timestampMs.toString())

            // Create a new CustomPoseLandmarkerResult object using the extracted values
            val result = CustomPoseLandmarkerResult.create(
                landmarks,
                worldLandmarks,
                segmentationMasksData,
                timestampMs
            )

            // Add the CustomPoseLandmarkerResult object to the resultList
            resultList.add(result)
        }

        Log.i("extractDataFromString", CustomResultBundle(
            resultList,
            inferenceTime,
            inputImageHeight,
            inputImageWidth,
        ).toString()
        )

        return CustomResultBundle(
            resultList,
            inferenceTime,
            inputImageHeight,
            inputImageWidth,
        )

    }

    // Accepted a Bitmap and runs pose landmarker inference on it to return
    // results back to the caller
    fun detectImage(image: Bitmap): ResultBundle? {
        if (runningMode != RunningMode.IMAGE) {
            throw IllegalArgumentException(
                "Attempting to call detectImage" +
                        " while not using RunningMode.IMAGE"
            )
        }


        // Inference time is the difference between the system time at the
        // start and finish of the process
        val startTime = SystemClock.uptimeMillis()

        // Convert the input Bitmap object to an MPImage object to run inference
        val mpImage = BitmapImageBuilder(image).build()

        // Run pose landmarker using MediaPipe Pose Landmarker API
        poseLandmarker?.detect(mpImage)?.also { landmarkResult ->
            val inferenceTimeMs = SystemClock.uptimeMillis() - startTime
            return ResultBundle(
                listOf(landmarkResult),
                inferenceTimeMs,
                image.height,
                image.width
            )
        }

        // If poseLandmarker?.detect() returns null, this is likely an error. Returning null
        // to indicate this.
        poseLandmarkerHelperListener?.onError(
            "Pose Landmarker failed to detect."
        )
        return null
    }

    private fun returnLivestreamVector(
        landmarkIndex1: Int,
        landmarkIndex2: Int,
        landmarkList: List<Landmark>
    ): LandmarkVector {
        val landmark1 = landmarkList[landmarkIndex1]
        val landmark2 = landmarkList[landmarkIndex2]

        return LandmarkVector(
            landmark1.x() - landmark2.x(),
            landmark1.y() - landmark2.y(),
            landmark1.z() - landmark2.z()
        )
    }

    // Return the landmark result to this PoseLandmarkerHelper's caller
    private fun returnLivestreamResult(
        result: PoseLandmarkerResult,
        input: MPImage
    ) {
        val finishTimeMs = SystemClock.uptimeMillis()
        val inferenceTime = finishTimeMs - result.timestampMs()

        // Display Results from Livestream
        if (result.worldLandmarks().size != 0) {
            val livestreamVector = returnLivestreamVector(13, 15, result.worldLandmarks()[0])
        }


        poseLandmarkerHelperListener?.onResults(
            ResultBundle(
                listOf(result),
                inferenceTime,
                input.height,
                input.width
            )
        )
    }

    // Return errors thrown during detection to this PoseLandmarkerHelper's
    // caller
    private fun returnLivestreamError(error: RuntimeException) {
        poseLandmarkerHelperListener?.onError(
            error.message ?: "An unknown error has occurred"
        )
    }

    // Logging function to print out strings > 1000 characters long
    private fun longLog(str: String) {
        if (str.length > 1000) {
            Log.d("jsonCustomResultBundle", str.substring(0, 1000))
            longLog(str.substring(1000))
        } else {
            Log.d("jsonCustomResultBundle", str)
        }
    }

    companion object {
        const val TAG = "PoseLandmarkerHelper"

        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val DEFAULT_POSE_DETECTION_CONFIDENCE = 0.7F
        const val DEFAULT_POSE_TRACKING_CONFIDENCE = 0.6F
        const val DEFAULT_POSE_PRESENCE_CONFIDENCE = 0.6F
        const val DEFAULT_NUM_POSES = 1
        const val OTHER_ERROR = 0
        const val GPU_ERROR = 1
        const val MODEL_POSE_LANDMARKER_FULL = 0
        const val MODEL_POSE_LANDMARKER_LITE = 1
        const val MODEL_POSE_LANDMARKER_HEAVY = 2
    }

    data class ResultBundle(
        val results: List<PoseLandmarkerResult>,
        val inferenceTime: Long,
        val inputImageHeight: Int,
        val inputImageWidth: Int,
    )

    //Custom Data Class to store
    data class CustomResultBundle(
        val results: List<CustomPoseLandmarkerResult>,
        val inferenceTime: Long,
        val inputImageHeight: Int,
        val inputImageWidth: Int
    )

    class CustomResultBundleInstanceCreator : InstanceCreator<CustomResultBundle> {
        override fun createInstance(type: Type): CustomResultBundle {
            return CustomResultBundle(
                listOf(),
                0L,
                0,
                0
            )
        }
    }

    interface LandmarkerListener {
        fun onError(error: String, errorCode: Int = OTHER_ERROR)
        fun onResults(resultBundle: ResultBundle)
    }
}