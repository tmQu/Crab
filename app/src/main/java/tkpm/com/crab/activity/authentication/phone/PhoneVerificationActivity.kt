package tkpm.com.crab.activity.authentication.phone

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import tkpm.com.crab.R
import tkpm.com.crab.activity.customer.MapsActivity
import tkpm.com.crab.activity.UpdateInfoActivity
import tkpm.com.crab.activity.driver.DriverMapActivity
import tkpm.com.crab.api.APICallback
import tkpm.com.crab.api.APIService
import tkpm.com.crab.credential_service.CredentialService
import tkpm.com.crab.dialog.LoadingDialog
import tkpm.com.crab.objects.AccountRequest
import tkpm.com.crab.objects.AccountResponse
import java.util.concurrent.TimeUnit


class PhoneVerificationActivity : AppCompatActivity() {
    private val TAG = "PhoneVerificationActivity"
    private lateinit var auth: FirebaseAuth

    private var storedVerificationId: String = ""
    private var phoneNumber: String = ""
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken

    private lateinit var otpInput: TextInputLayout
    private lateinit var errorMsg: TextView

    private var tryCount = 3
    private var timeOut = 60

    private lateinit var loadingDialog: LoadingDialog

    private val handler = Handler(Looper.getMainLooper())
    private val runnable = object: Runnable {
        override fun run() {
            findViewById<TextView>(R.id.resend_otp_msg).text = "Gửi lại mã OTP sau $timeOut giây"
            timeOut--
            if(timeOut == 0)
            {
                onClickSendOTPAgain()
                timeOut = 60
            }
            handler.postDelayed(this, 1000)
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone_verification)

        otpInput = findViewById(R.id.otp_input)
        errorMsg = findViewById(R.id.error_message)

        otpInput.editText?.addTextChangedListener {
            if (it?.length == 6) {
                verficationOtp()
            }
            Log.d(TAG, "OTP: ${it.toString()}")
        }

        storedVerificationId = intent.getStringExtra("storedVerificationId") ?: ""
        phoneNumber = intent.getStringExtra("phoneNumber") ?: ""
        if (storedVerificationId == "") {

        }
        loadingDialog = LoadingDialog(this)
        auth = FirebaseAuth.getInstance()
        handler.postDelayed(runnable, 1000)

        otpInput.editText?.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(otpInput.editText, InputMethodManager.SHOW_IMPLICIT)


    }

    private fun verficationOtp() {

        val otp = otpInput.editText?.text.toString()
        if (otp.length != 6)
        {
            return
        }
        loadingDialog = LoadingDialog(this)
        loadingDialog.startLoadingDialog()
        val credential = PhoneAuthProvider.getCredential(storedVerificationId, otp)
        signInWithPhoneAuthCredential(credential)

    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    val user = task.result?.user

                    val accountRequest = AccountRequest(
                        phone = user?.phoneNumber ?: "",
                        uid = user?.uid ?: ""
                    )

                    // Sign in/Sign up with firebase API
                    val context = this
                    APIService().doPost<AccountResponse>("firebase/auth", accountRequest, object :
                        APICallback<Any> {
                        override fun onSuccess(data: Any) {
                            data as AccountResponse

                            // Set the token
                            CredentialService().set(data.token)

                            // Move the MapsActivity when the user is logged in
                            // If the user is a new user, move to UpdateInfoActivity
                            val intent =
                                if (CredentialService().isNewUser()) {
                                    Intent(context, UpdateInfoActivity::class.java)
                                } else {
                                    if(CredentialService().getAll().role == "driver")
                                        Intent(context, DriverMapActivity::class.java)
                                    else
                                        // Move to MapsActivity (Customer)
                                        Intent(context, MapsActivity::class.java)
                                }

                            // Clear all activities in the back stack
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }

                        override fun onError(error: Throwable) {
                            Log.e(TAG, "Error: ${error.message}")
                            loadingDialog.dismissDialog()
                        }
                    })
                } else {
                    // Sign in failed, display a message and update the UI
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        // The verification code entered was invalid
                        Toast.makeText(this, "Mã OTP không hợp lệ", Toast.LENGTH_SHORT).show()
                        tryCount--
                        if(tryCount == 0)
                        {
                            errorMsg.text = "Mã mới đã được gửi đến số điện thoại của bạn"
                            onClickSendOTPAgain()
                        }
                        else{
                            errorMsg.setText("Mã OTP không hợp lệ, bạn còn $tryCount lần thử")
                        }
                        loadingDialog.dismissDialog()
                    }
                    else {
                        // Update UI
                        Toast.makeText(this, "Đăng nhập thất bại", Toast.LENGTH_SHORT).show()
                        loadingDialog.dismissDialog()
                        finish()
                    }

                }
            }
    }


    private fun onClickSendOTPAgain()
    {
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
                        Toast.makeText(this@PhoneVerificationActivity, "Quá nhiều request", Toast.LENGTH_SHORT).show()
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
                    timeOut = 60
                    tryCount = 3


                }
            }) // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

}