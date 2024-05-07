package tkpm.com.crab.activity.driver

import android.os.Bundle
import android.util.Log
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import tkpm.com.crab.R
import tkpm.com.crab.api.APICallback
import tkpm.com.crab.api.APIService
import tkpm.com.crab.credential_service.CredentialService
import tkpm.com.crab.dialog.LoadingDialog
import tkpm.com.crab.objects.AccountResponse
import tkpm.com.crab.objects.Vehicle

class ChangeVehicleInfo : AppCompatActivity() {
    val vehicleKeysArr = arrayOf("motorbike", "car")
    val vehicleValuesArr = arrayOf("Xe máy", "Ô tô")

    private lateinit var loadingDialog: LoadingDialog

    private lateinit var vehicleField: MaterialAutoCompleteTextView
    private lateinit var plateField: com.google.android.material.textfield.TextInputEditText
    private lateinit var descriptionField: com.google.android.material.textfield.TextInputEditText
    private lateinit var submitButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_change_vehicle_info)

        // Set id for elements
        vehicleField = findViewById(R.id.vehicle_info_type)
        plateField = findViewById(R.id.vehicle_info_plate)
        descriptionField = findViewById(R.id.vehicle_info_description)
        submitButton = findViewById(R.id.vehicle_info_submit)
        loadingDialog = LoadingDialog(this@ChangeVehicleInfo)

        // Load data
        loadData()
    }

    private fun loadData() {
        loadingDialog.startLoadingDialog()

        APIService().doGet<Vehicle>("accounts/${CredentialService().get()}/vehicles", object:
            APICallback<Any> {
            override fun onSuccess(data: Any) {
                data as Vehicle

                // Dismiss loading dialog
                loadingDialog.dismissDialog()

                // Set items for dropdown
                vehicleField.setSimpleItems(vehicleValuesArr)

                // Set data to fields
                if (data.type == "")
                    vehicleField.setText("Xe máy", false)
                else
                    vehicleField.setText(vehicleValuesArr[vehicleKeysArr.indexOf(data.type)], false)

                plateField.setText(data.plate)
                descriptionField.setText(data.description)

                // Handle submit
                submitButton.setOnClickListener {
                    handleSubmit()
                }
            }

            override fun onError(error: Throwable) {
                Log.e("API_SERVICE", "Error: ${error.message}")
                Toast.makeText(this@ChangeVehicleInfo, "Không thể lấy thông tin phương tiện", Toast.LENGTH_SHORT).show()
                loadingDialog.dismissDialog()
            }
        })
    }

    private fun handleSubmit()
    {
        // All fields are required
        if (vehicleField.text.toString() == "")
        {
            vehicleField.error = "Loại phương tiện không được để trống"
            return
        }

        if (plateField.text.toString() == "")
        {
            plateField.error = "Biển số không được để trống"
            return
        }

        if (descriptionField.text.toString() == "")
        {
            descriptionField.error = "Mô tả không được để trống"
            return
        }

        // Get data from fields
        val vehicle = Vehicle(
            user = CredentialService().get(),
            type = vehicleKeysArr[vehicleValuesArr.indexOf(vehicleField.text.toString())],
            plate = plateField.text.toString(),
            description = descriptionField.text.toString()
        )

        // Show loading dialog
        loadingDialog.startLoadingDialog()

        // Send data to server
        APIService().doPatch<AccountResponse>("accounts/${CredentialService().get()}/vehicles", vehicle, object:
            APICallback<Any> {
            override fun onSuccess(data: Any) {
                Toast.makeText(this@ChangeVehicleInfo, "Cập nhật thông tin phương tiện thành công", Toast.LENGTH_SHORT).show()
                loadingDialog.dismissDialog()

                // Recreate the activity
                recreate()
            }

            override fun onError(error: Throwable) {
                Log.e("API_SERVICE", "Error: ${error.message}")
                Toast.makeText(this@ChangeVehicleInfo, "Không thể cập nhật thông tin phương tiện", Toast.LENGTH_SHORT).show()
                loadingDialog.dismissDialog()
            }
        })
    }
}