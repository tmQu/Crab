package tkpm.com.crab.activity.authentication.phone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View.NOT_FOCUSABLE
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import tkpm.com.crab.R
import tkpm.com.crab.dialog.LoadingDialog
import java.util.concurrent.TimeUnit

class PhoneLoginActivity : AppCompatActivity() {
    val REQUEST_CODE_PERMISSION = 83
    private val TAG = "PhoneLoginActivity"
    private lateinit var auth: FirebaseAuth

    private lateinit var phoneEdt: TextInputLayout
    private lateinit var countryCodeEdt: TextInputLayout
    private lateinit var continueBtn: MaterialButton
    private lateinit var errorMsg: TextView

    private var storedVerificationId: String = ""
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone_login)

        binding()

        countryCodeEdt.editText?.setText("+84")
        countryCodeEdt.focusable =  NOT_FOCUSABLE
        countryCodeEdt.isFocusableInTouchMode = true
        countryCodeEdt.editText?.setOnClickListener {
            errorMsg.text = "Crab hiện tại chỉ có mặt ở Việt Nam"
        }
        auth = FirebaseAuth.getInstance()


        continueBtn.setOnClickListener {
            val phoneNumber = getPhoneNumber()

            if (phoneNumber != null)
            {
                loginByPhone(phoneNumber)
            }
        }

        phoneEdt.editText?.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(phoneEdt.editText, InputMethodManager.SHOW_IMPLICIT)

        checkLocationPermissions()
    }

    private fun binding()
    {
        phoneEdt = findViewById(R.id.phone_input)
        countryCodeEdt = findViewById(R.id.phone_prefix_input)
        continueBtn = findViewById(R.id.continue_button)
        errorMsg = findViewById(R.id.error_message)
    }

    private fun getPhoneNumber(): String?
    {

        val countryCode = countryCodeEdt.editText?.text.toString()
        var phoneNumber = phoneEdt.editText?.text.toString()
        if(phoneNumber.isNotEmpty() && phoneNumber[0] == '0')
        {
            phoneNumber = phoneNumber.substring(1)
        }
        if (phoneNumber.isNotEmpty())
        {
            val phone = "$countryCode$phoneNumber"
            // check phone
            val regex = Regex("^\\+84[0-9]{9,10}\$")
            if (!regex.matches(phone))
            {
                phoneEdt.error = "Số điện thoại không hợp lệ"
                return null
            }
            return phone
        }
        else
        {
            phoneEdt.error = "Vui lòng nhập số điện thoại"
        }
        return null
    }



    private fun loginByPhone(phoneNumber: String)
    {
        val progressDialog = LoadingDialog(this)
        progressDialog.startLoadingDialog()
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber) // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this) // Activity (for callback binding)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // This callback will be invoked in two situations:
                    // 1 - Instant verification. In some cases the phone number can be instantly
                    //     verified without needing to send or enter a verification code.
                    // 2 - Auto-retrieval. On some devices Google Play services can automatically
                    //     detect the incoming verification SMS and perform verification without
                    //     user action.

                    Log.d(TAG, "onVerificationCompleted:$credential")
                    signInWithPhoneAuthCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    // This callback is invoked in an invalid request for verification is made,
                    // for instance if the the phone number format is not valid.
                    Log.w(TAG, "onVerificationFailed", e)

                    if (e is FirebaseAuthInvalidCredentialsException) {
                        // Invalid request
                    } else if (e is FirebaseTooManyRequestsException) {
                        // The SMS quota for the project has been exceeded
                    } else if (e is FirebaseAuthMissingActivityForRecaptchaException) {
                        // reCAPTCHA verification attempted with null Activity
                    }

                    // Show a message and update the UI
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken,
                ) {
                    // The SMS verification code has been sent to the provided phone number, we
                    // now need to ask the user to enter the code and then construct a credential
                    // by combining the code with a verification ID.
                    Log.d(TAG, "onCodeSent:$verificationId")

                    // Save verification ID and resending token so we can use them later
                    storedVerificationId = verificationId
                    resendToken = token
                    val intent = Intent(this@PhoneLoginActivity, PhoneVerificationActivity::class.java)
                    intent.putExtra("storedVerificationId", storedVerificationId)
                    intent.putExtra("resendToken", resendToken)
                    intent.putExtra("phoneNumber", getPhoneNumber())
                    progressDialog.dismissDialog()
                    startActivity(intent)

                }
            }) // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")

                    val user = task.result?.user
                } else {
                    // Sign in failed, display a message and update the UI
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        // The verification code entered was invalid
                        Toast.makeText(this, "Đăng nhập thất bại", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
    }

    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "Permission granted")
        } else {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ), REQUEST_CODE_PERMISSION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResults.isNotEmpty()) {
                for (result in grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "Permission denied")
                        return
                    }
                }
                Log.d(TAG, "Permission granted")
            }
        }
    }
}