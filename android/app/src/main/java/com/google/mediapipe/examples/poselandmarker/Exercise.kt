package com.google.mediapipe.examples.poselandmarker

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

data class Exercise(
    val id : Int = 0,
    val name: String,
    val description: String,
    val uri: String,
    val resID: Int,
    val pairs: List<Pair<Int, Int>>,
    val resultBundle: PoseLandmarkerHelper.ResultBundle?)