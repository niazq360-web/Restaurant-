package com.example.data.auth

data class UserProfile(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val role: String = ROLE_CASHIER,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val ROLE_ADMIN = "admin"
        const val ROLE_MANAGER = "manager"
        const val ROLE_CASHIER = "cashier"
    }

    fun isAdmin(): Boolean = role.equals(ROLE_ADMIN, ignoreCase = true)
    
    fun isManager(): Boolean = role.equals(ROLE_MANAGER, ignoreCase = true) || isAdmin()
    
    fun isCashier(): Boolean = role.equals(ROLE_CASHIER, ignoreCase = true)

    fun getDisplayRole(): String {
        return when (role.lowercase()) {
            ROLE_ADMIN -> "Administrator"
            ROLE_MANAGER -> "Store Manager"
            ROLE_CASHIER -> "POS Cashier"
            else -> role.replaceFirstChar { it.uppercase() }
        }
    }
}
