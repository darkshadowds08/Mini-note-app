package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.IncomeSourceType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [TransactionEntity::class, IncomeSourceEntity::class, LedgerBookEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun incomeSourceDao(): IncomeSourceDao
    abstract fun ledgerBookDao(): LedgerBookDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mini_note_finance_db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(
                            database.transactionDao(),
                            database.incomeSourceDao(),
                            database.ledgerBookDao()
                        )
                    }
                }
            }

            suspend fun populateInitialData(
                transactionDao: TransactionDao,
                incomeSourceDao: IncomeSourceDao,
                ledgerBookDao: LedgerBookDao
            ) {
                val now = System.currentTimeMillis()
                val oneDay = 86400000L

                // 1. Initial Ledger Workspaces (واجهات النوتة)
                val defaultBookId = ledgerBookDao.insertBook(
                    LedgerBookEntity(
                        id = 1,
                        name = "النوتة الشخصية",
                        iconName = "wallet",
                        colorHex = "#0F766E"
                    )
                )

                ledgerBookDao.insertBook(
                    LedgerBookEntity(
                        id = 2,
                        name = "نوتة العمل والمشاريع",
                        iconName = "business",
                        colorHex = "#2563EB"
                    )
                )

                // 2. Initial Income Sources
                val salarySourceId = incomeSourceDao.insertIncomeSource(
                    IncomeSourceEntity(
                        name = "راتب شركة التقنية",
                        type = IncomeSourceType.SALARY.name,
                        targetMonthlyAmount = 15000.0,
                        expectedDay = 28,
                        notes = "الراتب الأساسي مع بدل السكن والاتصالات"
                    )
                )

                val freelanceSourceId = incomeSourceDao.insertIncomeSource(
                    IncomeSourceEntity(
                        name = "مشاريع العمل الحر والاستشارات",
                        type = IncomeSourceType.FREELANCE.name,
                        targetMonthlyAmount = 6000.0,
                        expectedDay = 15,
                        notes = "تطوير تطبيقات وتصميم واجهات"
                    )
                )

                val commerceSourceId = incomeSourceDao.insertIncomeSource(
                    IncomeSourceEntity(
                        name = "أرباح المتجر الإلكتروني",
                        type = IncomeSourceType.COMMERCE.name,
                        targetMonthlyAmount = 4500.0,
                        expectedDay = 10,
                        notes = "مبيعات إلكترونية وشحن سريع"
                    )
                )

                val investmentSourceId = incomeSourceDao.insertIncomeSource(
                    IncomeSourceEntity(
                        name = "عوائد وإيجار العقار السكني",
                        type = IncomeSourceType.INVESTMENT.name,
                        targetMonthlyAmount = 5000.0,
                        expectedDay = 1,
                        notes = "إيجار شهري ثابت للشقة"
                    )
                )

                val digitalSourceId = incomeSourceDao.insertIncomeSource(
                    IncomeSourceEntity(
                        name = "صناعة المحتوى والدورات",
                        type = IncomeSourceType.DIGITAL_CONTENT.name,
                        targetMonthlyAmount = 3000.0,
                        expectedDay = 20,
                        notes = "عوائد يوتيوب وباقات الاشتراك الرقمي"
                    )
                )

                // 3. Initial Transactions linked to default book
                val sampleList = listOf(
                    TransactionEntity(
                        title = "راتب الشهر الأساسي",
                        amount = 12500.0,
                        isAddition = true,
                        category = "SALARY",
                        timestamp = now - (oneDay * 4),
                        note = "تحويل الراتب الشهري للحساب",
                        sourceId = salarySourceId,
                        bookId = defaultBookId
                    ),
                    TransactionEntity(
                        title = "مشتريات وبقالة سوبرماركت",
                        amount = 650.0,
                        isAddition = false,
                        category = "FOOD",
                        timestamp = now - (oneDay * 3) - 3600000L,
                        note = "مستلزمات البيت الشهرية",
                        bookId = defaultBookId
                    ),
                    TransactionEntity(
                        title = "دفعة مشروع تطبيق جوال",
                        amount = 2300.0,
                        isAddition = true,
                        category = "FREELANCE",
                        timestamp = now - (oneDay * 2) - 18000000L,
                        note = "تسليم المرحلة الأولى للعميل",
                        sourceId = freelanceSourceId,
                        bookId = defaultBookId
                    ),
                    TransactionEntity(
                        title = "فاتورة الكهرباء والإنترنت",
                        amount = 420.0,
                        isAddition = false,
                        category = "BILLS",
                        timestamp = now - oneDay - 7200000L,
                        note = "سداد الفواتير الشهرية",
                        bookId = defaultBookId
                    ),
                    TransactionEntity(
                        title = "أرباح مبيعات أسبوعية للمتجر",
                        amount = 1850.0,
                        isAddition = true,
                        category = "OTHER_INCOME",
                        timestamp = now - 28000000L,
                        note = "تصفية أرباح الأسبوع من بوابة الدفع",
                        sourceId = commerceSourceId,
                        bookId = defaultBookId
                    ),
                    TransactionEntity(
                        title = "وقود ومواصلات",
                        amount = 350.0,
                        isAddition = false,
                        category = "TRANSPORT",
                        timestamp = now - 3600000L,
                        note = "وقود سيارة",
                        bookId = defaultBookId
                    )
                )
                for (item in sampleList) {
                    transactionDao.insertTransaction(item)
                }
            }
        }
    }
}
