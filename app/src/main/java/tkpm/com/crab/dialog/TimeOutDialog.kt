package tkpm.com.crab.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import tkpm.com.crab.R

class TimeOutDialog: DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder =  AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater;

            val viewDialog = inflater.inflate(R.layout.timeout_dialog, null)
            builder
                .setView(viewDialog)
            viewDialog.findViewById<ImageButton>(R.id.close).setOnClickListener {
                this.dismiss()
            }



            builder.create()
        }?: throw IllegalStateException("Activity cannot be null")
    }
}