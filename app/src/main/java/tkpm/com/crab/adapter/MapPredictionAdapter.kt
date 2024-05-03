package tkpm.com.crab.adapter
/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import android.content.Context
import android.graphics.Typeface
import android.text.style.CharacterStyle
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import com.google.android.libraries.places.api.model.AutocompletePrediction
import tkpm.com.crab.R


class MapPredictionAdapter(
    context: Context?,
    mResult: List<AutocompletePrediction> = listOf()
) : ArrayAdapter<AutocompletePrediction>(
    context!!, R.layout.map_prediction_row, R.id.text1, mResult
), Filterable{

    private var mResultList: List<AutocompletePrediction>? = null
    fun setPredictions(list: List<AutocompletePrediction>) {
        mResultList = list
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return mResultList!!.size
    }



    override fun getItem(position: Int): AutocompletePrediction {
        return mResultList!![position]
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val row = LayoutInflater.from(parent.context).inflate(R.layout.map_prediction_row, parent, false)
        val item = getItem(position)
        val textView1 = row.findViewById<View>(R.id.text1) as TextView
        val textView2 = row.findViewById<View>(R.id.text2) as TextView
        textView1.text = item.getPrimaryText(STYLE_BOLD)
        textView2.text = item.getSecondaryText(STYLE_BOLD)
        return row
    }


    override fun getFilter(): Filter {
        return object : Filter() {
            protected override fun performFiltering(constraint: CharSequence?): FilterResults? {
                val results = FilterResults()

                // We need a separate list to store the results, since
                // this is run asynchronously.
                var filterData = mResultList

                // Skip the autocomplete query if no constraints are given.

                results.values = filterData
                if (filterData != null) {
                    results.count = filterData.size
                } else {
                    results.count = 0
                }
                return results
            }

            protected override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                if (results != null && results.count > 0) {
                    // The API returned at least one result, update the data.
                    mResultList = results.values as List<AutocompletePrediction>
                    notifyDataSetChanged()
                } else {
                    // The API did not return any results, invalidate the data set.
                    notifyDataSetInvalidated()
                }
            }

            override fun convertResultToString(resultValue: Any?): CharSequence? {
//                 Override this method to display a readable result in the AutocompleteTextView
//                 when clicked.
                return if (resultValue is AutocompletePrediction) {
                    resultValue.getFullText(null)
//                    return null
                } else {
                    super.convertResultToString(resultValue)
                }
                return null
            }
        }
    }

    companion object {
        private const val TAG = "PlaceAutoCompleteAd"
        private val STYLE_BOLD: CharacterStyle = StyleSpan(Typeface.BOLD)
    }
}