package com.example.data.repository

import com.example.data.dao.StoreDao
import com.example.data.model.*
import com.example.ui.util.ClothingSizes
import com.example.ui.util.ClothingUnits
import com.example.ui.util.FormatUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

import androidx.room.withTransaction
import com.example.data.database.AppDatabase
import org.json.JSONArray
import org.json.JSONObject

class StoreRepository(
    private val dao: StoreDao,
    private val db: AppDatabase? = null
) {

    suspend fun <T> runInTransaction(block: suspend () -> T): T {
        return if (db != null) {
            db.withTransaction { block() }
        } else {
            block()
        }
    }

    val allProducts: Flow<List<ProductItem>> = dao.getAllProducts()
    val allSales: Flow<List<SaleTransaction>> = dao.getAllSales()
    val allPurchases: Flow<List<PurchaseRecord>> = dao.getAllPurchases()
    val allExpenses: Flow<List<ExpenseRecord>> = dao.getAllExpenses()
    val allEmployees: Flow<List<Employee>> = dao.getAllEmployees()
    val allEmployeeTransactions: Flow<List<EmployeeTransaction>> = dao.getAllEmployeeTransactions()
    val allDebts: Flow<List<DebtRecord>> = dao.getAllDebts()
    val allDebtPayments: Flow<List<DebtPayment>> = dao.getAllDebtPayments()
    val activeShift: Flow<CashDrawerShift?> = dao.getActiveShift()
    val allShifts: Flow<List<CashDrawerShift>> = dao.getAllShifts()
    val allCashMovements: Flow<List<CashDrawerMovement>> = dao.getAllCashMovements()
    val allExchangeAccounts: Flow<List<ExchangeAccount>> = dao.getAllExchangeAccounts()
    val allExchangeTransfers: Flow<List<ExchangeTransfer>> = dao.getAllExchangeTransfers()
    val allParties: Flow<List<PartyAccount>> = dao.getAllParties()
    val allAuditLogs: Flow<List<AuditLogRecord>> = dao.getAllAuditLogs()
    val storeSettings: Flow<StoreSettings?> = dao.getStoreSettings()

    suspend fun logAudit(action: String, actor: String = "المدير", details: String) {
        dao.insertAuditLog(AuditLogRecord(action = action, actor = actor, details = details, timestamp = System.currentTimeMillis()))
    }

    suspend fun saveStoreSettings(settings: StoreSettings) {
        val existing = dao.getStoreSettingsDirect()
        if (existing == null) {
            dao.insertStoreSettings(settings)
        } else {
            dao.updateStoreSettings(settings)
        }
        logAudit("تحديث الإعدادات", "المدير", "تم تحديث بيانات المحل والشعار ورقم الهاتف وإعدادات الفاتورة")
    }

    suspend fun seedInitialDataIfEmpty() {
        val existing = dao.getAllProducts().firstOrNull()
        if (existing.isNullOrEmpty()) {
            val now = System.currentTimeMillis()

            // 1. Menswear Products
            val p1 = ProductItem(
                category = "بشت",
                modelName = "بشت ملكي صيفي فاخر",
                color = "3 ألوان: أسود، كحلي، بيج",
                size = ClothingSizes.BIG,
                quantity = 24, // 2 درزن
                buyPrice = 280.0,
                sellPrice = 450.0,
                supplierName = "معمل الأصالة للأقمشة",
                purchaseDate = now - 86400000L * 3,
                invoiceNumber = "INV-101",
                barcode = "BSH-001",
                notes = "تشكيلة درزن ملكي صيفي",
                unit = ClothingUnits.DOZEN,
                colorsCount = 3,
                colorDetails = "أسود، كحلي، بيج"
            )
            val p2 = ProductItem(
                category = "كوت",
                modelName = "كوت رجالي صوف كلاسيك",
                color = "لونين: كحلي، رصاصي",
                size = ClothingSizes.MEDIUM,
                quantity = 12, // 1 درزن
                buyPrice = 160.0,
                sellPrice = 280.0,
                supplierName = "شركة النخبة للاستيراد",
                purchaseDate = now - 86400000L * 5,
                invoiceNumber = "INV-102",
                barcode = "COT-002",
                notes = "درزن مشكل ألوان كوت شتوي",
                unit = ClothingUnits.DOZEN,
                colorsCount = 2,
                colorDetails = "كحلي، رصاصي"
            )
            val p3 = ProductItem(
                category = "جاكت",
                modelName = "جاكيت سبورت كاجوال",
                color = "3 ألوان: بيج، زيتي، أسود",
                size = ClothingSizes.YOUTH,
                quantity = 6, // نص درزن
                buyPrice = 90.0,
                sellPrice = 160.0,
                supplierName = "معرض أزياء الشباب",
                purchaseDate = now - 86400000L * 2,
                invoiceNumber = "INV-103",
                barcode = "JCK-003",
                notes = "نص درزن شبابي حديث",
                unit = ClothingUnits.HALF_DOZEN,
                colorsCount = 3,
                colorDetails = "بيج، زيتي، أسود"
            )
            val p4 = ProductItem(
                category = "ثوب",
                modelName = "ثوب كويتي مطرز راقي",
                color = "لون واحد: أبيض ناصع",
                size = ClothingSizes.BIG,
                quantity = 20, // 1 كورجة
                buyPrice = 65.0,
                sellPrice = 120.0,
                supplierName = "وكالة أقمشة الخليج",
                purchaseDate = now - 86400000L * 4,
                invoiceNumber = "INV-104",
                barcode = "THB-004",
                notes = "كورجة أثواب أبيض ناصع",
                unit = ClothingUnits.KORJAH,
                colorsCount = 1,
                colorDetails = "أبيض ناصع"
            )
            val p5 = ProductItem(
                category = "دشداشة",
                modelName = "دشداشة شتوية دافئة",
                color = "لونين: رمادي، سكري",
                size = ClothingSizes.SMALL,
                quantity = 3, // قارب على النفاد
                buyPrice = 75.0,
                sellPrice = 135.0,
                supplierName = "معمل الفرسان للملبوسات",
                purchaseDate = now - 86400000L * 10,
                invoiceNumber = "INV-105",
                barcode = "DSH-005",
                notes = "متبقي 3 حبات فقط - قارب على النفاد",
                unit = ClothingUnits.PIECE,
                colorsCount = 2,
                colorDetails = "رمادي، سكري"
            )

            dao.insertProduct(p1)
            dao.insertProduct(p2)
            dao.insertProduct(p3)
            dao.insertProduct(p4)
            dao.insertProduct(p5)

            // 2. Exchange & Wallet Accounts
            val acc1 = ExchangeAccount(
                name = "صرافة الكريمي",
                accountNumber = "12345678",
                phone = "0501112233",
                balance = 1450.0,
                type = "صراف",
                notes = "الحساب الرئيسي للتحويلات"
            )
            val acc2 = ExchangeAccount(
                name = "محفظة جوالي / كاش",
                accountNumber = "0559998877",
                phone = "0559998877",
                balance = 600.0,
                type = "محفظة",
                notes = "دفع الزبائن عبر المحفظة"
            )
            dao.insertExchangeAccount(acc1)
            dao.insertExchangeAccount(acc2)

            // 3. Employees
            val emp1 = Employee(
                name = "أبو فهد",
                phone = "0501234567",
                role = "بائع صالة رئيسي",
                baseSalary = 3500.0,
                salaryType = "شهري",
                commissionPercent = 2.0
            )
            val emp2 = Employee(
                name = "وسام الكرخي",
                phone = "0559876543",
                role = "مساعد مبيعات وصندوق",
                baseSalary = 2800.0,
                salaryType = "شهري",
                commissionPercent = 1.5
            )
            val e1Id = dao.insertEmployee(emp1)
            val e2Id = dao.insertEmployee(emp2)

            // 4. Opening shift
            val shift = CashDrawerShift(
                openedAt = now - 3600000L * 4,
                openingBalance = 1500.0,
                isOpen = true,
                cashierName = "صاحب المحل",
                notes = "افتتاح الوردية الصباحية"
            )
            val shiftId = dao.insertShift(shift)

            dao.insertCashMovement(
                CashDrawerMovement(
                    shiftId = shiftId,
                    type = "CAPITAL_DEPOSIT",
                    amount = 1500.0,
                    isIncome = true,
                    title = "رصيد افتتاح الوردية",
                    details = "بداية اليوم",
                    timestamp = shift.openedAt
                )
            )

            // 5. Demo sale
            val demoSale = SaleTransaction(
                productId = 4,
                productName = "ثوب كويتي مطرز راقي",
                category = "ثوب",
                color = "لون واحد: أبيض ناصع",
                size = ClothingSizes.BIG,
                quantity = 2,
                unitBuyPrice = 65.0,
                unitSellPrice = 120.0,
                discount = 10.0,
                totalAmount = 230.0,
                profit = 100.0,
                customerName = "عبد الله السالم",
                customerPhone = "0541239876",
                paymentType = "نقداً",
                cashAmount = 230.0,
                employeeName = "أبو فهد",
                employeeId = e1Id,
                timestamp = now - 3600000L * 2,
                shiftId = shiftId,
                unit = ClothingUnits.PIECE
            )
            val sId = dao.insertSale(demoSale)
            dao.insertCashMovement(
                CashDrawerMovement(
                    shiftId = shiftId,
                    type = "SALE_CASH",
                    amount = 230.0,
                    isIncome = true,
                    title = "بيع نقدي: ثوب كويتي مطرز راقي (2 حبه)",
                    details = "الزبون: عبد الله السالم",
                    timestamp = demoSale.timestamp,
                    referenceSaleId = sId
                )
            )

            // 6. Demo expense
            val demoExpense = ExpenseRecord(
                category = "الضيافة والشاي",
                amount = 45.0,
                notes = "شاي وضيافة زبائن المعرض",
                receiptNumber = "EXP-01",
                timestamp = now - 3600000L,
                paidFromCash = true,
                shiftId = shiftId
            )
            val expId = dao.insertExpense(demoExpense)
            dao.insertCashMovement(
                CashDrawerMovement(
                    shiftId = shiftId,
                    type = "EXPENSE_PAID",
                    amount = 45.0,
                    isIncome = false,
                    title = "مصروف: الضيافة والشاي",
                    details = "شاي وضيافة زبائن المعرض",
                    timestamp = demoExpense.timestamp,
                    referenceExpenseId = expId
                )
            )

            // 7. Demo customer debt
            val demoDebt = DebtRecord(
                type = "CUSTOMER",
                partyName = "أحمد التميمي",
                phone = "0561122334",
                originalAmount = 320.0,
                paidAmount = 120.0,
                remainingAmount = 200.0,
                details = "شراء جاكيت سبورت - دفعة 120 ريال والمتبقي 200 ريال (عليه زلط)",
                dueDate = now + 86400000L * 5,
                createdAt = now - 86400000L,
                isSettled = false
            )
            dao.insertDebt(demoDebt)

            // 8. Demo supplier debt (عليا زلط للمورد)
            val demoSupplierDebt1 = DebtRecord(
                type = "SUPPLIER",
                partyName = "معمل الأصالة للأقمشة",
                phone = "0509988771",
                originalAmount = 15000.0,
                paidAmount = 0.0,
                remainingAmount = 15000.0,
                details = "متبقي شراء بضاعة بشت ملكي صيفي (عليا زلط للمورد)",
                dueDate = now + 86400000L * 20,
                createdAt = now - 86400000L * 3,
                isSettled = false
            )
            dao.insertDebt(demoSupplierDebt1)

            val demoSupplierDebt2 = DebtRecord(
                type = "SUPPLIER",
                partyName = "وكالة أقمشة الخليج",
                phone = "0554433221",
                originalAmount = 8000.0,
                paidAmount = 0.0,
                remainingAmount = 8000.0,
                details = "متبقي دفعة أثواب كويتية (عليا زلط للمورد)",
                dueDate = now + 86400000L * 25,
                createdAt = now - 86400000L * 4,
                isSettled = false
            )
            dao.insertDebt(demoSupplierDebt2)

            // 9. Demo Customer & Supplier Accounts (Parties)
            dao.insertParty(
                PartyAccount(
                    type = "CUSTOMER",
                    name = "أحمد التميمي",
                    phone = "0561122334",
                    address = "صنعاء - شارع حائل",
                    notes = "زبون دائم - عليه زلط سابقة",
                    initialBalance = 200.0,
                    createdAt = now - 86400000L
                )
            )
            dao.insertParty(
                PartyAccount(
                    type = "CUSTOMER",
                    name = "عبد الله السالم",
                    phone = "0541239876",
                    address = "شارع حدة",
                    notes = "زبون مشتريات نقدية مميز",
                    initialBalance = 0.0,
                    createdAt = now - 86400000L * 2
                )
            )
            dao.insertParty(
                PartyAccount(
                    type = "SUPPLIER",
                    name = "معمل الأصالة للأقمشة",
                    phone = "0509988771",
                    address = "سوق الأقمشة المركزي",
                    notes = "مورد بشوت ملكية - عليا زلط له 15,000 ريال",
                    initialBalance = 15000.0,
                    createdAt = now - 86400000L * 5
                )
            )
            dao.insertParty(
                PartyAccount(
                    type = "SUPPLIER",
                    name = "وكالة أقمشة الخليج",
                    phone = "0554433221",
                    address = "المنطقة الصناعية",
                    notes = "مورد أثواب كويتية - عليا زلط له 8,000 ريال",
                    initialBalance = 8000.0,
                    createdAt = now - 86400000L * 6
                )
            )
            dao.insertParty(
                PartyAccount(
                    type = "SUPPLIER",
                    name = "شركة النخبة للاستيراد",
                    phone = "0505544332",
                    address = "الشارع الرئيسي",
                    notes = "مورد أكوات صوف وجواكت - الحساب خالص",
                    initialBalance = 0.0,
                    createdAt = now - 86400000L * 10
                )
            )
        }
    }

    // --- Product CRUD ---
    suspend fun saveProduct(product: ProductItem): Long {
        return if (product.id == 0L) {
            dao.insertProduct(product)
        } else {
            dao.updateProduct(product)
            product.id
        }
    }

    suspend fun deleteProduct(id: Long) = dao.deleteProduct(id)

    // --- Sale Execution (Cash, Wallet Transfer, Split Half-Cash/Half-Transfer, Debt) ---
    suspend fun executeSale(
        product: ProductItem,
        quantity: Int,
        sellPrice: Double,
        discount: Double,
        customerName: String,
        customerPhone: String,
        paymentType: String,
        employee: Employee?,
        unit: String = ClothingUnits.PIECE,
        cashPart: Double = 0.0,
        transferPart: Double = 0.0,
        exchangeAccount: ExchangeAccount? = null,
        transferRef: String = ""
    ): SaleTransaction = executeSaleCart(
        lines = listOf(SaleLineInput(product, quantity, sellPrice, unit)),
        discount = discount,
        customerName = customerName,
        customerPhone = customerPhone,
        paymentType = paymentType,
        employee = employee,
        cashPart = cashPart,
        transferPart = transferPart,
        exchangeAccount = exchangeAccount,
        transferRef = transferRef
    ).single()

    /**
     * Saves every line of a cart in one Room transaction. Payment movements and
     * customer debt are deliberately recorded once for the invoice, not once per line.
     */
    suspend fun executeSaleCart(
        lines: List<SaleLineInput>,
        discount: Double,
        customerName: String,
        customerPhone: String,
        paymentType: String,
        employee: Employee?,
        cashPart: Double = 0.0,
        transferPart: Double = 0.0,
        exchangeAccount: ExchangeAccount? = null,
        transferRef: String = ""
    ): List<SaleTransaction> {
        require(lines.isNotEmpty()) { "سلة البيع فارغة." }
        require(lines.all { it.quantity > 0 }) { "يجب أن تكون كمية كل منتج أكبر من الصفر." }
        require(lines.all { it.sellPrice > 0 }) { "يجب أن يكون سعر كل منتج أكبر من الصفر." }
        require(discount >= 0) { "لا يمكن إدخال خصم بقيمة سالبة." }

        val subtotal = lines.sumOf { it.sellPrice * it.quantity }
        require(subtotal > 0) { "إجمالي الفاتورة يجب أن يكون أكبر من الصفر." }
        require(discount < subtotal) { "الخصم يجب أن يكون أقل من إجمالي الفاتورة." }
        val totalAmount = subtotal - discount
        val isCash = PaymentMethods.isCash(paymentType)
        val isTransfer = PaymentMethods.isTransfer(paymentType)
        val isSplit = PaymentMethods.isSplit(paymentType)
        val isCredit = PaymentMethods.isCredit(paymentType)
        require(isCash || isTransfer || isSplit || isCredit) { "طريقة دفع غير معروفة." }
        require(!isCredit || customerName.isNotBlank()) { "يجب كتابة اسم العميل للبيع الآجل." }
        require(!(isTransfer || isSplit) || exchangeAccount != null) { "يجب اختيار حساب الصراف أو المحفظة." }

        val finalCash = when {
            isCash -> totalAmount
            isSplit -> cashPart
            else -> 0.0
        }
        val finalTransfer = when {
            isTransfer -> totalAmount
            isSplit -> transferPart
            else -> 0.0
        }
        if (isSplit) {
            require(finalCash > 0 && finalTransfer > 0) { "يجب إدخال مبلغ الكاش والتحويل في الدفع المختلط." }
        }
        require(kotlin.math.abs(finalCash + finalTransfer + (if (isCredit) totalAmount else 0.0) - totalAmount) <= 0.01) {
            "مجموع مبالغ الدفع لا يساوي إجمالي الفاتورة."
        }

        return runInTransaction {
            val now = System.currentTimeMillis()
            val invoiceNumber = "INV-$now"
            val activeShift = dao.getActiveShift().firstOrNull()
            val shiftId = activeShift?.id ?: 0L
            val freshProducts = lines.map { line ->
                dao.getProductById(line.product.id)
                    ?: throw IllegalStateException("المنتج ${line.product.modelName} غير موجود.")
            }
            freshProducts.forEachIndexed { index, fresh ->
                val requiredPieces = lines[index].quantity * ClothingUnits.getMultiplier(lines[index].unit)
                if (fresh.quantity < requiredPieces) {
                    throw IllegalStateException("المنتج ${fresh.modelName}: الكمية المطلوبة $requiredPieces، والمتوفر ${fresh.quantity}.")
                }
                if (dao.decrementStockSafe(fresh.id, requiredPieces, now) == 0) {
                    throw IllegalStateException("تعذر خصم مخزون المنتج ${fresh.modelName} بسبب حركة متزامنة.")
                }
            }

            val saved = mutableListOf<SaleTransaction>()
            var allocatedDiscount = 0.0
            lines.forEachIndexed { index, line ->
                val fresh = freshProducts[index]
                val gross = line.sellPrice * line.quantity
                val lineDiscount = if (index == lines.lastIndex) discount - allocatedDiscount else discount * gross / subtotal
                allocatedDiscount += lineDiscount
                val lineTotal = gross - lineDiscount
                val pieces = line.quantity * ClothingUnits.getMultiplier(line.unit)
                val sale = SaleTransaction(
                    productId = fresh.id,
                    productName = fresh.modelName,
                    category = fresh.category,
                    color = fresh.color,
                    size = fresh.size,
                    quantity = pieces,
                    unitBuyPrice = fresh.buyPrice,
                    unitSellPrice = line.sellPrice / ClothingUnits.getMultiplier(line.unit),
                    discount = lineDiscount,
                    totalAmount = lineTotal,
                    profit = lineTotal - fresh.buyPrice * pieces,
                    customerName = customerName.ifBlank { "زبون نقدي" },
                    customerPhone = customerPhone,
                    paymentType = paymentType,
                    cashAmount = if (index == 0) finalCash else 0.0,
                    transferAmount = if (index == 0) finalTransfer else 0.0,
                    exchangeAccountId = exchangeAccount?.id ?: 0L,
                    exchangeAccountName = exchangeAccount?.name ?: "",
                    transferRef = transferRef,
                    employeeName = employee?.name ?: "صاحب المحل",
                    employeeId = employee?.id ?: 0L,
                    timestamp = now,
                    shiftId = shiftId,
                    unit = line.unit,
                    invoiceNumber = invoiceNumber
                )
                saved += sale.copy(id = dao.insertSale(sale))
            }

            val first = saved.first()
            if (finalCash > 0) {
                dao.insertCashMovement(CashDrawerMovement(
                    shiftId = shiftId, type = "SALE_CASH", amount = finalCash, isIncome = true,
                    title = "مبيعات نقدية: فاتورة $invoiceNumber",
                    details = "الزبون: ${customerName.ifBlank { "نقدي" }}",
                    timestamp = now, referenceSaleId = first.id
                ))
            }
            if (finalTransfer > 0 && exchangeAccount != null) {
                dao.updateExchangeBalance(exchangeAccount.id, finalTransfer)
                dao.insertExchangeTransfer(ExchangeTransfer(
                    accountId = exchangeAccount.id, accountName = exchangeAccount.name,
                    type = "مبيعات تحويل", amount = finalTransfer, isCredit = true,
                    title = "مبيعات تحويل: فاتورة $invoiceNumber", referenceNumber = transferRef,
                    notes = "الزبون: ${customerName.ifBlank { "تحويل" }}", timestamp = now
                ))
            }
            if (isCredit) {
                dao.insertDebt(DebtRecord(
                    type = "CUSTOMER", partyName = customerName, phone = customerPhone,
                    originalAmount = totalAmount, remainingAmount = totalAmount,
                    details = "فاتورة بيع آجل $invoiceNumber", referenceSaleId = first.id,
                    dueDate = now + 86400000L * 15, createdAt = now, isSettled = false
                ))
            }
            dao.insertAuditLog(AuditLogRecord(
                action = "تسجيل بيع", actor = employee?.name ?: "صاحب المحل",
                details = "فاتورة $invoiceNumber تحتوي ${saved.size} منتجاً بإجمالي ${FormatUtils.formatCurrency(totalAmount)}",
                timestamp = now
            ))
            saved
        }
    }

    // --- Delete / Reverse Sale ---
    suspend fun deleteSale(saleId: Long) {
        runInTransaction {
            val sale = dao.getSaleById(saleId) ?: return@runInTransaction

            // 1. Safely restore stock back
            dao.incrementStock(sale.productId, sale.quantity)

            // 2. Reverse cash movements
            dao.deleteCashMovementsForSale(saleId)

            // 3. Reverse exchange transfer if any
            if (sale.transferAmount > 0 && sale.exchangeAccountId > 0) {
                dao.updateExchangeBalance(sale.exchangeAccountId, -sale.transferAmount)
            }

            // 4. Reverse debt if this was debt sale
            val debt = dao.getDebtBySaleId(saleId)
            if (debt != null) {
                dao.deleteDebt(debt.id)
            }

            // 5. Delete sale
            dao.deleteSale(saleId)

            dao.insertAuditLog(
                AuditLogRecord(
                    action = "إلغاء بيع",
                    actor = "المدير",
                    details = "تم إلغاء فاتورة البيع #${sale.id} وإرجاع (${sale.quantity}) حبة لمخزون (${sale.productName})",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun updateSale(sale: SaleTransaction) {
        dao.updateSale(sale)
    }

    // --- Purchase Goods / Restock ---
    suspend fun recordPurchase(
        supplierName: String,
        category: String,
        modelName: String,
        color: String,
        size: String,
        unit: String,
        unitCount: Double,
        buyPricePerUnit: Double,
        sellPricePerUnit: Double,
        paidAmount: Double,
        invoiceNumber: String,
        paymentMethod: String,
        barcode: String,
        notes: String,
        colorsCount: Int,
        colorDetails: String,
        cashPaidPart: Double = 0.0,
        transferPaidPart: Double = 0.0,
        exchangeAccount: ExchangeAccount? = null
    ): Long {
        // Strict accounting validations
        if (unitCount <= 0) {
            throw IllegalArgumentException("يرجى إدخال كمية شراء صحيحة أكبر من الصفر.")
        }
        if (buyPricePerUnit <= 0) {
            throw IllegalArgumentException("سعر الشراء يجب أن يكون أكبر من الصفر.")
        }
        if (sellPricePerUnit <= 0) {
            throw IllegalArgumentException("سعر البيع المقترح يجب أن يكون أكبر من الصفر.")
        }
        if (paidAmount < 0) {
            throw IllegalArgumentException("المبلغ المدفوع لا يمكن أن يكون سالباً.")
        }
        val totalCost = buyPricePerUnit * unitCount
        if (paidAmount > totalCost) {
            throw IllegalArgumentException("المبلغ المدفوع (${FormatUtils.formatCurrency(paidAmount)}) لا يمكن أن يتجاوز إجمالي الفاتورة (${FormatUtils.formatCurrency(totalCost)}).")
        }

        if (paymentMethod == "تحويل عبر الصرافة" || paymentMethod == "تحويل صرافة") {
            if (exchangeAccount == null) {
                throw IllegalArgumentException("يجب اختيار حساب الصراف أو المحفظة المدفوع منها قبل تأكيد الشراء.")
            }
        }
        if (paymentMethod == "نصف كاش ونصف حوالة") {
            if (exchangeAccount == null) {
                throw IllegalArgumentException("يجب اختيار حساب الصراف أو المحفظة للدفعة المحولة.")
            }
            if (kotlin.math.abs((cashPaidPart + transferPaidPart) - paidAmount) > 1.0) {
                throw IllegalArgumentException("مجموع الدفع النقدي ($cashPaidPart) والتحويل ($transferPaidPart) يجب أن يعادل المبلغ المدفوع ($paidAmount).")
            }
        }

        return runInTransaction {
            val activeShiftObj = dao.getActiveShift().firstOrNull()
            val shiftId = activeShiftObj?.id ?: 0L
            val now = System.currentTimeMillis()

            val multiplier = ClothingUnits.getMultiplier(unit)
            val totalPieces = (unitCount * multiplier).toInt().coerceAtLeast(1)
            val remainingDebt = totalCost - paidAmount

            val buyPricePerPiece = totalCost / totalPieces
            val sellPricePerPiece = (sellPricePerUnit * unitCount) / totalPieces

            val finalCashPaid = when (paymentMethod) {
                "نقداً من الصندوق", "نقداً (كاش)" -> paidAmount
                "نصف كاش ونصف حوالة" -> cashPaidPart
                else -> 0.0
            }
            val finalTransferPaid = when (paymentMethod) {
                "تحويل عبر الصرافة", "تحويل صرافة" -> paidAmount
                "نصف كاش ونصف حوالة" -> transferPaidPart
                else -> 0.0
            }

            val purchase = PurchaseRecord(
                supplierName = supplierName.ifBlank { "مورد عام" },
                category = category,
                modelName = modelName,
                color = color,
                size = size,
                quantity = totalPieces,
                unitBuyPrice = buyPricePerPiece,
                unitSellPrice = sellPricePerPiece,
                totalCost = totalCost,
                paidAmount = paidAmount,
                remainingDebt = remainingDebt,
                invoiceNumber = invoiceNumber,
                paymentMethod = paymentMethod,
                cashPaid = finalCashPaid,
                transferPaid = finalTransferPaid,
                exchangeAccountId = exchangeAccount?.id ?: 0L,
                exchangeAccountName = exchangeAccount?.name ?: "",
                timestamp = now,
                unit = unit,
                unitCount = unitCount,
                colorsCount = colorsCount,
                notes = notes
            )
            val purchaseId = dao.insertPurchase(purchase)

            // Add or update product stock atomically
            val allProds = dao.getAllProducts().firstOrNull() ?: emptyList()
            val existing = allProds.find {
                it.modelName.trim().equals(modelName.trim(), ignoreCase = true) &&
                        it.color.trim().equals(color.trim(), ignoreCase = true) &&
                        it.size.trim().equals(size.trim(), ignoreCase = true)
            }

            if (existing != null) {
                val updated = existing.copy(
                    quantity = existing.quantity + totalPieces,
                    buyPrice = buyPricePerPiece,
                    sellPrice = sellPricePerPiece,
                    supplierName = supplierName.ifBlank { existing.supplierName },
                    barcode = barcode.ifBlank { existing.barcode },
                    notes = if (notes.isNotBlank()) notes else existing.notes,
                    unit = unit,
                    colorsCount = colorsCount,
                    colorDetails = colorDetails
                )
                dao.updateProduct(updated)
            } else {
                val newProduct = ProductItem(
                    category = category,
                    modelName = modelName,
                    color = color,
                    size = size,
                    quantity = totalPieces,
                    buyPrice = buyPricePerPiece,
                    sellPrice = sellPricePerPiece,
                    supplierName = supplierName.ifBlank { "مورد عام" },
                    purchaseDate = now,
                    invoiceNumber = invoiceNumber,
                    barcode = barcode,
                    notes = notes,
                    unit = unit,
                    colorsCount = colorsCount,
                    colorDetails = colorDetails
                )
                dao.insertProduct(newProduct)
            }

            val unitDesc = if (unit == ClothingUnits.PIECE) "$totalPieces حبه" else "${unitCount.toInt()} $unit ($totalPieces حبه)"

            // 1. If cash paid -> Deduct from cash drawer
            if (finalCashPaid > 0) {
                dao.insertCashMovement(
                    CashDrawerMovement(
                        shiftId = shiftId,
                        type = "PURCHASE_PAID",
                        amount = finalCashPaid,
                        isIncome = false,
                        title = "شراء بضاعة: $modelName ($unitDesc)",
                        details = "المورد: $supplierName | فاتورة: $invoiceNumber${if (finalTransferPaid > 0) " (جزء نقدي)" else ""}",
                        timestamp = now,
                        referencePurchaseId = purchaseId
                    )
                )
            }

            // 2. If transfer paid -> Deduct from exchange account
            if (finalTransferPaid > 0 && exchangeAccount != null) {
                dao.updateExchangeBalance(exchangeAccount.id, -finalTransferPaid)
                dao.insertExchangeTransfer(
                    ExchangeTransfer(
                        accountId = exchangeAccount.id,
                        accountName = exchangeAccount.name,
                        type = "مشتريات تحويل",
                        amount = finalTransferPaid,
                        isCredit = false,
                        title = "مشتريات تحويل: $modelName ($unitDesc)",
                        referenceNumber = invoiceNumber,
                        notes = "فاتورة شراء #${purchaseId} | المورد: $supplierName",
                        timestamp = now
                    )
                )
            }

            // 3. If remaining debt -> Record supplier debt
            if (remainingDebt > 0) {
                dao.insertDebt(
                    DebtRecord(
                        type = "SUPPLIER",
                        partyName = supplierName.ifBlank { "مورد" },
                        phone = "",
                        originalAmount = totalCost,
                        paidAmount = paidAmount,
                        remainingAmount = remainingDebt,
                        details = "متبقي شراء بضاعة: $modelName ($unitDesc) فاتورة #$invoiceNumber",
                        referencePurchaseId = purchaseId,
                        dueDate = now + 86400000L * 30,
                        createdAt = now,
                        isSettled = false
                    )
                )
            }

            dao.insertAuditLog(
                AuditLogRecord(
                    action = "تسجيل مشتريات",
                    actor = "صاحب المحل",
                    details = "فاتورة شراء #${purchaseId}: $modelName ($unitDesc) من $supplierName بإجمالي ${FormatUtils.formatCurrency(totalCost)}",
                    timestamp = now
                )
            )

            purchaseId
        }
    }

    // --- Delete / Reverse Purchase (Safeguarded against sold stock) ---
    suspend fun deletePurchase(purchaseId: Long) {
        runInTransaction {
            val purchase = dao.getPurchaseById(purchaseId) ?: return@runInTransaction

            // 1. Find product and verify stock has not already been sold
            val allProds = dao.getAllProducts().firstOrNull() ?: emptyList()
            val prod = allProds.find {
                it.modelName.trim().equals(purchase.modelName.trim(), ignoreCase = true) &&
                        it.color.trim().equals(purchase.color.trim(), ignoreCase = true) &&
                        it.size.trim().equals(purchase.size.trim(), ignoreCase = true)
            }
            if (prod != null) {
                if (prod.quantity < purchase.quantity) {
                    throw IllegalStateException(
                        "لا يمكن حذف فاتورة الشراء هذه لأن جزءاً من البضاعة تم بيعه بالفعل! " +
                        "(المتبقي في المخزن ${prod.quantity} حبة بينما الفاتورة تحتوي على ${purchase.quantity} حبة). " +
                        "يرجى تسوية المخزون عبر الجرد بدلاً من حذف الفاتورة."
                    )
                }
                val newQty = prod.quantity - purchase.quantity
                dao.updateStock(prod.id, newQty, System.currentTimeMillis())
            }

            // 2. Refund cash movements
            dao.deleteCashMovementsForPurchase(purchaseId)

            // 3. Refund exchange account if transfer was used
            if (purchase.transferPaid > 0 && purchase.exchangeAccountId > 0) {
                dao.updateExchangeBalance(purchase.exchangeAccountId, purchase.transferPaid)
            }

            // 4. Delete supplier debt if linked
            val debt = dao.getDebtByPurchaseId(purchaseId)
            if (debt != null) {
                dao.deleteDebt(debt.id)
            }

            // 5. Delete purchase record
            dao.deletePurchase(purchaseId)

            dao.insertAuditLog(
                AuditLogRecord(
                    action = "إلغاء مشتريات",
                    actor = "المدير",
                    details = "تم إلغاء فاتورة الشراء #${purchase.id} للمنتج (${purchase.modelName}) واسترداد المبالغ المدفوعة.",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun updatePurchase(purchase: PurchaseRecord) {
        dao.updatePurchase(purchase)
    }

    // --- Expenses ---
    suspend fun recordExpense(
        category: String,
        amount: Double,
        notes: String,
        receiptNumber: String,
        paidFromCash: Boolean,
        exchangeAccount: ExchangeAccount? = null
    ): Long {
        require(amount > 0) { "يجب أن يكون مبلغ المصروف أكبر من الصفر." }
        require(paidFromCash || exchangeAccount != null) { "يجب اختيار حساب الصراف عند دفع المصروف خارج الصندوق." }
        val activeShiftObj = dao.getActiveShift().firstOrNull()
        val shiftId = activeShiftObj?.id ?: 0L
        val now = System.currentTimeMillis()

        val expense = ExpenseRecord(
            category = category,
            amount = amount,
            notes = notes,
            receiptNumber = receiptNumber,
            timestamp = now,
            paidFromCash = paidFromCash,
            exchangeAccountId = exchangeAccount?.id ?: 0L,
            exchangeAccountName = exchangeAccount?.name ?: "",
            shiftId = shiftId
        )
        val id = dao.insertExpense(expense)

        if (paidFromCash) {
            dao.insertCashMovement(
                CashDrawerMovement(
                    shiftId = shiftId,
                    type = "EXPENSE_PAID",
                    amount = amount,
                    isIncome = false,
                    title = "مصروف: $category",
                    details = notes.ifBlank { "وصل: $receiptNumber" },
                    timestamp = now,
                    referenceExpenseId = id
                )
            )
        } else if (exchangeAccount != null) {
            dao.updateExchangeBalance(exchangeAccount.id, -amount)
            dao.insertExchangeTransfer(
                ExchangeTransfer(
                    accountId = exchangeAccount.id,
                    accountName = exchangeAccount.name,
                    type = "سحب مصروف",
                    amount = amount,
                    isCredit = false,
                    title = "مصروف من الصراف: $category",
                    referenceNumber = receiptNumber,
                    notes = notes,
                    timestamp = now
                )
            )
        }
        return id
    }

    suspend fun updateExpense(expense: ExpenseRecord) {
        dao.updateExpense(expense)
    }

    suspend fun deleteExpense(id: Long) {
        dao.deleteCashMovementsForExpense(id)
        val exp = dao.getExpenseById(id)
        if (exp != null && exp.exchangeAccountId > 0) {
            dao.updateExchangeBalance(exp.exchangeAccountId, exp.amount)
        }
        dao.deleteExpense(id)
    }

    // --- Employees & Payroll ---
    suspend fun saveEmployee(employee: Employee): Long {
        return if (employee.id == 0L) {
            dao.insertEmployee(employee)
        } else {
            dao.updateEmployee(employee)
            employee.id
        }
    }

    suspend fun deleteEmployee(employeeId: Long) {
        dao.deleteEmployee(employeeId)
    }

    suspend fun deleteEmployeeTransaction(txId: Long) {
        dao.deleteCashMovementsForEmployeeTx(txId)
        dao.deleteEmployeeTransaction(txId)
    }

    suspend fun recordEmployeeTransaction(
        employeeId: Long,
        employeeName: String,
        type: String,
        amount: Double,
        notes: String,
        paidFromCashDrawer: Boolean,
        exchangeAccount: ExchangeAccount? = null
    ): Long {
        require(amount > 0) { "يجب أن يكون مبلغ حركة الموظف أكبر من الصفر." }
        require(paidFromCashDrawer || exchangeAccount != null || type == "خصم غياب") { "يجب اختيار مصدر الدفع لحركة الموظف." }
        val activeShiftObj = dao.getActiveShift().firstOrNull()
        val shiftId = activeShiftObj?.id ?: 0L
        val now = System.currentTimeMillis()

        val tx = EmployeeTransaction(
            employeeId = employeeId,
            employeeName = employeeName,
            type = type,
            amount = amount,
            notes = notes,
            timestamp = now,
            shiftId = shiftId,
            paidFromCash = paidFromCashDrawer,
            exchangeAccountId = exchangeAccount?.id ?: 0L,
            exchangeAccountName = exchangeAccount?.name ?: ""
        )
        val id = dao.insertEmployeeTransaction(tx)

        if (paidFromCashDrawer && (type == "سلفة" || type == "صرف راتب" || type == "مكافأة")) {
            dao.insertCashMovement(
                CashDrawerMovement(
                    shiftId = shiftId,
                    type = "SALARY_PAID",
                    amount = amount,
                    isIncome = false,
                    title = "$type للعامل: $employeeName",
                    details = notes,
                    timestamp = now,
                    referenceEmployeeTxId = id
                )
            )
        } else if (exchangeAccount != null && (type == "سلفة" || type == "صرف راتب" || type == "مكافأة")) {
            dao.updateExchangeBalance(exchangeAccount.id, -amount)
            dao.insertExchangeTransfer(
                ExchangeTransfer(
                    accountId = exchangeAccount.id,
                    accountName = exchangeAccount.name,
                    type = type,
                    amount = amount,
                    isCredit = false,
                    title = "$type للعامل: $employeeName",
                    notes = notes,
                    timestamp = now
                )
            )
        }
        return id
    }

    // --- Debts & Collections ---
    suspend fun recordDebtPayment(
        debt: DebtRecord,
        paymentAmount: Double,
        notes: String,
        paymentMethod: String = "نقداً", // "نقداً" or "صرافة/محفظة"
        exchangeAccount: ExchangeAccount? = null
    ): Long {
        require(paymentAmount > 0) { "يجب أن يكون مبلغ السداد أكبر من الصفر." }
        require(paymentAmount <= debt.remainingAmount + 0.001) { "مبلغ السداد أكبر من الدين المتبقي." }
        require(paymentMethod == "نقداً" || exchangeAccount != null) { "يجب اختيار حساب الصراف عند السداد بالتحويل." }
        val activeShiftObj = dao.getActiveShift().firstOrNull()
        val shiftId = activeShiftObj?.id ?: 0L
        val now = System.currentTimeMillis()

        val newPaid = debt.paidAmount + paymentAmount
        val newRemaining = (debt.originalAmount - newPaid).coerceAtLeast(0.0)
        val isSettled = newRemaining <= 0.001

        dao.updateDebt(
            debt.copy(
                paidAmount = newPaid,
                remainingAmount = newRemaining,
                isSettled = isSettled
            )
        )

        val payment = DebtPayment(
            debtId = debt.id,
            partyName = debt.partyName,
            amount = paymentAmount,
            timestamp = now,
            notes = notes,
            shiftId = shiftId,
            paymentMethod = paymentMethod,
            exchangeAccountId = exchangeAccount?.id ?: 0L
        )
        val paymentId = dao.insertDebtPayment(payment)

        if (debt.type == "CUSTOMER") {
            if (paymentMethod == "نقداً") {
                dao.insertCashMovement(
                    CashDrawerMovement(
                        shiftId = shiftId,
                        type = "DEBT_COLLECTED",
                        amount = paymentAmount,
                        isIncome = true,
                        title = "تحصيل دين من الزبون: ${debt.partyName}",
                        details = "المتبقي بعد الدفع: ${newRemaining.toLong()} ريال",
                        timestamp = now
                    )
                )
            } else if (exchangeAccount != null) {
                dao.updateExchangeBalance(exchangeAccount.id, paymentAmount)
                dao.insertExchangeTransfer(
                    ExchangeTransfer(
                        accountId = exchangeAccount.id,
                        accountName = exchangeAccount.name,
                        type = "تحصيل دين",
                        amount = paymentAmount,
                        isCredit = true,
                        title = "تحصيل دين عبر الصراف: ${debt.partyName}",
                        notes = notes,
                        timestamp = now
                    )
                )
            }
        } else {
            // Supplier debt payment
            if (paymentMethod == "نقداً") {
                dao.insertCashMovement(
                    CashDrawerMovement(
                        shiftId = shiftId,
                        type = "SUPPLIER_PAID",
                        amount = paymentAmount,
                        isIncome = false,
                        title = "تسديد دين للمورد: ${debt.partyName}",
                        details = "المتبقي للمورد: ${newRemaining.toLong()} ريال",
                        timestamp = now
                    )
                )
            } else if (exchangeAccount != null) {
                dao.updateExchangeBalance(exchangeAccount.id, -paymentAmount)
                dao.insertExchangeTransfer(
                    ExchangeTransfer(
                        accountId = exchangeAccount.id,
                        accountName = exchangeAccount.name,
                        type = "تسديد دين مورد",
                        amount = paymentAmount,
                        isCredit = false,
                        title = "تسديد دين للمورد عبر الصراف: ${debt.partyName}",
                        notes = notes,
                        timestamp = now
                    )
                )
            }
        }

        return paymentId
    }

    suspend fun addNewDebt(debt: DebtRecord): Long = dao.insertDebt(debt)
    suspend fun deleteDebt(id: Long) = dao.deleteDebt(id)

    // --- Parties Management (Customers & Suppliers) ---
    suspend fun saveParty(party: PartyAccount, initialDebt: Double = 0.0, debtNotes: String = ""): Long {
        val now = System.currentTimeMillis()
        val partyId = if (party.id == 0L) {
            dao.insertParty(party.copy(initialBalance = initialDebt))
        } else {
            dao.updateParty(party)
            party.id
        }

        if (party.id == 0L && initialDebt > 0) {
            val isCustomer = party.type == "CUSTOMER"
            val debt = DebtRecord(
                type = if (isCustomer) "CUSTOMER" else "SUPPLIER",
                partyName = party.name,
                phone = party.phone,
                originalAmount = initialDebt,
                paidAmount = 0.0,
                remainingAmount = initialDebt,
                details = debtNotes.ifBlank {
                    if (isCustomer) "رصيد افتتاحي (عليه زلط سابقة عند فتح الحساب)"
                    else "رصيد افتتاحي (عليا زلط للمورد عند فتح الحساب)"
                },
                dueDate = now + if (isCustomer) 86400000L * 15 else 86400000L * 30,
                createdAt = now,
                isSettled = false
            )
            dao.insertDebt(debt)
        }
        return partyId
    }

    suspend fun deleteParty(id: Long) {
        dao.deleteParty(id)
    }

    suspend fun recordPartyPayment(
        partyName: String,
        partyType: String,
        totalPayment: Double,
        notes: String,
        paymentMethod: String = "نقداً",
        exchangeAccount: ExchangeAccount? = null
    ): Boolean {
        if (totalPayment <= 0) return false
        val allDebtsList = dao.getAllDebts().firstOrNull() ?: emptyList()
        val partyDebts = allDebtsList.filter {
            it.partyName.trim().equals(partyName.trim(), ignoreCase = true) &&
            it.type == partyType &&
            !it.isSettled
        }.sortedBy { it.createdAt }

        var remainingToApply = totalPayment
        for (debt in partyDebts) {
            if (remainingToApply <= 0.001) break
            val paymentForThisDebt = minOf(remainingToApply, debt.remainingAmount)
            recordDebtPayment(
                debt = debt,
                paymentAmount = paymentForThisDebt,
                notes = notes,
                paymentMethod = paymentMethod,
                exchangeAccount = exchangeAccount
            )
            remainingToApply -= paymentForThisDebt
        }
        return true
    }

    // --- Exchange Accounts & Wallets Management ---
    suspend fun saveExchangeAccount(account: ExchangeAccount): Long {
        return if (account.id == 0L) {
            dao.insertExchangeAccount(account)
        } else {
            dao.updateExchangeAccount(account)
            account.id
        }
    }

    suspend fun deleteExchangeAccount(id: Long) {
        dao.deleteExchangeAccount(id)
    }

    suspend fun transferBetweenCashAndExchange(
        accountId: Long,
        amount: Double,
        fromCashToExchange: Boolean,
        notes: String
    ) {
        require(amount > 0) { "يجب أن يكون مبلغ التحويل أكبر من الصفر." }
        val account = dao.getExchangeAccountById(accountId) ?: throw IllegalArgumentException("حساب الصراف غير موجود.")
        if (!fromCashToExchange) {
            require(account.balance + 0.001 >= amount) { "الرصيد المتوفر في حساب الصراف لا يكفي لهذا السحب." }
        }
        val activeShiftObj = dao.getActiveShift().firstOrNull()
        val shiftId = activeShiftObj?.id ?: 0L
        val now = System.currentTimeMillis()

        if (fromCashToExchange) {
            // Deduct from cash drawer -> Add to exchange account
            dao.insertCashMovement(
                CashDrawerMovement(
                    shiftId = shiftId,
                    type = "TRANSFER_TO_EXCHANGE",
                    amount = amount,
                    isIncome = false,
                    title = "إيداع نقدي لحساب الصراف: ${account.name}",
                    details = notes,
                    timestamp = now
                )
            )
            dao.updateExchangeBalance(accountId, amount)
            dao.insertExchangeTransfer(
                ExchangeTransfer(
                    accountId = accountId,
                    accountName = account.name,
                    type = "إيداع من الكاش",
                    amount = amount,
                    isCredit = true,
                    title = "إيداع نقدي من الصندوق",
                    notes = notes,
                    timestamp = now
                )
            )
        } else {
            // Withdraw from exchange account -> Add to cash drawer
            dao.insertCashMovement(
                CashDrawerMovement(
                    shiftId = shiftId,
                    type = "CAPITAL_DEPOSIT",
                    amount = amount,
                    isIncome = true,
                    title = "سحب كاش من الصراف: ${account.name}",
                    details = notes,
                    timestamp = now
                )
            )
            dao.updateExchangeBalance(accountId, -amount)
            dao.insertExchangeTransfer(
                ExchangeTransfer(
                    accountId = accountId,
                    accountName = account.name,
                    type = "سحب للكاش",
                    amount = amount,
                    isCredit = false,
                    title = "سحب نقدي إلى الصندوق",
                    notes = notes,
                    timestamp = now
                )
            )
        }
    }

    // --- Shifts & Drawer ---
    suspend fun openShift(cashierName: String, openingBalance: Double, notes: String): Long {
        val now = System.currentTimeMillis()
        val shift = CashDrawerShift(
            openedAt = now,
            closedAt = null,
            openingBalance = openingBalance,
            closingBalanceActual = null,
            isOpen = true,
            cashierName = cashierName.ifBlank { "الكاشير" },
            notes = notes
        )
        val shiftId = dao.insertShift(shift)

        dao.insertCashMovement(
            CashDrawerMovement(
                shiftId = shiftId,
                type = "CAPITAL_DEPOSIT",
                amount = openingBalance,
                isIncome = true,
                title = "رصيد افتتاح الوردية",
                details = notes,
                timestamp = now
            )
        )
        return shiftId
    }

    suspend fun closeShift(shift: CashDrawerShift, actualCashInDrawer: Double, notes: String) {
        val now = System.currentTimeMillis()
        val updated = shift.copy(
            closedAt = now,
            closingBalanceActual = actualCashInDrawer,
            isOpen = false,
            notes = if (notes.isNotBlank()) "${shift.notes} | إغلاق: $notes" else shift.notes
        )
        dao.updateShift(updated)
    }

    suspend fun recordOwnerWithdrawal(amount: Double, reason: String) {
        require(amount > 0) { "يجب أن يكون مبلغ السحب أكبر من الصفر." }
        val activeShiftObj = dao.getActiveShift().firstOrNull()
        val shiftId = activeShiftObj?.id ?: 0L
        val now = System.currentTimeMillis()

        dao.insertCashMovement(
            CashDrawerMovement(
                shiftId = shiftId,
                type = "WITHDRAWAL",
                amount = amount,
                isIncome = false,
                title = "سحب صاحب المحل",
                details = reason.ifBlank { "سحب شخصي" },
                timestamp = now
            )
        )
    }

    suspend fun recordManualDeposit(amount: Double, reason: String) {
        require(amount > 0) { "يجب أن يكون مبلغ الإيداع أكبر من الصفر." }
        val activeShiftObj = dao.getActiveShift().firstOrNull()
        val shiftId = activeShiftObj?.id ?: 0L
        val now = System.currentTimeMillis()

        dao.insertCashMovement(
            CashDrawerMovement(
                shiftId = shiftId,
                type = "CAPITAL_DEPOSIT",
                amount = amount,
                isIncome = true,
                title = "إيداع نقدي بالصندوق",
                details = reason.ifBlank { "زيادة رأس مال / سيولة" },
                timestamp = now
            )
        )
    }

    // --- Reset / Clear Data ---
    suspend fun clearTransactionsOnly() {
        dao.deleteAllSales()
        dao.deleteAllPurchases()
        dao.deleteAllExpenses()
        dao.deleteAllEmployeeTransactions()
        dao.deleteAllDebts()
        dao.deleteAllDebtPayments()
        dao.deleteAllCashMovements()
        dao.deleteAllShifts()
        dao.deleteAllExchangeTransfers()
    }

    suspend fun clearEverythingCompletely() {
        clearTransactionsOnly()
        dao.deleteAllProducts()
        dao.deleteAllEmployees()
        dao.deleteAllExchangeAccounts()
        dao.deleteAllParties()
        dao.deleteAllAuditLogs()
        dao.deleteAllStoreSettings()
    }

    suspend fun executeStoreFullAudit(
        actualDrawerCash: Double,
        exchangeBalances: Map<Long, Double>,
        resetTransactions: Boolean,
        notes: String
    ) {
        val now = System.currentTimeMillis()
        if (resetTransactions) {
            clearTransactionsOnly()
        } else {
            val currentActiveShift = dao.getActiveShift().firstOrNull()
            if (currentActiveShift != null) {
                closeShift(currentActiveShift, actualDrawerCash, "إغلاق قبل اعتماد الجرد")
            }
        }

        // Open new active shift with audited actual cash
        val newShiftId = dao.insertShift(
            CashDrawerShift(
                openedAt = now,
                closedAt = null,
                openingBalance = actualDrawerCash,
                closingBalanceActual = null,
                isOpen = true,
                cashierName = "صاحب المحل",
                notes = "اعتماد جرد المحل: ${notes.ifBlank { "بداية الأرصدة الفعلية" }}"
            )
        )

        dao.insertCashMovement(
            CashDrawerMovement(
                shiftId = newShiftId,
                type = "CAPITAL_DEPOSIT",
                amount = actualDrawerCash,
                isIncome = true,
                title = "رصيد الصندوق المعتمد من الجرد",
                details = notes,
                timestamp = now
            )
        )

        // Update all exchange account balances
        exchangeBalances.forEach { (accountId, balance) ->
            dao.setExchangeBalance(accountId, balance)
            val acc = dao.getExchangeAccountById(accountId)
            dao.insertExchangeTransfer(
                ExchangeTransfer(
                    accountId = accountId,
                    accountName = acc?.name ?: "حساب صراف",
                    type = "تعديل رصيد",
                    amount = balance,
                    isCredit = true,
                    title = "رصيد معتمد من جرد المحل",
                    referenceNumber = "AUDIT",
                    notes = notes,
                    timestamp = now
                )
            )
        }
    }

    suspend fun restoreFullData(
        productsJson: JSONArray?,
        salesJson: JSONArray?,
        purchasesJson: JSONArray?,
        expensesJson: JSONArray?,
        employeesJson: JSONArray?,
        employeeTransactionsJson: JSONArray?,
        accountsJson: JSONArray?,
        exchangeTransfersJson: JSONArray?,
        partiesJson: JSONArray?,
        debtsJson: JSONArray?,
        debtPaymentsJson: JSONArray?,
        shiftsJson: JSONArray?,
        cashMovementsJson: JSONArray?,
        auditLogsJson: JSONArray?,
        settingsJson: JSONObject?
    ) {
        runInTransaction {
            clearEverythingCompletely()
            fun JSONObject.longOrNull(key: String): Long? = if (isNull(key)) null else optLong(key)
            fun JSONObject.doubleOrNull(key: String): Double? = if (isNull(key)) null else optDouble(key)

            productsJson?.let { array -> for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                dao.insertProduct(ProductItem(
                    id = o.optLong("id", 0L), category = o.optString("category", "أخرى"), modelName = o.optString("modelName", "صنف"), color = o.optString("color", "عام"), size = o.optString("size", "حر"),
                    quantity = o.optInt("quantity", 0), buyPrice = o.optDouble("buyPrice", 0.0), sellPrice = o.optDouble("sellPrice", 0.0), supplierName = o.optString("supplierName", ""), purchaseDate = o.optLong("purchaseDate", System.currentTimeMillis()), invoiceNumber = o.optString("invoiceNumber", ""), barcode = o.optString("barcode", ""), notes = o.optString("notes", ""), lastSoldTimestamp = o.optLong("lastSoldTimestamp", 0L), unit = o.optString("unit", ClothingUnits.PIECE), colorsCount = o.optInt("colorsCount", 1), colorDetails = o.optString("colorDetails", "")
                ))
            }}
            accountsJson?.let { array -> for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                dao.insertExchangeAccount(ExchangeAccount(id = o.optLong("id", 0L), name = o.optString("name", "حساب"), accountNumber = o.optString("accountNumber", ""), phone = o.optString("phone", ""), balance = o.optDouble("balance", 0.0), type = o.optString("type", "صراف"), notes = o.optString("notes", ""), createdAt = o.optLong("createdAt", System.currentTimeMillis())))
            }}
            partiesJson?.let { array -> for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                dao.insertParty(PartyAccount(id = o.optLong("id", 0L), type = o.optString("type", "CUSTOMER"), name = o.optString("name", ""), phone = o.optString("phone", ""), address = o.optString("address", ""), notes = o.optString("notes", ""), initialBalance = o.optDouble("initialBalance", 0.0), createdAt = o.optLong("createdAt", System.currentTimeMillis())))
            }}
            employeesJson?.let { array -> for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                dao.insertEmployee(Employee(id = o.optLong("id", 0L), name = o.optString("name", ""), phone = o.optString("phone", ""), role = o.optString("role", "بائع"), startDate = o.optLong("startDate", System.currentTimeMillis()), baseSalary = o.optDouble("baseSalary", 0.0), salaryType = o.optString("salaryType", "شهري"), commissionPercent = o.optDouble("commissionPercent", 0.0), active = o.optBoolean("active", true)))
            }}
            shiftsJson?.let { array -> for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                dao.insertShift(CashDrawerShift(id = o.optLong("id", 0L), openedAt = o.optLong("openedAt", System.currentTimeMillis()), closedAt = o.longOrNull("closedAt"), openingBalance = o.optDouble("openingBalance", 0.0), closingBalanceActual = o.doubleOrNull("closingBalanceActual"), isOpen = o.optBoolean("isOpen", true), cashierName = o.optString("cashierName", "صاحب المحل"), notes = o.optString("notes", "")))
            }}
            salesJson?.let { array -> for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                dao.insertSale(SaleTransaction(id = o.optLong("id", 0L), productId = o.optLong("productId", 0L), productName = o.optString("productName", ""), category = o.optString("category", ""), color = o.optString("color", ""), size = o.optString("size", ""), quantity = o.optInt("quantity", 1), unitBuyPrice = o.optDouble("unitBuyPrice", 0.0), unitSellPrice = o.optDouble("unitSellPrice", 0.0), discount = o.optDouble("discount", 0.0), totalAmount = o.optDouble("totalAmount", 0.0), profit = o.optDouble("profit", 0.0), customerName = o.optString("customerName", "زبون نقدي"), customerPhone = o.optString("customerPhone", ""), paymentType = o.optString("paymentType", "نقداً"), cashAmount = o.optDouble("cashAmount", 0.0), transferAmount = o.optDouble("transferAmount", 0.0), exchangeAccountId = o.optLong("exchangeAccountId", 0L), exchangeAccountName = o.optString("exchangeAccountName", ""), transferRef = o.optString("transferRef", ""), employeeName = o.optString("employeeName", "صاحب المحل"), employeeId = o.optLong("employeeId", 0L), timestamp = o.optLong("timestamp", System.currentTimeMillis()), shiftId = o.optLong("shiftId", 0L), unit = o.optString("unit", ClothingUnits.PIECE), invoiceNumber = o.optString("invoiceNumber", "")))
            }}
            purchasesJson?.let { array -> for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                dao.insertPurchase(PurchaseRecord(id = o.optLong("id", 0L), supplierName = o.optString("supplierName", "مورد"), category = o.optString("category", ""), modelName = o.optString("modelName", ""), color = o.optString("color", ""), size = o.optString("size", ""), quantity = o.optInt("quantity", 1), unitBuyPrice = o.optDouble("unitBuyPrice", 0.0), unitSellPrice = o.optDouble("unitSellPrice", 0.0), totalCost = o.optDouble("totalCost", 0.0), paidAmount = o.optDouble("paidAmount", 0.0), remainingDebt = o.optDouble("remainingDebt", 0.0), invoiceNumber = o.optString("invoiceNumber", ""), paymentMethod = o.optString("paymentMethod", "نقداً من الصندوق"), cashPaid = o.optDouble("cashPaid", 0.0), transferPaid = o.optDouble("transferPaid", 0.0), exchangeAccountId = o.optLong("exchangeAccountId", 0L), exchangeAccountName = o.optString("exchangeAccountName", ""), timestamp = o.optLong("timestamp", System.currentTimeMillis()), unit = o.optString("unit", ClothingUnits.PIECE), unitCount = o.optDouble("unitCount", 1.0), colorsCount = o.optInt("colorsCount", 1), notes = o.optString("notes", "")))
            }}
            expensesJson?.let { array -> for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                dao.insertExpense(ExpenseRecord(id = o.optLong("id", 0L), category = o.optString("category", "مصروف"), amount = o.optDouble("amount", 0.0), notes = o.optString("notes", ""), receiptNumber = o.optString("receiptNumber", ""), timestamp = o.optLong("timestamp", System.currentTimeMillis()), paidFromCash = o.optBoolean("paidFromCash", true), exchangeAccountId = o.optLong("exchangeAccountId", 0L), exchangeAccountName = o.optString("exchangeAccountName", ""), shiftId = o.optLong("shiftId", 0L)))
            }}
            employeeTransactionsJson?.let { array -> for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                dao.insertEmployeeTransaction(EmployeeTransaction(id = o.optLong("id", 0L), employeeId = o.optLong("employeeId", 0L), employeeName = o.optString("employeeName", ""), type = o.optString("type", ""), amount = o.optDouble("amount", 0.0), notes = o.optString("notes", ""), timestamp = o.optLong("timestamp", System.currentTimeMillis()), shiftId = o.optLong("shiftId", 0L), paidFromCash = o.optBoolean("paidFromCash", true), exchangeAccountId = o.optLong("exchangeAccountId", 0L), exchangeAccountName = o.optString("exchangeAccountName", "")))
            }}
            debtsJson?.let { array -> for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                dao.insertDebt(DebtRecord(id = o.optLong("id", 0L), type = o.optString("type", "CUSTOMER"), partyName = o.optString("partyName", ""), phone = o.optString("phone", ""), originalAmount = o.optDouble("originalAmount", 0.0), paidAmount = o.optDouble("paidAmount", 0.0), remainingAmount = o.optDouble("remainingAmount", 0.0), details = o.optString("details", ""), referenceSaleId = o.longOrNull("referenceSaleId"), referencePurchaseId = o.longOrNull("referencePurchaseId"), dueDate = o.optLong("dueDate", 0L), createdAt = o.optLong("createdAt", System.currentTimeMillis()), isSettled = o.optBoolean("isSettled", false)))
            }}
            debtPaymentsJson?.let { array -> for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                dao.insertDebtPayment(DebtPayment(id = o.optLong("id", 0L), debtId = o.optLong("debtId", 0L), partyName = o.optString("partyName", ""), amount = o.optDouble("amount", 0.0), timestamp = o.optLong("timestamp", System.currentTimeMillis()), notes = o.optString("notes", ""), shiftId = o.optLong("shiftId", 0L), paymentMethod = o.optString("paymentMethod", "نقداً"), exchangeAccountId = o.optLong("exchangeAccountId", 0L)))
            }}
            cashMovementsJson?.let { array -> for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                dao.insertCashMovement(CashDrawerMovement(id = o.optLong("id", 0L), shiftId = o.optLong("shiftId", 0L), type = o.optString("type", ""), amount = o.optDouble("amount", 0.0), isIncome = o.optBoolean("isIncome", true), title = o.optString("title", ""), details = o.optString("details", ""), timestamp = o.optLong("timestamp", System.currentTimeMillis()), referenceSaleId = o.longOrNull("referenceSaleId"), referencePurchaseId = o.longOrNull("referencePurchaseId"), referenceExpenseId = o.longOrNull("referenceExpenseId"), referenceEmployeeTxId = o.longOrNull("referenceEmployeeTxId")))
            }}
            exchangeTransfersJson?.let { array -> for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                dao.insertExchangeTransfer(ExchangeTransfer(id = o.optLong("id", 0L), accountId = o.optLong("accountId", 0L), accountName = o.optString("accountName", ""), type = o.optString("type", ""), amount = o.optDouble("amount", 0.0), isCredit = o.optBoolean("isCredit", true), title = o.optString("title", ""), referenceNumber = o.optString("referenceNumber", ""), notes = o.optString("notes", ""), timestamp = o.optLong("timestamp", System.currentTimeMillis())))
            }}
            auditLogsJson?.let { array -> for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                dao.insertAuditLog(AuditLogRecord(id = o.optLong("id", 0L), action = o.optString("action", "استعادة"), actor = o.optString("actor", "المدير"), details = o.optString("details", ""), timestamp = o.optLong("timestamp", System.currentTimeMillis())))
            }}
            settingsJson?.let { o -> dao.insertStoreSettings(StoreSettings(id = o.optInt("id", 1), shopNameAr = o.optString("shopNameAr", "محل الأناقة للملابس الرجالية"), shopNameEn = o.optString("shopNameEn", "Al-Anaqah Menswear"), phone = o.optString("phone", ""), address = o.optString("address", ""), currency = o.optString("currency", "ريال"), taxNumber = o.optString("taxNumber", ""), invoiceHeader = o.optString("invoiceHeader", ""), invoiceFooter = o.optString("invoiceFooter", ""), showProfitInReports = o.optBoolean("showProfitInReports", true), managerPin = o.optString("managerPin", ""), isManagerRole = o.optBoolean("isManagerRole", true))) }

            dao.insertAuditLog(AuditLogRecord(action = "استعادة نسخة احتياطية", actor = "المدير", details = "تمت استعادة نسخة كاملة مع الحفاظ على المعرفات والعلاقات", timestamp = System.currentTimeMillis()))
        }
    }
}
