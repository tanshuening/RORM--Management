package com.examples.rormmanagement

import android.app.AlertDialog
import android.content.Context
import android.util.AttributeSet
import android.widget.ArrayAdapter
import androidx.appcompat.widget.AppCompatSpinner

class MultiSelectSpinner : AppCompatSpinner {

    private var items: Array<String> = emptyArray()
    private var selectedItems: BooleanArray = BooleanArray(0)
    private var selectedItemStrings: MutableList<String> = mutableListOf()
    private var adapter: ArrayAdapter<String>

    constructor(context: Context) : super(context) {
        adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item)
        super.setAdapter(adapter)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item)
        super.setAdapter(adapter)
    }

    fun setItems(items: Array<String>) {
        this.items = items
        this.selectedItems = BooleanArray(items.size)
        adapter.clear()
        adapter.add("Select items")
    }

    fun setSelectedItems(selectedItemsList: List<String>) {
        selectedItemStrings.clear()
        selectedItems.fill(false)
        selectedItemsList.forEach { item ->
            val index = items.indexOf(item)
            if (index >= 0) {
                selectedItems[index] = true
                selectedItemStrings.add(item)
            }
        }
        adapter.clear()
        adapter.add(if (selectedItemStrings.isNotEmpty()) selectedItemStrings.joinToString(", ") else "Select items")
    }

    override fun performClick(): Boolean {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Select items")
        builder.setMultiChoiceItems(items, selectedItems) { _, which, isChecked ->
            selectedItems[which] = isChecked
        }
        builder.setPositiveButton("OK") { _, _ ->
            selectedItemStrings.clear()
            for (i in items.indices) {
                if (selectedItems[i]) {
                    selectedItemStrings.add(items[i])
                }
            }
            adapter.clear()
            adapter.add(if (selectedItemStrings.isNotEmpty()) selectedItemStrings.joinToString(", ") else "Select items")
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
        return true
    }

    fun getSelectedItems(): List<String> {
        return selectedItemStrings
    }
}
