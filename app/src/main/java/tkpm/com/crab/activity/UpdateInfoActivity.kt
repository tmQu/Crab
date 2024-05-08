package tkpm.com.crab.activity

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.squareup.picasso.Picasso
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import tkpm.com.crab.R
import tkpm.com.crab.activity.customer.CustomerMapsActivity
import tkpm.com.crab.activity.driver.DriverMapActivity
import tkpm.com.crab.api.APICallback
import tkpm.com.crab.api.APIService
import tkpm.com.crab.credential_service.CredentialService
import tkpm.com.crab.dialog.LoadingDialog
import tkpm.com.crab.objects.AccountResponse
import tkpm.com.crab.objects.BucketResponse
import tkpm.com.crab.objects.InternalAccount
import java.io.ByteArrayOutputStream

class UpdateInfoActivity : AppCompatActivity() {
    val REQUEST_CODE = 39
    val roleKeyArr = arrayOf("customer", "driver")
    val roleValueArr = arrayOf("Khách hàng", "Tài xế")

    private lateinit var loadingDialog: LoadingDialog

    private lateinit var avatar: ImageView
    private lateinit var phoneLayout: com.google.android.material.textfield.TextInputLayout
    private lateinit var phone: com.google.android.material.textfield.TextInputEditText
    private lateinit var name: com.google.android.material.textfield.TextInputEditText
    private lateinit var role: MaterialAutoCompleteTextView
    private lateinit var updateButton: MaterialButton

    private var avatarChangeStatus = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_update_info)

        // Get id for elements
        avatar = findViewById(R.id.update_info_avatar)
        phoneLayout = findViewById(R.id.update_info_phone_layout)
        phone = findViewById(R.id.update_info_phone)
        name = findViewById(R.id.update_info_name)
        role = findViewById(R.id.update_info_role)
        updateButton = findViewById(R.id.update_info_button)
        loadingDialog = LoadingDialog(this@UpdateInfoActivity)

        // Set default image for avatar
        avatar.setImageResource(R.drawable.grab_splash)

        // Change avatar option
        avatar.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, this.REQUEST_CODE)
        }

        // Disable phone input
        phoneLayout.isEnabled = false

        // Fill data in phone
        phone.setText(CredentialService().getAll().phone)

        // Set adapter for dropdown list
        role.setSimpleItems(roleValueArr)

        // Set default value for dropdown list
        role.setText(roleValueArr[0], false)

        // Set on click listener for update button
        updateButton.setOnClickListener {
            // Get the key based on the value
            val roleKey = roleKeyArr[roleValueArr.indexOf(role.text.toString())]

            // Name must not be empty
            if (name.text.toString().isEmpty()) {
                // Set error for name
                name.error = "Tên không được để trống"
                return@setOnClickListener
            }

            // Driver must has avatar
            if (roleKey == "driver" && !avatarChangeStatus) {
                Toast.makeText(this, "Tài xế phải có ảnh đại diện", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show loading dialog
            loadingDialog.startLoadingDialog()

            // Handle update info
            handleUpdateInfo()
        }
    }

    private fun handleUpdateInfo() {
        // Handle change info here
        // With avatarChangeStatus is true, we will upload the avatar to the server
        if (avatarChangeStatus) {
            // Upload avatar to the server, then update info

            // Get the avatar image
            val bitmap = (avatar.drawable as BitmapDrawable).bitmap
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            val byteArray = stream.toByteArray()
            val requestBody =
                RequestBody.create(MediaType.parse("multipart/form-data"), byteArray)
            val multipartBody =
                MultipartBody.Part.createFormData("file", "avatar_${CredentialService().get()}.jpg", requestBody)

            APIService().doPutMultipart<BucketResponse>("files", multipartBody, object:
                APICallback<Any> {
                override fun onSuccess(data: Any) {
                    data as BucketResponse
                    Log.d("API_SERVICE", "Data: $data")

                    // Update info to the server
                    val updateInfoRequest = InternalAccount(
                        id = CredentialService().get(),
                        name = name.text.toString(),
                        role = roleKeyArr[roleValueArr.indexOf(role.text.toString())],
                        avatar = data.id
                    )

                    APIService().doPatch<AccountResponse>("accounts", updateInfoRequest, object: APICallback<Any> {
                        override fun onSuccess(data: Any) {
                            data as AccountResponse

                            // Set the token
                            CredentialService().set(data.token)

                            Toast.makeText(this@UpdateInfoActivity, "Cập nhật thông tin thành công", Toast.LENGTH_SHORT).show()
                            loadingDialog.dismissDialog()

                            // Move to the MapsActivity by role
                            val intent =
                                if (CredentialService().getAll().role == "customer")
                                    Intent(
                                        this@UpdateInfoActivity,
                                        CustomerMapsActivity::class.java
                                    )
                                else
                                    Intent(this@UpdateInfoActivity, DriverMapActivity::class.java)

                            // Add flags to clear the back stack
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }

                        override fun onError(error: Throwable) {
                            Log.e("API_SERVICE", "Error: ${error.message}")
                            Toast.makeText(this@UpdateInfoActivity, "Cập nhật thông tin thất bại", Toast.LENGTH_SHORT).show()
                            loadingDialog.dismissDialog()
                        }
                    })
                }

                override fun onError(error: Throwable) {
                    Log.e("API_SERVICE", "Error: ${error.message}")
                    Toast.makeText(this@UpdateInfoActivity, "Cập nhật thông tin thất bại", Toast.LENGTH_SHORT).show()
                    loadingDialog.dismissDialog()
                }
            })

        } else {
            val updateInfoRequest = InternalAccount(
                id = CredentialService().get(),
                name = name.text.toString(),
                role = roleKeyArr[roleValueArr.indexOf(role.text.toString())]
            )

            APIService().doPatch<AccountResponse>("accounts", updateInfoRequest, object: APICallback<Any> {
                override fun onSuccess(data: Any) {
                    data as AccountResponse

                    // Set the token
                    CredentialService().set(data.token)

                    Toast.makeText(this@UpdateInfoActivity, "Cập nhật thông tin thành công", Toast.LENGTH_SHORT).show()
                    loadingDialog.dismissDialog()

                    // Move to the MapsActivity by role
                    val intent =
                        if (CredentialService().getAll().role == "customer")
                            Intent(this@UpdateInfoActivity, CustomerMapsActivity::class.java)
                        else
                            Intent(this@UpdateInfoActivity, DriverMapActivity::class.java)

                    // Add flags to clear the back stack
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }

                override fun onError(error: Throwable) {
                    Log.e("API_SERVICE", "Error: ${error.message}")
                    Toast.makeText(this@UpdateInfoActivity, "Cập nhật thông tin thất bại", Toast.LENGTH_SHORT).show()
                    loadingDialog.dismissDialog()
                }
            })
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == this.REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Picasso.get().load(data.data).into(avatar)
            avatarChangeStatus = true
        }
    }
}