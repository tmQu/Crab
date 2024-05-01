package tkpm.com.crab.credential_service


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat.startActivity
import com.auth0.android.jwt.JWT
import tkpm.com.crab.MainActivity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

class CredentialService {
    @SuppressLint("SdCardPath")
    private val CREDENTIAL_DIR: String = "/data/data/tkpm.com.crab/Credential"
    fun get(): String {
        // Get the credential from Credential file
        return try {
            val credentialFile = File(CREDENTIAL_DIR)
            if (!credentialFile.exists()) {
                return ""
            }

            val inputStream: InputStream = FileInputStream(CREDENTIAL_DIR)

            // Read the credential from the file
            val credentialJWT = inputStream.bufferedReader().use { it.readText() }

            // Check if jwt is empty
            if (credentialJWT == "") {
                return ""
            }

            val jwt = JWT(credentialJWT)

            // Get id from the credential
            val credential = jwt.getClaim("_id").asString()!!

            return credential

        } catch (t: Throwable) {
            // Handle exception
            Log.e("CREDENTIAL_SERVICE", "Error in getting credential", t)

            ""
        }
    }

    fun set(newCredential: String) {
        try {
            val credentialFile = File(CREDENTIAL_DIR)
            if (!credentialFile.exists()) {
                credentialFile.createNewFile()
            }
            FileOutputStream(credentialFile).use {
                it.write(newCredential.toByteArray())
                Log.d("CREDENTIAL_SERVICE", "Credential set successfully")
            }
        } catch (t: Throwable) {
            Log.e("CREDENTIAL_SERVICE", "Error in setting credential", t)
        }
    }

    fun erase() {
        try {
            val credentialFile = File(CREDENTIAL_DIR)
            if (credentialFile.exists()) {
                val fileOutputStream = FileOutputStream(credentialFile)
                fileOutputStream.channel.truncate(0)
                fileOutputStream.close()
                Log.d("CREDENTIAL_SERVICE", "Credential erased successfully")
            } else {
                Log.d("CREDENTIAL_SERVICE", "Credential file does not exist")
            }
        } catch (t: Throwable) {
            Log.e("CREDENTIAL_SERVICE", "Error in erasing credential content", t)
        }
    }

    fun credentialValidation(context: Context) {
        // Get the credential from Credential file
        try {
            val credentialFile = File(CREDENTIAL_DIR)
            if (!credentialFile.exists()) {
                return
            }

            val inputStream: InputStream = FileInputStream(CREDENTIAL_DIR)

            // Read the credential from the file
            val credentialJWT = inputStream.bufferedReader().use { it.readText() }
            val jwt = JWT(credentialJWT)

            // Get exp from the credential
            val exp = jwt.getClaim("exp").asDate()!!
            if (exp.before(java.util.Date())) {
                // Erase credential
                CredentialService().erase()

                // Move back to main activity
                val i = Intent(context, MainActivity::class.java)
                startActivity(context, i, null)
            }
        } catch (t: Throwable) {
            // Handle exception
            Log.e("CREDENTIAL_SERVICE", "Error in getting credential", t)

            // Erase credential
            CredentialService().erase()

            // Move back to main activity
            val i = Intent(context, MainActivity::class.java)
            startActivity(context, i, null)
        }
    }

    fun getJWTToken(): String {
        // Get the credential from Credential file
        return try {
            val credentialFile = File(CREDENTIAL_DIR)
            if (!credentialFile.exists()) {
                return ""
            }

            val inputStream: InputStream = FileInputStream(CREDENTIAL_DIR)

            // Read the credential from the file
            val token = inputStream.bufferedReader().use { it.readText() }

            return token

        } catch (t: Throwable) {
            // Handle exception
            Log.e("CREDENTIAL_SERVICE", "Error in getting credential", t)
            ""
        }
    }

    fun isExpired(): Boolean {
        // Get the credential from Credential file
        return try {
            val credentialFile = File(CREDENTIAL_DIR)
            if (!credentialFile.exists()) {
                return true
            }

            val inputStream: InputStream = FileInputStream(CREDENTIAL_DIR)

            // Read the credential from the file
            val credentialJWT = inputStream.bufferedReader().use { it.readText() }
            val jwt = JWT(credentialJWT)

            // Get exp from the credential
            val exp = jwt.getClaim("exp").asDate()!!
            exp.before(java.util.Date())

        } catch (t: Throwable) {
            // Handle exception
            Log.e("CREDENTIAL_SERVICE", "Error in getting credential", t)
            true
        }
    }
}