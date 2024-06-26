package tkpm.com.crab.credential_service


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat.startActivity
import com.auth0.android.jwt.JWT
import tkpm.com.crab.MainActivity
import tkpm.com.crab.api.APICallback
import tkpm.com.crab.api.APIService
import tkpm.com.crab.objects.AccountRequest
import tkpm.com.crab.objects.InternalAccount
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
            jwt.getClaim("_id").asString()!!

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
            } else {
                APIService().doGet<InternalAccount>(
                    "accounts/get-user/${
                        jwt.getClaim("phone").asString()
                    }", object :
                        APICallback<Any> {
                        override fun onSuccess(data: Any) {
                            data as InternalAccount
                            Log.d("CREDENTIAL_SERVICE", "Credential is valid")
                            return
                        }

                        override fun onError(error: Throwable) {
                            // Handle exception
                            Log.e("CREDENTIAL_SERVICE", "Error in getting credential", error)

                            // Erase credential
                            CredentialService().erase()

                            // Move back to main activity
                            val i = Intent(context, MainActivity::class.java)
                            startActivity(context, i, null)
                        }
                    })
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

    fun getAll(): InternalAccount {
        return try {
            val credentileFile = File(CREDENTIAL_DIR)
            if (!credentileFile.exists()) {
                AccountRequest()
            }

            val inputStream: InputStream = FileInputStream(CREDENTIAL_DIR)
            val credentialJWT = inputStream.bufferedReader().use { it.readText() }

            val jwt = JWT(credentialJWT)

            val id = jwt.getClaim("_id").asString()
            val phone = jwt.getClaim("phone").asString()
            val name = jwt.getClaim("name").asString()
            val role = jwt.getClaim("role").asString()
            val avatar = jwt.getClaim("avatar").asString()

            InternalAccount(
                id = id ?: "",
                phone = phone ?: "",
                name = name ?: "",
                role = role ?: "",
                avatar = avatar ?: ""
            )
        } catch (t: Throwable) {
            Log.e("CREDENTIAL_SERVICE", "Error in getting all credential", t)
            InternalAccount()
        }
    }

    fun isNewUser(): Boolean {
        return try {
            val credentileFile = File(CREDENTIAL_DIR)
            if (!credentileFile.exists()) {
                return false
            }

            val inputStream: InputStream = FileInputStream(CREDENTIAL_DIR)
            val credentialJWT = inputStream.bufferedReader().use { it.readText() }

            val jwt = JWT(credentialJWT)

            val id = jwt.getClaim("_id").asString()
            val name = jwt.getClaim("name").asString()

            id != null && name == null
        } catch (t: Throwable) {
            Log.e("CREDENTIAL_SERVICE", "Error in getting all credential", t)
            false
        }
    }
}