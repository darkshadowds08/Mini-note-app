package com.example

import com.example.model.TransactionCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testBalanceCalculation() {
        val transactions = listOf(
            Pair(1000.0, true),
            Pair(250.0, false),
            Pair(500.0, true),
            Pair(100.0, false)
        )
        val totalIncome = transactions.filter { it.second }.sumOf { it.first }
        val totalExpense = transactions.filter { !it.second }.sumOf { it.first }
        val balance = totalIncome - totalExpense

        assertEquals(1500.0, totalIncome, 0.001)
        assertEquals(350.0, totalExpense, 0.001)
        assertEquals(1150.0, balance, 0.001)
    }

    @Test
    fun testCategoryLookup() {
        val salaryCat = TransactionCategory.fromName("SALARY", true)
        assertEquals("راتب شهري", salaryCat.arabicName)
        assertTrue(salaryCat.isAddition)

        val foodCat = TransactionCategory.fromName("FOOD", false)
        assertEquals("طعام وبقالة", foodCat.arabicName)
    }
}
