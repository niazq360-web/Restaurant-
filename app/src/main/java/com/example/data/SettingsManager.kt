package com.example.data

import android.content.Context
import androidx.core.content.edit

class SettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("pos_settings", Context.MODE_PRIVATE)

    // Restaurant & Owner Profile
    fun getRestaurantName(): String = prefs.getString("name", "My Restaurant") ?: "My Restaurant"
    fun getRestaurantLocation(): String = prefs.getString("location", "Main Market, City") ?: "Main Market, City"
    fun getRestaurantPhone(): String = prefs.getString("phone", "0300-1234567") ?: "0300-1234567"
    fun getOwnerName(): String = prefs.getString("owner_name", "Owner Name") ?: "Owner Name"
    fun getOwnerPhone(): String = prefs.getString("owner_phone", "") ?: ""
    fun getReceiptFooter(): String = prefs.getString("receipt_footer", "Thank you for dining with us! Visit again.") ?: "Thank you for dining with us! Visit again."
    fun getQrCodePath(): String = prefs.getString("qr_path", "") ?: ""
    fun getLogoPath(): String = prefs.getString("logo_path", "") ?: ""

    fun saveRestaurantProfile(
        restaurantName: String,
        location: String,
        phone: String,
        ownerName: String,
        ownerPhone: String,
        receiptFooter: String
    ) {
        prefs.edit {
            putString("name", restaurantName)
            putString("location", location)
            putString("phone", phone)
            putString("owner_name", ownerName)
            putString("owner_phone", ownerPhone)
            putString("receipt_footer", receiptFooter)
        }
    }

    fun saveDetails(name: String, location: String, phone: String) {
        prefs.edit {
            putString("name", name)
            putString("location", location)
            putString("phone", phone)
        }
    }
    
    fun saveQrCodePath(path: String) {
        prefs.edit { putString("qr_path", path) }
    }

    fun saveLogoPath(path: String) {
        prefs.edit { putString("logo_path", path) }
    }

    // 12-Month Financial Cycle & Auto-Renewal
    fun getCycleStartDate(): Long {
        val date = prefs.getLong("cycle_start_date", 0L)
        if (date == 0L) {
            val now = System.currentTimeMillis()
            prefs.edit { putLong("cycle_start_date", now) }
            return now
        }
        return date
    }

    fun getCycleNumber(): Int = prefs.getInt("cycle_number", 1)

    fun is12MonthCycleCompleted(): Boolean {
        val start = getCycleStartDate()
        val elapsed = System.currentTimeMillis() - start
        val oneYearMillis = 365L * 24 * 60 * 60 * 1000L
        return elapsed >= oneYearMillis
    }

    fun startNext12MonthCycle() {
        val currentCycle = getCycleNumber()
        prefs.edit {
            putLong("cycle_start_date", System.currentTimeMillis())
            putInt("cycle_number", currentCycle + 1)
        }
    }

    // User Account (100% Offline)
    fun getUserPhone(): String = prefs.getString("user_phone", "") ?: ""
    fun getUserName(): String = prefs.getString("user_name", "") ?: ""
    fun getUserRole(): String = prefs.getString("user_role", "Manager") ?: "Manager"
    fun isUserLoggedIn(): Boolean = prefs.getBoolean("user_logged_in", false)

    fun saveUserAccount(phone: String, name: String, role: String = "Manager") {
        prefs.edit {
            putString("user_phone", phone)
            putString("user_name", name)
            putString("user_role", role)
            putBoolean("user_logged_in", true)
        }
    }

    fun logoutUser() {
        prefs.edit {
            putBoolean("user_logged_in", false)
        }
    }
}
