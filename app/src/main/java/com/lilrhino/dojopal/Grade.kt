package com.lilrhino.dojopal

import com.google.firebase.Timestamp

data class Grade(
    val datePassed: String = "",
    val examiner: String = "",
    val grade: String = "",
    val gradeId: String = "",
    val createdAt: Timestamp = Timestamp.now()
) {
    // Empty constructor for Firestore
    constructor() : this("", "", "", "", Timestamp.now())
    
    // Helper function to get grade display name
    fun getGradeDisplayName(): String {
        return grade
    }
    
    // Helper function to get grade order for sorting
    fun getGradeOrder(): Int {
        return when (grade) {
            "10th Kyu" -> 1
            "9th Kyu" -> 2
            "8th Kyu" -> 3
            "7th Kyu" -> 4
            "6th Kyu" -> 5
            "5th Kyu" -> 6
            "4th Kyu" -> 7
            "3rd Kyu" -> 8
            "2nd Kyu" -> 9
            "1st Kyu" -> 10
            "1st Dan" -> 11
            "2nd Dan" -> 12
            "3rd Dan" -> 13
            "4th Dan" -> 14
            "5th Dan" -> 15
            "6th Dan" -> 16
            "7th Dan" -> 17
            "8th Dan" -> 18
            "9th Dan" -> 19
            "10th Dan" -> 20
            else -> 0
        }
    }
}
