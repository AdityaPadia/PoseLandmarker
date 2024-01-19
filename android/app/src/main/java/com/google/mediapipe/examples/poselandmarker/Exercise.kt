package com.google.mediapipe.examples.poselandmarker

import android.net.Uri

data class Exercise(val name: String, val description: String, val uri: String, val pairs: List<Pair<Int, Int>>, val resultBundle: PoseLandmarkerHelper.ResultBundle?) {
}
