package com.google.mediapipe.examples.poselandmarker

interface DataTransfer {
    fun transferVideoLandmarkVector(landmarkVector: List<LandmarkVector?>)
    fun transferLivestreamLandmarkVector(landmarkVector: List<LandmarkVector?>)
}