package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

enum class TransactionCategory(
    val arabicName: String,
    val isAddition: Boolean
) {
    // Income Categories
    SALARY("راتب شهري", true),
    FREELANCE("عمل حر وأرباح", true),
    INVESTMENT("عوائد واستثمار", true),
    BONUS("مكافأة وحوافز", true),
    GIFT("هدية أو منحة", true),
    OTHER_INCOME("إيداع آخر", true),

    // Expense Categories
    FOOD("طعام وبقالة", false),
    BILLS("فواتير وخدمات", false),
    SHOPPING("تسوق ومشتريات", false),
    TRANSPORT("مواصلات ووقود", false),
    HEALTH("صحة وعلاج", false),
    HOUSING("إيجار ومنزل", false),
    ENTERTAINMENT("ترفيه ونزهات", false),
    OTHER_EXPENSE("مصروف آخر", false);

    val icon: ImageVector
        get() = when (this) {
            SALARY -> Icons.Default.Payments
            FREELANCE -> Icons.Default.Work
            INVESTMENT -> Icons.Default.TrendingUp
            BONUS -> Icons.Default.Stars
            GIFT -> Icons.Default.CardGiftcard
            OTHER_INCOME -> Icons.Default.Savings
            FOOD -> Icons.Default.Restaurant
            BILLS -> Icons.Default.ReceiptLong
            SHOPPING -> Icons.Default.ShoppingBag
            TRANSPORT -> Icons.Default.DirectionsCar
            HEALTH -> Icons.Default.MedicalServices
            HOUSING -> Icons.Default.Home
            ENTERTAINMENT -> Icons.Default.Movie
            OTHER_EXPENSE -> Icons.Default.CreditCard
        }

    companion object {
        fun fromName(name: String, fallbackAddition: Boolean): TransactionCategory {
            return entries.firstOrNull { it.name == name || it.arabicName == name }
                ?: if (fallbackAddition) OTHER_INCOME else OTHER_EXPENSE
        }

        fun getCategories(isAddition: Boolean): List<TransactionCategory> {
            return entries.filter { it.isAddition == isAddition }
        }
    }
}
