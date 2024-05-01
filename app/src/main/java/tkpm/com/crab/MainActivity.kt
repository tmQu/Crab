package tkpm.com.crab

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.firebase.FirebaseApp
import tkpm.com.crab.activity.customer.MapsActivity
import tkpm.com.crab.activity.authentication.phone.PhoneLoginActivity
import tkpm.com.crab.credential_service.CredentialService

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
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        // Configure the behavior of the hidden system bars.
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        logo = findViewById(R.id.logo)
        bottomImage = findViewById(R.id.bottom_image)
        logo.animation = AnimationUtils.loadAnimation(this, R.anim.bottom_animation)
        logo.animation = AnimationUtils.loadAnimation(this, R.anim.logo_animation)

        FirebaseApp.initializeApp(this)

        // Credential validation
        if (CredentialService().get() != "") {
            CredentialService().credentialValidation(this)
        }

        Handler(Looper.getMainLooper()).postDelayed({
//            if (checkAuthen()) {
//                val intent = Intent(this, PhoneLoginActivity::class.java)
//                startActivity(intent)
//                Log.i("MainActivity", "User is already logged in")
//                finish()
//            } else {
//                val intent = Intent(this, LoginActivity::class.java)
//                val pairs = arrayOf<Pair<View, String>>(
//                    Pair(logo, "logo_image")
//                )
//
//                val options =
//                    ActivityOptions.makeSceneTransitionAnimation(this@MainActivity, *pairs)
//                startActivity(intent, options.toBundle())
//                finish()
//            }

            // Check if user login or not
            // If user not login, redirect to login page
            if (CredentialService().get() == "") {
                val intent = Intent(this@MainActivity, PhoneLoginActivity::class.java)
                startActivity(intent)
                finish()
            } else
            {
                // get user information and save to shared preference
                val intent = Intent(this@MainActivity, MapsActivity::class.java)
                startActivity(intent)
                finish()
            }
        }, 1500)
    }
}