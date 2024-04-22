package com.google.mediapipe.examples.poselandmarker

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment

class SetupCameraDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction.
            val builder = AlertDialog.Builder(it)
            builder.setTitle("Camera Setup")
            builder.setMessage("Please make sure that your body is visible in the frame to perform the exercise")

                .setPositiveButton("Start") { dialog, id ->

                    dialog.dismiss()

                    // Start the video camera activity
//                    Intent(context, VideoCameraActivity::class.java).also {
//                        it.putExtra("uri", uri)
//                        it.putExtra("dataUri", dataUri)
//                        it.putExtra("pairList", pairList)
//                        it.putExtra("exerciseName", exerciseName)
//                        startActivity(it)
//                    }


                }
                .setNegativeButton("Cancel") { dialog, id ->
                    dialog.dismiss()

                    // User cancelled the dialog.
                }
            // Create the AlertDialog object and return it.
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}


class ItemFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    private fun showCameraSetupDialog() {
//        val dialog = SetupCameraDialogFragment()
//        dialog.show(parentFragmentManager, "Setup Camera Dialog Fragment")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_item, container, false)
        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val tvDescription = view.findViewById<TextView>(R.id.tvDescription)
        val ivExercisePicture = view.findViewById<ImageView>(R.id.ivExercisePicture)

        tvTitle.text = arguments?.getString("exerciseName")
        tvDescription.text = arguments?.getString("exerciseDescription")
        val exerciseName = arguments?.getString("exerciseName").toString()
        val uri = arguments?.getString("uri").toString()
        val dataUri = arguments?.getString("dataUri").toString()
        val pairList = arguments?.getString("pairs").toString()
        val imageResID = arguments?.getString("imageResID")?.toInt()

        if (imageResID != null) {
            ivExercisePicture.setImageResource(imageResID)
        }

        view.setOnClickListener {
            showCameraSetupDialog()

            Log.i("URI", uri)

            Intent(context, VideoCameraActivity::class.java).also {
                it.putExtra("uri", uri)
                it.putExtra("dataUri", dataUri)
                it.putExtra("pairList", pairList)
                it.putExtra("exerciseName", exerciseName)
                startActivity(it)
            }
        }

        return view
    }

    companion object {

    }
}