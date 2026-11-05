package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.*
import com.example.data.repository.StoreRepository
import kotlinx.coroutines.flow.firstOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/** Full JSON export/import for every business table in the local database. */
object BackupManager {
    private const val BACKUP_VERSION = 2

    suspend fun createBackupFile(context: Context, repository: StoreRepository): File {
        val root = JSONObject().apply {
            put("app", "HesabiStore")
            put("version", BACKUP_VERSION)
            put("databaseVersion", 7)
            put("timestamp", System.currentTimeMillis())
        }

        root.put("products", JSONArray().also { out ->
            (repository.allProducts.firstOrNull() ?: emptyList()).forEach { p -> out.put(p.toJson()) }
        })
        root.put("sales", JSONArray().also { out ->
            (repository.allSales.firstOrNull() ?: emptyList()).forEach { s -> out.put(s.toJson()) }
        })
        root.put("purchases", JSONArray().also { out ->
            (repository.allPurchases.firstOrNull() ?: emptyList()).forEach { p -> out.put(p.toJson()) }
        })
        root.put("expenses", JSONArray().also { out ->
            (repository.allExpenses.firstOrNull() ?: emptyList()).forEach { e -> out.put(e.toJson()) }
        })
        root.put("employees", JSONArray().also { out ->
            (repository.allEmployees.firstOrNull() ?: emptyList()).forEach { e -> out.put(e.toJson()) }
        })
        root.put("employeeTransactions", JSONArray().also { out ->
            (repository.allEmployeeTransactions.firstOrNull() ?: emptyList()).forEach { t -> out.put(t.toJson()) }
        })
        root.put("debts", JSONArray().also { out ->
            (repository.allDebts.firstOrNull() ?: emptyList()).forEach { d -> out.put(d.toJson()) }
        })
        root.put("debtPayments", JSONArray().also { out ->
            (repository.allDebtPayments.firstOrNull() ?: emptyList()).forEach { p -> out.put(p.toJson()) }
        })
        root.put("shifts", JSONArray().also { out ->
            (repository.allShifts.firstOrNull() ?: emptyList()).forEach { s -> out.put(s.toJson()) }
        })
        root.put("cashMovements", JSONArray().also { out ->
            (repository.allCashMovements.firstOrNull() ?: emptyList()).forEach { m -> out.put(m.toJson()) }
        })
        root.put("exchangeAccounts", JSONArray().also { out ->
            (repository.allExchangeAccounts.firstOrNull() ?: emptyList()).forEach { a -> out.put(a.toJson()) }
        })
        root.put("exchangeTransfers", JSONArray().also { out ->
            (repository.allExchangeTransfers.firstOrNull() ?: emptyList()).forEach { t -> out.put(t.toJson()) }
        })
        root.put("parties", JSONArray().also { out ->
            (repository.allParties.firstOrNull() ?: emptyList()).forEach { p -> out.put(p.toJson()) }
        })
        root.put("auditLogs", JSONArray().also { out ->
            (repository.allAuditLogs.firstOrNull() ?: emptyList()).forEach { l -> out.put(l.toJson()) }
        })
        repository.storeSettings.firstOrNull()?.let { root.put("storeSettings", it.toJson()) }

        val backupDir = File(context.cacheDir, "backups")
        if (!backupDir.exists()) backupDir.mkdirs()
        val file = File(
            backupDir,
            "نسخة_احتياطية_${System.currentTimeMillis()}.json"
        )
        FileOutputStream(file).use { it.write(root.toString(2).toByteArray(Charsets.UTF_8)) }
        return file
    }

    fun shareBackupFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "نسخة احتياطية - تطبيق حسابي للمحل")
                putExtra(Intent.EXTRA_TEXT, "نسخة احتياطية كاملة لبيانات المحل بتاريخ ${FormatUtils.formatDate(System.currentTimeMillis())}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "مشاركة أو حفظ النسخة الاحتياطية"))
        } catch (e: Exception) {
            Toast.makeText(context, "فشل تصدير النسخة الاحتياطية: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    suspend fun restoreFromJson(jsonString: String, repository: StoreRepository): Boolean {
        return try {
            val root = JSONObject(jsonString)
            require(root.optString("app") == "HesabiStore")
            require(root.optInt("version", 1) in 1..BACKUP_VERSION)
            repository.restoreFullData(
                productsJson = root.optJSONArray("products"),
                salesJson = root.optJSONArray("sales"),
                purchasesJson = root.optJSONArray("purchases"),
                expensesJson = root.optJSONArray("expenses"),
                employeesJson = root.optJSONArray("employees"),
                employeeTransactionsJson = root.optJSONArray("employeeTransactions"),
                accountsJson = root.optJSONArray("exchangeAccounts"),
                exchangeTransfersJson = root.optJSONArray("exchangeTransfers"),
                partiesJson = root.optJSONArray("parties"),
                debtsJson = root.optJSONArray("debts"),
                debtPaymentsJson = root.optJSONArray("debtPayments"),
                shiftsJson = root.optJSONArray("shifts"),
                cashMovementsJson = root.optJSONArray("cashMovements"),
                auditLogsJson = root.optJSONArray("auditLogs"),
                settingsJson = root.optJSONObject("storeSettings")
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun ProductItem.toJson() = JSONObject().apply {
        put("id", id); put("category", category); put("modelName", modelName); put("color", color); put("size", size)
        put("quantity", quantity); put("buyPrice", buyPrice); put("sellPrice", sellPrice); put("supplierName", supplierName)
        put("purchaseDate", purchaseDate); put("invoiceNumber", invoiceNumber); put("barcode", barcode); put("notes", notes)
        put("lastSoldTimestamp", lastSoldTimestamp); put("unit", unit); put("colorsCount", colorsCount); put("colorDetails", colorDetails)
    }

    private fun SaleTransaction.toJson() = JSONObject().apply {
        put("id", id); put("productId", productId); put("productName", productName); put("category", category); put("color", color); put("size", size)
        put("quantity", quantity); put("unitBuyPrice", unitBuyPrice); put("unitSellPrice", unitSellPrice); put("discount", discount); put("totalAmount", totalAmount); put("profit", profit)
        put("customerName", customerName); put("customerPhone", customerPhone); put("paymentType", paymentType); put("cashAmount", cashAmount); put("transferAmount", transferAmount)
        put("exchangeAccountId", exchangeAccountId); put("exchangeAccountName", exchangeAccountName); put("transferRef", transferRef); put("employeeName", employeeName); put("employeeId", employeeId); put("timestamp", timestamp); put("shiftId", shiftId); put("unit", unit); put("invoiceNumber", invoiceNumber)
    }

    private fun PurchaseRecord.toJson() = JSONObject().apply {
        put("id", id); put("supplierName", supplierName); put("category", category); put("modelName", modelName); put("color", color); put("size", size); put("quantity", quantity)
        put("unitBuyPrice", unitBuyPrice); put("unitSellPrice", unitSellPrice); put("totalCost", totalCost); put("paidAmount", paidAmount); put("remainingDebt", remainingDebt); put("invoiceNumber", invoiceNumber); put("paymentMethod", paymentMethod)
        put("cashPaid", cashPaid); put("transferPaid", transferPaid); put("exchangeAccountId", exchangeAccountId); put("exchangeAccountName", exchangeAccountName); put("timestamp", timestamp); put("unit", unit); put("unitCount", unitCount); put("colorsCount", colorsCount); put("notes", notes)
    }

    private fun ExpenseRecord.toJson() = JSONObject().apply {
        put("id", id); put("category", category); put("amount", amount); put("notes", notes); put("receiptNumber", receiptNumber); put("timestamp", timestamp); put("paidFromCash", paidFromCash); put("exchangeAccountId", exchangeAccountId); put("exchangeAccountName", exchangeAccountName); put("shiftId", shiftId)
    }

    private fun Employee.toJson() = JSONObject().apply {
        put("id", id); put("name", name); put("phone", phone); put("role", role); put("startDate", startDate); put("baseSalary", baseSalary); put("salaryType", salaryType); put("commissionPercent", commissionPercent); put("active", active)
    }

    private fun EmployeeTransaction.toJson() = JSONObject().apply {
        put("id", id); put("employeeId", employeeId); put("employeeName", employeeName); put("type", type); put("amount", amount); put("notes", notes); put("timestamp", timestamp); put("shiftId", shiftId); put("paidFromCash", paidFromCash); put("exchangeAccountId", exchangeAccountId); put("exchangeAccountName", exchangeAccountName)
    }

    private fun DebtRecord.toJson() = JSONObject().apply {
        put("id", id); put("type", type); put("partyName", partyName); put("phone", phone); put("originalAmount", originalAmount); put("paidAmount", paidAmount); put("remainingAmount", remainingAmount); put("details", details)
        put("referenceSaleId", referenceSaleId ?: JSONObject.NULL); put("referencePurchaseId", referencePurchaseId ?: JSONObject.NULL); put("dueDate", dueDate); put("createdAt", createdAt); put("isSettled", isSettled)
    }

    private fun DebtPayment.toJson() = JSONObject().apply {
        put("id", id); put("debtId", debtId); put("partyName", partyName); put("amount", amount); put("timestamp", timestamp); put("notes", notes); put("shiftId", shiftId); put("paymentMethod", paymentMethod); put("exchangeAccountId", exchangeAccountId)
    }

    private fun CashDrawerShift.toJson() = JSONObject().apply {
        put("id", id); put("openedAt", openedAt); put("closedAt", closedAt ?: JSONObject.NULL); put("openingBalance", openingBalance); put("closingBalanceActual", closingBalanceActual ?: JSONObject.NULL); put("isOpen", isOpen); put("cashierName", cashierName); put("notes", notes)
    }

    private fun CashDrawerMovement.toJson() = JSONObject().apply {
        put("id", id); put("shiftId", shiftId); put("type", type); put("amount", amount); put("isIncome", isIncome); put("title", title); put("details", details); put("timestamp", timestamp)
        put("referenceSaleId", referenceSaleId ?: JSONObject.NULL); put("referencePurchaseId", referencePurchaseId ?: JSONObject.NULL); put("referenceExpenseId", referenceExpenseId ?: JSONObject.NULL); put("referenceEmployeeTxId", referenceEmployeeTxId ?: JSONObject.NULL)
    }

    private fun ExchangeAccount.toJson() = JSONObject().apply {
        put("id", id); put("name", name); put("accountNumber", accountNumber); put("phone", phone); put("balance", balance); put("type", type); put("notes", notes); put("createdAt", createdAt)
    }

    private fun ExchangeTransfer.toJson() = JSONObject().apply {
        put("id", id); put("accountId", accountId); put("accountName", accountName); put("type", type); put("amount", amount); put("isCredit", isCredit); put("title", title); put("referenceNumber", referenceNumber); put("notes", notes); put("timestamp", timestamp)
    }

    private fun PartyAccount.toJson() = JSONObject().apply {
        put("id", id); put("type", type); put("name", name); put("phone", phone); put("address", address); put("notes", notes); put("initialBalance", initialBalance); put("createdAt", createdAt)
    }

    private fun AuditLogRecord.toJson() = JSONObject().apply {
        put("id", id); put("action", action); put("actor", actor); put("details", details); put("timestamp", timestamp)
    }

    private fun StoreSettings.toJson() = JSONObject().apply {
        put("id", id); put("shopNameAr", shopNameAr); put("shopNameEn", shopNameEn); put("phone", phone); put("address", address); put("currency", currency); put("taxNumber", taxNumber)
        put("invoiceHeader", invoiceHeader); put("invoiceFooter", invoiceFooter); put("showProfitInReports", showProfitInReports); put("managerPin", managerPin); put("isManagerRole", isManagerRole)
    }
}
