package tkpm.com.crab

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent


// THIS CUSTOM CLASS IS USED TO CLEAR THE FOCUS OF THE AUTOCOMPLETE TEXTVIEW
// WHEN THE KEYBOARD IS HIDE (USER HIDE THE KEYBOARD BY PRESSING BACK)

// REFERENCE: https://code.luasoftware.com/tutorials/android/edittext-clear-focus-on-keyboard-hidden
class CustomeAutocompleteTextView: androidx.appcompat.widget.AppCompatAutoCompleteTextView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)
    override fun onKeyPreIme(keyCode: Int, event: KeyEvent?): Boolean {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            clearFocus()
        }
        return super.onKeyPreIme(keyCode, event)
    }
}