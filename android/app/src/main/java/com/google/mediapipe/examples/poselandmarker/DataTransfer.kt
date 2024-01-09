package com.google.mediapipe.examples.poselandmarker

interface DataTransfer {
    fun transferVideoLandmarkVector(landmarkVector: LandmarkVector?)
    fun transferLivestreamLandmarkVector(landmarkVector: LandmarkVector?)
}