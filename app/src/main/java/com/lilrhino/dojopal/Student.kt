package com.lilrhino.dojopal

import com.google.firebase.Timestamp

data class Student(
    val address: String = "",
    val agreedToMembershipTerms: Boolean = false,
    val agreedToPhotography: Boolean = false,
    val birthDate: String = "",
    val clubName: String = "",
    val dateJoined: Timestamp = Timestamp.now(),
    val emailAddress: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val licDate: String = "",
    val licExpDate: String = "",
    val occupation: String = "",
    val phone: String = "",
    val postcode: String = "",
    val gradingHistory: List<Grade> = emptyList(),
    val licenseApplicationStatus: String? = null // null, "pending", "approved", "rejected"
) {
    // Empty constructor for Firestore
    constructor() : this("", false, false, "", "", Timestamp.now(), "", "", "", "", "", "", "", "", emptyList(), null)
    
    // Helper function to get full name
    fun getFullName(): String {
        return "$firstName $lastName".trim()
    }
    
    // Helper function to get full address
    fun getFullAddress(): String {
        return "$address, $postcode".trim().trimEnd(',').trim()
    }
    
    // Helper function to get highest grade
    fun getHighestGrade(): Grade? {
        return gradingHistory.maxByOrNull { it.getGradeOrder() }
    }
    
    // Helper function to check if license is expired
    // License is expired if expiry date is in the current month or before
    fun isLicenseExpired(): Boolean {
        if (licExpDate.isEmpty()) return false
        
        try {
            // Parse date in DD/MM/YYYY format
            val parts = licExpDate.split("/")
            if (parts.size != 3) return false
            
            val day = parts[0].toIntOrNull() ?: return false
            val month = parts[1].toIntOrNull() ?: return false
            val year = parts[2].toIntOrNull() ?: return false
            
            // Get current date
            val currentDate = java.util.Calendar.getInstance()
            val currentYear = currentDate.get(java.util.Calendar.YEAR)
            val currentMonth = currentDate.get(java.util.Calendar.MONTH) + 1 // Calendar months are 0-based
            
            // Check if expired (expiry date is in current month or before)
            if (year < currentYear) return true
            if (year == currentYear && month < currentMonth) return true
            if (year == currentYear && month == currentMonth) return true // Expired if in current month
            
            return false
        } catch (e: Exception) {
            return false
        }
    }
    
    // Helper function to get expiration status message
    fun getLicenseExpirationStatus(): String {
        if (licExpDate.isEmpty()) return "No expiry date"
        if (isLicenseExpired()) return "License Expired"
        return "Valid until $licExpDate"
    }
    
    // Helper function to check if license application is pending
    fun isLicenseApplicationPending(): Boolean {
        return licenseApplicationStatus == "pending"
    }
}
