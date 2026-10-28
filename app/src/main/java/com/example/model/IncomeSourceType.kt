package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class IncomeSourceType(
    val titleArabic: String,
    val subtitleArabic: String,
    val primaryColor: Color
) {
    SALARY(
        "وظيفة وراتب شهري",
        "الرواتب الأساسية، البدلات، والمكافآت الوظيفية",
        Color(0xFF2563EB)
    ),
    FREELANCE(
        "عمل حر ومشاريع",
        "مشاريع البرمجة، التصميم، والخدمات الاستشارية",
        Color(0xFF7C3AED)
    ),
    COMMERCE(
        "تجارة ومبيعات",
        "أرباح المتاجر الإلكترونية، المنتجات، والصفقات",
        Color(0xFFD97706)
    ),
    INVESTMENT(
        "استثمارات وعقارات",
        "عوائد الإيجارات، توزيعات الأسهم، وصناديق الاستثمار",
        Color(0xFF059669)
    ),
    DIGITAL_CONTENT(
        "محتوى ومنتجات رقمية",
        "يوتيوب، إعلانات، دورات تعليمية، وكتب إلكترونية",
        Color(0xFFE11D48)
    ),
    PASSIVE(
        "دخل سلبي وأصول",
        "حقوق الملكية، شراكات، وفوائد دورية",
        Color(0xFF0D9488)
    ),
    OTHER(
        "مصادر متنوعة أخرى",
        "هدايا، منح، وإيرادات غير مصنفة",
        Color(0xFF475569)
    );

    val icon: ImageVector
        get() = when (this) {
            SALARY -> Icons.Default.Payments
            FREELANCE -> Icons.Default.Computer
            COMMERCE -> Icons.Default.Storefront
            INVESTMENT -> Icons.Default.TrendingUp
            DIGITAL_CONTENT -> Icons.Default.VideoLibrary
            PASSIVE -> Icons.Default.Savings
            OTHER -> Icons.Default.AccountBalanceWallet
        }

    companion object {
        fun fromString(value: String): IncomeSourceType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OTHER
        }
    }
}
