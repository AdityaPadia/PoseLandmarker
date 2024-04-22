package com.google.mediapipe.examples.poselandmarker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        if (currentUser != null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        //Authenticate with Email and Password
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val registerButton = findViewById<Button>(R.id.btnRegister)
        val signInButton = findViewById<Button>(R.id.btnSignIn)

        //Create user with email and password
        registerButton.setOnClickListener {
            val email = etEmail.text.toString().trim()
            createUserWithEmailAndPassword(email, etPassword.toString())
        }

        //Sign in an user with email and password
        signInButton.setOnClickListener {
            val email = etEmail.text.toString().trim()
            signInUserWithEmailAndPassword(email, etPassword.toString())
        }



        //Authenticate with Google
        val googleSignInButton = findViewById<SignInButton>(R.id.googleSignInButton)
        googleSignInButton.setSize(SignInButton.SIZE_WIDE)
        googleSignInButton.setOnClickListener {
            googleSignIn()
        }

    }

    private fun createUserWithEmailAndPassword(email: String, password: String) {
        Log.i("createUserWithEmailAndPassword", email.toString())
        Log.i("createUserWithEmailAndPassword", password.toString())
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("createUserWithEmailAndPassword", "createUserWithEmail:success")
                    val user = auth.currentUser
                    if (user != null) {
                        updateUI(user)
                    }
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("createUserWithEmailAndPassword", "createUserWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Authentication failed.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
    }

    private fun signInUserWithEmailAndPassword(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("signInUserWithEmailAndPassword", "signInWithEmail:success")
                    val user = auth.currentUser
                    if (user != null) {
                        updateUI(user)
                    }
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("signInUserWithEmailAndPassword", "signInWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Authentication failed.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
    }

    private fun googleSignIn() {
        val signInIntent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result ->
        if (result.resultCode == Activity.RESULT_OK){
            val task = GoogleSignIn.getSignedInAccountFromIntent((result.data))
            handleResults(task)
        }
    }

    private fun handleResults(task: Task<GoogleSignInAccount>) {
        if (task.isSuccessful){
            val account : GoogleSignInAccount? = task.result
            if (account != null){
                updateUI(account)
            }
        } else {
            Toast.makeText(this, task.exception.toString(), Toast.LENGTH_SHORT).show()
        }
    }


    //Update UI function for Email Sign In
    private fun updateUI(account: FirebaseUser) {
        val sharedPref: SharedPreferences = getSharedPreferences("userprefs", Context.MODE_PRIVATE)
        sharedPref.edit().putString("email", account.email.toString()).apply()
        sharedPref.edit().putString("name", account.displayName.toString()).apply()

        val intent : Intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }


    //Update UI function for Google Sign In Account
    private fun updateUI(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener {
            if (it.isSuccessful){
                val sharedPref: SharedPreferences = getSharedPreferences("userprefs", Context.MODE_PRIVATE)
                sharedPref.edit().putString("email", account.email.toString()).apply()
                sharedPref.edit().putString("name", account.displayName.toString()).apply()

                val intent : Intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}