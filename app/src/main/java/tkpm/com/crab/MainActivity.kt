package tkpm.com.crab

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.JsonObject
import tkpm.com.crab.activity.UpdateInfoActivity
import tkpm.com.crab.activity.authentication.phone.PhoneLoginActivity
import tkpm.com.crab.activity.customer.CustomerMapsActivity
import tkpm.com.crab.activity.driver.DriverMapActivity
import tkpm.com.crab.api.APICallback
import tkpm.com.crab.api.APIService
import tkpm.com.crab.credential_service.CredentialService

class MainActivity : AppCompatActivity() {
    private lateinit var logo: ImageView
    private lateinit var bottomImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Disable dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

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
            // Check if user login or not
            // If user not login, redirect to login page
            if (CredentialService().get() == "") {
                val intent = Intent(this@MainActivity, PhoneLoginActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                // If user login, redirect to update info page if user is new user
                val intent =
                    if (CredentialService().isNewUser())
                        Intent(this@MainActivity, UpdateInfoActivity::class.java)
                    else
                    {
                        if(CredentialService().getAll().role == "driver")
                            Intent(this@MainActivity, DriverMapActivity::class.java)
                        else
                            Intent(this@MainActivity, CustomerMapsActivity::class.java)
                    }
                FirebaseMessaging.getInstance().token.addOnCompleteListener {
                    sendRegistrationToServer(it.result.toString())
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }

            }
        }, 1500)
    }


    private fun sendRegistrationToServer(token: String) {
        val obj = JsonObject()
        obj.addProperty("user", CredentialService().getAll().id)
        obj.addProperty("token", token)

        APIService().doPost<Any>("notification/update-token", obj,  object : APICallback<Any> {
            override fun onSuccess(result: Any) {
                Log.i("Notification", "result: $result")
            }

            override fun onError(t: Throwable) {
                Log.i("Notification", "result: ${t.message}")

            }
        })
    }
}