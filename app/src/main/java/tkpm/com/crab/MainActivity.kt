package tkpm.com.crab

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Pair
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.firebase.FirebaseApp
import tkpm.com.crab.activity.authentication.LoginActivity
import tkpm.com.crab.activity.authentication.phone.PhoneLoginActivity
import tkpm.com.crab.activity.authentication.phone.PhoneVerificationActivity

class MainActivity : AppCompatActivity() {



    private lateinit var logo: ImageView
    private lateinit var bottomImage: ImageView
    private fun checkAuthen(): Boolean {
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        // hide the system bar
        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        // Configure the behavior of the hidden system bars.
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())


        logo = findViewById(R.id.logo)
        bottomImage = findViewById(R.id.bottom_image)
        logo.animation = AnimationUtils.loadAnimation(this, R.anim.bottom_animation)
        logo.animation = AnimationUtils.loadAnimation(this, R.anim.logo_animation)

        FirebaseApp.initializeApp(this)
        Handler(Looper.getMainLooper()).postDelayed({
            if (checkAuthen())
            {
                val intent = Intent(this, PhoneLoginActivity::class.java)
                startActivity(intent)
                Log.i("MainActivity", "User is already logged in")
                finish()
            }
            else {
                val intent = android.content.Intent(this, LoginActivity::class.java)
                val pairs = arrayOf<Pair<View, String>>(
                    Pair(logo, "logo_image")
                )

                val options = ActivityOptions.makeSceneTransitionAnimation(this@MainActivity, *pairs)
                startActivity(intent, options.toBundle())
                finish()
            }
        }, 1500)


    }




}