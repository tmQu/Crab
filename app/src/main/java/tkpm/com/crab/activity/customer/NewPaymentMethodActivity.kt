package tkpm.com.crab.activity.customer

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import tkpm.com.crab.R
import tkpm.com.crab.api.APICallback
import tkpm.com.crab.api.APIService
import tkpm.com.crab.credential_service.CredentialService
import tkpm.com.crab.dialog.LoadingDialog
import tkpm.com.crab.objects.PaymentMethodRequest
import tkpm.com.crab.utils.CreditCardNumberFormattingTextWatcher


class NewPaymentMethodActivity : AppCompatActivity() {
    private lateinit var loadingDialog: LoadingDialog

    private lateinit var name: com.google.android.material.textfield.TextInputEditText
    private lateinit var number: com.google.android.material.textfield.TextInputEditText
    private lateinit var exp: com.google.android.material.textfield.TextInputEditText
    private lateinit var cvv: com.google.android.material.textfield.TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_new_payment_method)

        // Set id for elements
        name = findViewById(R.id.new_payment_method_name)
        number = findViewById(R.id.new_payment_method_number)
        exp = findViewById(R.id.new_payment_method_exp)
        cvv = findViewById(R.id.new_payment_method_cvv)
        loadingDialog = LoadingDialog(this@NewPaymentMethodActivity)

        // Use watcher for number
        number.addTextChangedListener(
            CreditCardNumberFormattingTextWatcher()
        )

        // Use watcher for exp
        exp.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(p0: Editable?) {}

                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                override fun onTextChanged(p0: CharSequence?, start: Int, removed: Int, added: Int) {
                    if (start == 1 && start+added == 2 && p0?.contains('/') == false) {
                        exp.setText(p0.toString() + "/")
                        exp.setSelection(exp.text?.length ?: 0)
                    } else if (start == 3 && start-removed == 2 && p0?.contains('/') == true) {
                        exp.setText(p0.toString().replace("/", ""))
                        exp.setSelection(exp.text?.length ?: 0)
                    }
                }
            })

        // Set onClickListener for newPaymentMethodButton
        findViewById<com.google.android.material.button.MaterialButton>(R.id.new_payment_method_save).setOnClickListener {
            val nameText = name.text.toString()
            val numberText = number.text.toString()
            val expText = exp.text.toString()
            val cvvText = cvv.text.toString()

            // Show error if any field is empty
            if (nameText.isEmpty()) {
                name.error = "Tên chủ thẻ không được để trống"
                return@setOnClickListener
            }

            if (numberText.isEmpty()) {
                number.error = "Số thẻ không được để trống"
                return@setOnClickListener
            }

            if (expText.isEmpty()) {
                exp.error = "Ngày hết hạn không được để trống"
                return@setOnClickListener
            }

            if (cvvText.isEmpty()) {
                cvv.error = "CVV không được để trống"
                return@setOnClickListener
            }

            // Check if number has 19 characters
            if (numberText.length < 19) {
                number.error = "Số thẻ không hợp lệ"
                return@setOnClickListener
            }

            val paymentMethodRequest = PaymentMethodRequest(
                id = "",
                name = nameText,
                number = numberText.replace(" ", ""),
                exp = expText,
                cvv = cvvText
            )

            // Save payment method
            APIService().doPost<Any>("accounts/${CredentialService().get()}/payment-methods", paymentMethodRequest, object:
                APICallback<Any> {
                override fun onSuccess(data: Any) {
                    Toast.makeText(this@NewPaymentMethodActivity, "Cập nhật thông tin thành công", Toast.LENGTH_SHORT).show()
                    loadingDialog.dismissDialog()

                    // Back to previous activity
                    finish()
                }

                override fun onError(error: Throwable) {
                    Log.e("API_SERVICE", "Error: ${error.message}")
                    Toast.makeText(this@NewPaymentMethodActivity, "Cập nhật thông tin thất bại", Toast.LENGTH_SHORT).show()
                    loadingDialog.dismissDialog()
                }
            })
        }
    }
}