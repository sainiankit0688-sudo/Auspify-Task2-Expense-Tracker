package com.example.expensetracker

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var dao: ExpenseDao
    private lateinit var adapter: ExpenseAdapter
    private var editingExpense: Expense? = null

    private val categories = listOf("Food", "Travel", "Shopping", "Bills", "Other")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dao = AppDatabase.getDatabase(this).expenseDao()

        val editTitle = findViewById<EditText>(R.id.editTitle)
        val editAmount = findViewById<EditText>(R.id.editAmount)
        val spinnerCategory = findViewById<Spinner>(R.id.spinnerCategory)
        val buttonAdd = findViewById<Button>(R.id.buttonAdd)
        val textTotal = findViewById<TextView>(R.id.textTotal)
        val recyclerExpenses = findViewById<RecyclerView>(R.id.recyclerExpenses)

        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = spinnerAdapter

        adapter = ExpenseAdapter(
            onEdit = { expense ->
                editingExpense = expense
                editTitle.setText(expense.title)
                editAmount.setText(expense.amount.toString())
                val index = categories.indexOf(expense.category)
                if (index >= 0) spinnerCategory.setSelection(index)
                buttonAdd.text = "Update Expense"
            },
            onDelete = { expense ->
                lifecycleScope.launch { dao.delete(expense) }
                Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
            }
        )

        recyclerExpenses.layoutManager = LinearLayoutManager(this)
        recyclerExpenses.adapter = adapter

        buttonAdd.setOnClickListener {
            val title = editTitle.text.toString().trim()
            val amountText = editAmount.text.toString().trim()
            val category = spinnerCategory.selectedItem.toString()

            if (title.isEmpty() || amountText.isEmpty()) {
                Toast.makeText(this, "Title aur Amount bharo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountText.toDoubleOrNull()
            if (amount == null) {
                Toast.makeText(this, "Sahi amount likho", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val old = editingExpense
                if (old != null) {
                    dao.update(old.copy(title = title, amount = amount, category = category))
                    editingExpense = null
                    buttonAdd.text = "Add Expense"
                    Toast.makeText(this@MainActivity, "Updated", Toast.LENGTH_SHORT).show()
                } else {
                    dao.insert(Expense(title = title, amount = amount, category = category))
                    Toast.makeText(this@MainActivity, "Added", Toast.LENGTH_SHORT).show()
                }
                editTitle.text.clear()
                editAmount.text.clear()
            }
        }

        lifecycleScope.launch {
            dao.getAllExpenses().collect { list ->
                adapter.submitList(list)
            }
        }

        lifecycleScope.launch {
            dao.getTotalAmount().collect { total ->
                textTotal.text = "Total Spending: Rs. ${total ?: 0.0}"
            }
        }
    }
}