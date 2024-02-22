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
import android.graphics.Camera
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.Gallery
import androidx.core.app.NotificationCompat.GroupAlertBehavior
import androidx.core.content.ContextCompat
import com.google.mediapipe.examples.poselandmarker.fragment.CameraFragment
import com.google.mediapipe.examples.poselandmarker.fragment.GalleryFragment
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Timer
import java.util.TimerTask
import kotlin.math.max
import kotlin.math.min


interface OverlayViewListener {
    fun onOverlayViewPause()
    fun onOverlayViewPlay()
}

class OverlayView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {

    private var results: PoseLandmarkerResult? = null
    private var customResults: CustomPoseLandmarkerResult? = null
    private var pointPaint = Paint()
    private var linePaint = Paint()

    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1

    private var jointPairsList = listOf<Pair<Int, Int>>()
    private var jointAngleList = listOf<Float>()

    private var isRed = false
    private var redTimer: Timer? = null

    private var overlayViewListener: OverlayViewListener? = null

    init {
        initPaints()
    }

    fun clear() {
        results = null
        customResults = null
        pointPaint.reset()
        linePaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        val blueColor = ContextCompat.getColor(context!!, R.color.mp_color_primary)
        val yellowColor = ContextCompat.getColor(context!!, R.color.mp_color_secondary)
        val redColor = ContextCompat.getColor(context!!, R.color.mp_color_error)

        linePaint.color =
            blueColor
        linePaint.strokeWidth = LANDMARK_STROKE_WIDTH
        linePaint.style = Paint.Style.STROKE

        pointPaint.color = Color.YELLOW
        pointPaint.strokeWidth = LANDMARK_STROKE_WIDTH
        pointPaint.style = Paint.Style.FILL
    }

    fun setOverlayViewListener(listener: OverlayViewListener) {
        overlayViewListener = listener
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        val blueColor = ContextCompat.getColor(context!!, R.color.mp_color_primary)
        val yellowColor = ContextCompat.getColor(context!!, R.color.mp_color_secondary)
        val redColor = ContextCompat.getColor(context!!, R.color.mp_color_error)

        customResults?.let { poseLandmarkerResult ->
            for(landmark in poseLandmarkerResult.landmarks()) {

                for(normalizedLandmark in landmark) {
                    canvas.drawPoint(
                        normalizedLandmark.x() * imageWidth * scaleFactor,
                        normalizedLandmark.y() * imageHeight * scaleFactor,
                        pointPaint
                    )
                }
                var isAnyJointRed = false

                PoseLandmarker.POSE_LANDMARKS.forEach {

                    val start = it.start()
                    val end = it.end()


                    if (jointPairsList.isNotEmpty())
                    {
//                        Log.i("jointPairsList", jointPairsList.toString())
                        var isFound = false;
                        var index = -1;

                        for (i in jointPairsList.indices) {
                            if ((jointPairsList[i].first == start && jointPairsList[i].second == end)) {
                                isFound = true;
                                index = i
                                break;
                            }
                        }

                        if (isFound && jointAngleList.isNotEmpty())
                        {
                            val angle = jointAngleList[index]
                            if (angle < 30) {
                                linePaint.color = blueColor
                            }
                            else if (angle >= 30 && angle < 60) {
                                linePaint.color = yellowColor
                            }
                            else if (angle >= 60) {
                                //Pause video and play audio
                                linePaint.color = redColor
                                isAnyJointRed = true
                                Log.i("Color", "$start, $end joint color is red")
                            }
                        }
                        else {
                            linePaint.color = blueColor
                        }

                        canvas.drawLine(
                            poseLandmarkerResult.landmarks().get(0).get(it!!.start()).x() * imageWidth * scaleFactor,
                            poseLandmarkerResult.landmarks().get(0).get(it.start()).y() * imageHeight * scaleFactor,
                            poseLandmarkerResult.landmarks().get(0).get(it.end()).x() * imageWidth * scaleFactor,
                            poseLandmarkerResult.landmarks().get(0).get(it.end()).y() * imageHeight * scaleFactor,
                            linePaint)
                    }
                    else
                    {
                        canvas.drawLine(
                            poseLandmarkerResult.landmarks().get(0).get(it!!.start()).x() * imageWidth * scaleFactor,
                            poseLandmarkerResult.landmarks().get(0).get(it.start()).y() * imageHeight * scaleFactor,
                            poseLandmarkerResult.landmarks().get(0).get(it.end()).x() * imageWidth * scaleFactor,
                            poseLandmarkerResult.landmarks().get(0).get(it.end()).y() * imageHeight * scaleFactor,
                            linePaint)
                    }

                }



                if (isAnyJointRed) {
                    //Pause video and play audio
                    Log.i("isAnyJointRed", "Joint is red")
                    overlayViewListener?.onOverlayViewPause()

                } else {
                    Log.i("isAnyJointRed", "Joint is not red")
                    overlayViewListener?.onOverlayViewPlay()
                }
            }
        }
    }


    fun setJointList(jointPairs : List<Pair<Int, Int>>) {
        jointPairsList = jointPairs
    }

    fun setAngleList(angleList : List<Float>) {
        jointAngleList = angleList
        Log.i("angleList", jointAngleList.toString())
    }

    fun setResults(
        poseLandmarkerResults: PoseLandmarkerResult,
        imageHeight: Int,
        imageWidth: Int,
        runningMode: RunningMode = RunningMode.IMAGE
    ) {
        results = poseLandmarkerResults

        this.imageHeight = imageHeight
        this.imageWidth = imageWidth

        scaleFactor = when (runningMode) {
            RunningMode.IMAGE,
            RunningMode.VIDEO -> {
                min(width * 1f / imageWidth, height * 1f / imageHeight)
            }
            RunningMode.LIVE_STREAM -> {
                // PreviewView is in FILL_START mode. So we need to scale up the
                // landmarks to match with the size that the captured images will be
                // displayed.
                max(width * 1f / imageWidth, height * 1f / imageHeight)
            }
        }
        invalidate()
    }

    fun customSetResults(
        poseLandmarkerResults: CustomPoseLandmarkerResult,
        imageHeight: Int,
        imageWidth: Int,
        runningMode: RunningMode = RunningMode.IMAGE
    ) {
        customResults = poseLandmarkerResults

        this.imageHeight = imageHeight
        this.imageWidth = imageWidth

        scaleFactor = when (runningMode) {
            RunningMode.IMAGE,
            RunningMode.VIDEO -> {
                min(width * 1f / imageWidth, height * 1f / imageHeight)
            }
            RunningMode.LIVE_STREAM -> {
                // PreviewView is in FILL_START mode. So we need to scale up the
                // landmarks to match with the size that the captured images will be
                // displayed.
                max(width * 1f / imageWidth, height * 1f / imageHeight)
            }
        }
        invalidate()
    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 12F
    }
}