package com.lilrhino.dojopal

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val emailAddress: String = "",
    val clubName: String = "",
    val approved: Boolean = false,
    val students: List<Student> = emptyList(),
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    // Empty constructor for Firestore
    constructor() : this("", "", "", "", "", false, emptyList(), Timestamp.now(), Timestamp.now())
    
    // Helper function to get full name
    fun getFullName(): String {
        return "$firstName $lastName".trim()
    }
}
