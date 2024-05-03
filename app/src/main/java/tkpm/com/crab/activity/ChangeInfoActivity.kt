package tkpm.com.crab.activity

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View.INVISIBLE
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.squareup.picasso.Picasso
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import tkpm.com.crab.BuildConfig
import tkpm.com.crab.R
import tkpm.com.crab.activity.customer.MapsActivity
import tkpm.com.crab.api.APICallback
import tkpm.com.crab.api.APIService
import tkpm.com.crab.credential_service.CredentialService
import tkpm.com.crab.dialog.LoadingDialog
import tkpm.com.crab.objects.AccountResponse
import tkpm.com.crab.objects.BucketResponse
import tkpm.com.crab.objects.InternalAccount
import java.io.ByteArrayOutputStream

class ChangeInfoActivity : AppCompatActivity() {
    val REQUEST_CODE = 79

    private lateinit var loadingDialog: LoadingDialog

    private lateinit var avatar: ImageView
    private lateinit var phoneLayout: com.google.android.material.textfield.TextInputLayout
    private lateinit var phone: com.google.android.material.textfield.TextInputEditText
    private lateinit var name: com.google.android.material.textfield.TextInputEditText
    private lateinit var roleLayout: com.google.android.material.textfield.TextInputLayout
    private lateinit var role: MaterialAutoCompleteTextView
    private lateinit var updateButton: MaterialButton

    private var avatarChangeStatus = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_info)

        // Get id for elements
        avatar = findViewById(R.id.update_info_avatar)
        phoneLayout = findViewById(R.id.update_info_phone_layout)
        phone = findViewById(R.id.update_info_phone)
        name = findViewById(R.id.update_info_name)
        roleLayout = findViewById(R.id.update_info_role_layout)
        role = findViewById(R.id.update_info_role)
        updateButton = findViewById(R.id.update_info_button)
        loadingDialog = LoadingDialog(this@ChangeInfoActivity)

        // Get user data
        val user = CredentialService().getAll()

        // Log user data
        Log.d("USER_DATA", user.toString())

        // Check if user has avatar or not
        if (user.avatar == "")
            Picasso.get().load(R.drawable.grab_splash).into(avatar)
        else
            Picasso.get().load(BuildConfig.BASE_URL + "files/" + user.avatar).into(avatar)

        // Set avatar click listener
        avatar.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, this.REQUEST_CODE);
        }

        // Set phone
        phone.setText(user.phone)
        phoneLayout.isEnabled = false

        // Set name
        name.setText(user.name)

        // Set role
        role.setText(user.role)
        roleLayout.visibility = INVISIBLE

        // Set on click listener for update button
        updateButton.setOnClickListener {
            // Name must not be empty
            if (name.text.toString().isEmpty()) {
                // Set error for name
                name.error = "Tên không được để trống"
                return@setOnClickListener
            }

            // Show loading dialog
            loadingDialog.startLoadingDialog()

            // Handle update info
            handleUpdateInfo()
        }
    }

    private fun handleUpdateInfo(){
        if (avatarChangeStatus)
        {
            // Get the avatar image
            val bitmap = (avatar.drawable as BitmapDrawable).bitmap
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            val byteArray = stream.toByteArray()
            val requestBody =
                RequestBody.create(MediaType.parse("multipart/form-data"), byteArray)
            val multipartBody =
                MultipartBody.Part.createFormData("file", "avatar_${CredentialService().get()}.jpg", requestBody)

            // Upload avatar to the server
            APIService().doPutMultipart<BucketResponse>("files", multipartBody, object:
                APICallback<Any> {
                override fun onSuccess(data: Any) {
                    data as BucketResponse
                    Log.d("API_SERVICE", "Data: $data")

                    // Update info to the server
                    val updateInfoRequest = InternalAccount(
                        id = CredentialService().get(),
                        name = name.text.toString(),
                        role = role.text.toString(),
                        avatar = data.id
                    )

                    APIService().doPatch<AccountResponse>("accounts", updateInfoRequest, object: APICallback<Any> {
                        override fun onSuccess(data: Any) {
                            data as AccountResponse

                            // Set the token
                            CredentialService().set(data.token)

                            Toast.makeText(this@ChangeInfoActivity, "Cập nhật thông tin thành công", Toast.LENGTH_SHORT).show()
                            loadingDialog.dismissDialog()

                            // Move to previous page
                            finish()
                        }

                        override fun onError(error: Throwable) {
                            Log.e("API_SERVICE", "Error: ${error.message}")
                            Toast.makeText(this@ChangeInfoActivity, "Cập nhật thông tin thất bại", Toast.LENGTH_SHORT).show()
                            loadingDialog.dismissDialog()
                        }
                    })
                }

                override fun onError(error: Throwable) {
                    Log.e("API_SERVICE", "Error: ${error.message}")
                    Toast.makeText(this@ChangeInfoActivity, "Cập nhật thông tin thất bại", Toast.LENGTH_SHORT).show()
                    loadingDialog.dismissDialog()
                }
            })

        } else
        {
            val updateInfoRequest = InternalAccount(
                id = CredentialService().get(),
                name = name.text.toString(),
                role = role.text.toString()
            )

            // Update info
            APIService().doPatch<AccountResponse>("accounts", updateInfoRequest, object:
                APICallback<Any> {
                override fun onSuccess(data: Any) {
                    data as AccountResponse

                    // Set the token
                    CredentialService().set(data.token)

                    Toast.makeText(this@ChangeInfoActivity, "Cập nhật thông tin thành công", Toast.LENGTH_SHORT).show()
                    loadingDialog.dismissDialog()

                    // Go back to previous page
                    finish()
                }

                override fun onError(error: Throwable) {
                    Log.e("API_SERVICE", "Error: ${error.message}")
                    Toast.makeText(this@ChangeInfoActivity, "Cập nhật thông tin thất bại", Toast.LENGTH_SHORT).show()
                    loadingDialog.dismissDialog()
                }
            })
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == this.REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Picasso.get().load(data.data).into(avatar)
            avatarChangeStatus = true
        }
    }
}