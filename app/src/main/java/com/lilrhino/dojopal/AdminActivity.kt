package com.lilrhino.dojopal

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lilrhino.dojopal.ui.theme.DojoPalTheme

class AdminActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DojoPalTheme {
                AdminScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen() {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    var allUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchText by remember { mutableStateOf("") }
    var selectedUser by remember { mutableStateOf<User?>(null) }
    var showUserDetails by remember { mutableStateOf(false) }

    val filteredUsers = if (searchText.isEmpty()) {
        allUsers
    } else {
        allUsers.filter { user ->
            user.getFullName().contains(searchText, ignoreCase = true) ||
            user.emailAddress.contains(searchText, ignoreCase = true) ||
            user.clubName.contains(searchText, ignoreCase = true)
        }
    }

    fun loadAllUsers() {
        // Security: This function should only be accessible to admins
        // Firestore Rules will enforce this server-side
        isLoading = true
        firestore.collection("accounts")
            .limit(1000) // Security: Limit results to prevent DoS
            .get()
            .addOnSuccessListener { snapshot ->
                val users = mutableListOf<User>()
                for (document in snapshot.documents) {
                    // Security: Validate document ID
                    val docId = document.id
                    if (docId.isEmpty() || docId.length > 128) continue

                    val user = document.toObject(User::class.java)
                    user?.let {
                        // Security: Limit students per user
                        if (it.students.size > 100) {
                            // Truncate to prevent memory issues
                            val limitedStudents = it.students.take(100)
                            val limitedUser = it.copy(students = limitedStudents)
                            users.add(limitedUser)
                        } else {
                            users.add(it)
                        }
                    }
                }
                allUsers = users.sortedBy { it.getFullName() }
                isLoading = false
            }
            .addOnFailureListener { exception ->
                // Security: Don't expose detailed error information
                Toast.makeText(context, "Failed to load users", Toast.LENGTH_LONG).show()
                isLoading = false
            }
    }

    LaunchedEffect(Unit) {
        loadAllUsers()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
                actions = {
                    IconButton(onClick = { loadAllUsers() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = {
                        auth.signOut()
                        val intent = Intent(context, SignInActivity::class.java)
                        context.startActivity(intent)
                        (context as ComponentActivity).finish()
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Search bar
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = { Text("Search users...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    singleLine = true
                )

                // Users list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredUsers) { user ->
                        UserCard(
                            user = user,
                            onClick = {
                                selectedUser = user
                                showUserDetails = true
                            }
                        )
                    }
                }
            }
        }
    }

    // User details sheet
    if (showUserDetails && selectedUser != null) {
        AdminUserDetailSheet(
            user = selectedUser!!,
            onDismiss = {
                showUserDetails = false
                selectedUser = null
            },
            onUpdate = {
                loadAllUsers()
                showUserDetails = false
                selectedUser = null
            }
        )
    }
}

@Composable
fun UserCard(user: User, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = user.getFullName(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = user.emailAddress,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = user.clubName,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                if (user.approved) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Approved",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Approved",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = "Pending",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Pending",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${user.students.size} student${if (user.students.size != 1) "s" else ""}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserDetailSheet(
    user: User,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit
) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    var isUpdating by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var approved by remember { mutableStateOf(user.approved) }
    var isEditing by remember { mutableStateOf(false) }
    var editedFirstName by remember { mutableStateOf(user.firstName) }
    var editedLastName by remember { mutableStateOf(user.lastName) }
    var editedEmail by remember { mutableStateOf(user.emailAddress) }
    var editedClubName by remember { mutableStateOf(user.clubName) }
    var showAddStudent by remember { mutableStateOf(false) }
    var showEditStudent by remember { mutableStateOf<Student?>(null) }
    var showAddGrade by remember { mutableStateOf<Student?>(null) }
    var showPromoteStudent by remember { mutableStateOf<Student?>(null) }
    var showRenewLicense by remember { mutableStateOf<Student?>(null) }
    
    // Update local state when user changes
    LaunchedEffect(user) {
        editedFirstName = user.firstName
        editedLastName = user.lastName
        editedEmail = user.emailAddress
        editedClubName = user.clubName
        approved = user.approved
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = user.getFullName(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            // User Information Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "User Information",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        if (!isEditing) {
                            TextButton(onClick = { 
                                isEditing = true
                            }) {
                                Text("Edit")
                            }
                        } else {
                            Row {
                                TextButton(onClick = { 
                                    isEditing = false
                                    editedFirstName = user.firstName
                                    editedLastName = user.lastName
                                    editedEmail = user.emailAddress
                                    editedClubName = user.clubName
                                    approved = user.approved
                                }) {
                                    Text("Cancel")
                                }
                                TextButton(
                                    onClick = { 
                                        isUpdating = true
                                        saveChanges(
                                            userId = user.id,
                                            firstName = editedFirstName,
                                            lastName = editedLastName,
                                            email = editedEmail,
                                            clubName = editedClubName,
                                            approved = approved,
                                            firestore = firestore
                                        ) { success ->
                                            isUpdating = false
                                            if (success) {
                                                isEditing = false
                                                onUpdate()
                                            }
                                        }
                                    },
                                    enabled = !isUpdating
                                ) {
                                    Text("Save")
                                }
                            }
                        }
                    }
                    
                    if (isEditing) {
                        OutlinedTextField(
                            value = editedFirstName,
                            onValueChange = { editedFirstName = it },
                            label = { Text("First Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = editedLastName,
                            onValueChange = { editedLastName = it },
                            label = { Text("Last Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = editedEmail,
                            onValueChange = { editedEmail = it },
                            label = { Text("Email Address") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = editedClubName,
                            onValueChange = { editedClubName = it },
                            label = { Text("Club Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        InfoRow("Name", user.getFullName())
                        InfoRow("Email", user.emailAddress)
                        InfoRow("Club", user.clubName)
                        InfoRow("Status", if (user.approved) "Approved" else "Pending Approval")
                        InfoRow("User ID", user.id)
                    }
                }
            }

            // Approval Toggle
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Account Status",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Approved")
                        Switch(
                            checked = approved,
                            onCheckedChange = { newValue ->
                                approved = newValue
                                if (!isEditing) {
                                    updateApproval(user.id, newValue, firestore) { success ->
                                        if (success) {
                                            onUpdate()
                                        }
                                    }
                                }
                            },
                            enabled = !isUpdating
                        )
                    }
                }
            }

            // Students Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Students (${user.students.size})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        IconButton(onClick = {
                            showAddStudent = true
                        }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add Student",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    if (showAddStudent) {
                        AdminAddStudentSheet(
                            userId = user.id,
                            firestore = firestore,
                            onDismiss = { showAddStudent = false },
                            onUpdate = {
                                showAddStudent = false
                                onUpdate()
                            }
                        )
                    }

                    if (user.students.isEmpty()) {
                        Text(
                            text = "No students",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        user.students.forEach { student ->
                            AdminStudentInfoCard(
                                student = student,
                                userId = user.id,
                                onEdit = { studentToEdit ->
                                    showEditStudent = studentToEdit
                                },
                                onAddGrade = { studentToAddGrade ->
                                    showAddGrade = studentToAddGrade
                                },
                                onPromote = { studentToPromote ->
                                    showPromoteStudent = studentToPromote
                                },
                                onRenewLicense = { studentToRenew ->
                                    showRenewLicense = studentToRenew
                                },
                                onDelete = { studentToDelete ->
                                    deleteStudentFromAccount(user.id, studentToDelete, firestore) { success ->
                                        if (success) {
                                            onUpdate()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
            
            if (showEditStudent != null) {
                AdminEditStudentSheet(
                    userId = user.id,
                    student = showEditStudent!!,
                    firestore = firestore,
                    onDismiss = { showEditStudent = null },
                    onUpdate = {
                        showEditStudent = null
                        onUpdate()
                    }
                )
            }
            
            if (showAddGrade != null) {
                AdminAddGradeSheet(
                    userId = user.id,
                    student = showAddGrade!!,
                    firestore = firestore,
                    onDismiss = { showAddGrade = null },
                    onUpdate = {
                        showAddGrade = null
                        onUpdate()
                    }
                )
            }
            
            if (showPromoteStudent != null) {
                AdminPromoteStudentSheet(
                    userId = user.id,
                    student = showPromoteStudent!!,
                    firestore = firestore,
                    onDismiss = { showPromoteStudent = null },
                    onUpdate = {
                        showPromoteStudent = null
                        onUpdate()
                    }
                )
            }
            
            if (showRenewLicense != null) {
                AdminRenewLicenseSheet(
                    userId = user.id,
                    student = showRenewLicense!!,
                    firestore = firestore,
                    onDismiss = { showRenewLicense = null },
                    onUpdate = {
                        showRenewLicense = null
                        onUpdate()
                    }
                )
            }

            // Danger Zone
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Danger Zone",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )

                    Button(
                        onClick = { showDeleteConfirmation = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isUpdating
                    ) {
                        Text("Delete Account")
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Account") },
            text = {
                Text("Are you sure you want to delete this account? This will permanently delete the user and all their students. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        deleteAccount(user.id, firestore) { success ->
                            if (success) {
                                onUpdate()
                                onDismiss()
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AdminStudentInfoCard(
    student: Student,
    userId: String,
    onEdit: (Student) -> Unit,
    onAddGrade: (Student) -> Unit,
    onPromote: (Student) -> Unit,
    onRenewLicense: (Student) -> Unit,
    onDelete: (Student) -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${student.firstName} ${student.lastName}",
                        fontWeight = FontWeight.Bold
                    )
                    if (student.emailAddress.isNotEmpty()) {
                        Text(
                            text = student.emailAddress,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (student.phone.isNotEmpty()) {
                        Text(
                            text = "Phone: ${student.phone}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Show highest grade if available
                    val highestGrade = student.getHighestGrade()
                    if (highestGrade != null) {
                        Text(
                            text = "Highest Grade: ${highestGrade.grade}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = { onPromote(student) },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowUpward,
                            contentDescription = "Promote Student",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    IconButton(
                        onClick = { onAddGrade(student) },
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.School,
                            contentDescription = "Add Grade",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    IconButton(
                        onClick = { onEdit(student) },
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Student",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    IconButton(
                        onClick = { showDeleteConfirmation = true },
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Student",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            // License status alerts - matching StudentsActivity logic
            if (student.isLicenseApplicationPending()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⏳ License Application Pending - Arrange payment with instructor",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (student.isLicenseExpired()) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onRenewLicense(student) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚠️ License Expired - Tap to Renew",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (student.licExpDate.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "License valid until: ${student.licExpDate}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Student") },
            text = {
                Text("Are you sure you want to delete ${student.firstName} ${student.lastName}? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete(student)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

fun updateApproval(userId: String, approved: Boolean, firestore: FirebaseFirestore, onComplete: (Boolean) -> Unit) {
    // Security: Validate input
    if (userId.isEmpty() || userId.length > 128 || !userId.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
        onComplete(false)
        return
    }

    // Security: Firestore Rules will enforce admin-only access server-side
    firestore.collection("accounts")
        .document(userId)
        .update("approved", approved)
        .addOnSuccessListener {
            onComplete(true)
        }
        .addOnFailureListener {
            onComplete(false)
        }
}

fun deleteAccount(userId: String, firestore: FirebaseFirestore, onComplete: (Boolean) -> Unit) {
    // Security: Validate input
    if (userId.isEmpty() || userId.length > 128 || !userId.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
        onComplete(false)
        return
    }

    // Security: Firestore Rules will enforce admin-only access server-side
    firestore.collection("accounts")
        .document(userId)
        .delete()
        .addOnSuccessListener {
            onComplete(true)
        }
        .addOnFailureListener {
            onComplete(false)
        }
}

fun saveChanges(
    userId: String,
    firstName: String,
    lastName: String,
    email: String,
    clubName: String,
    approved: Boolean,
    firestore: FirebaseFirestore,
    onComplete: (Boolean) -> Unit
) {
    // Validate inputs
    if (firstName.isEmpty() || lastName.isEmpty() || 
        email.isEmpty() || clubName.isEmpty()) {
        onComplete(false)
        return
    }
    
    // Validate email format
    val emailRegex = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    if (!email.matches(Regex(emailRegex))) {
        onComplete(false)
        return
    }
    
    // Security: Validate input
    if (userId.isEmpty() || userId.length > 128 || !userId.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
        onComplete(false)
        return
    }
    
    // Fetch current user data to preserve students
    firestore.collection("accounts")
        .document(userId)
        .get()
        .addOnSuccessListener { document ->
            if (document.exists()) {
                val currentUser = document.toObject(User::class.java)
                if (currentUser != null) {
                    // Create updated user with new values but preserve students
                    val updatedUser = currentUser.copy(
                        firstName = firstName.trim(),
                        lastName = lastName.trim(),
                        emailAddress = email.trim(),
                        clubName = clubName.trim(),
                        approved = approved
                    )
                    
                    // Update the document
                    firestore.collection("accounts")
                        .document(userId)
                        .set(updatedUser)
                        .addOnSuccessListener {
                            onComplete(true)
                        }
                        .addOnFailureListener {
                            onComplete(false)
                        }
                } else {
                    onComplete(false)
                }
            } else {
                onComplete(false)
            }
        }
        .addOnFailureListener {
            onComplete(false)
        }
}

// These functions are now in AdminStudentSheets.kt

fun deleteStudentFromAccount(userId: String, studentToDelete: Student, firestore: FirebaseFirestore, onComplete: (Boolean) -> Unit) {
    // Security: Validate input
    if (userId.isEmpty() || userId.length > 128 || !userId.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
        onComplete(false)
        return
    }

    // Fetch user, remove student, and update
    firestore.collection("accounts")
        .document(userId)
        .get()
        .addOnSuccessListener { document ->
            if (document.exists()) {
                val user = document.toObject(User::class.java)
                if (user != null) {
                    // Find and remove the student by matching firstName, lastName, and emailAddress
                    val updatedStudents = user.students.filter { student ->
                        !(student.firstName == studentToDelete.firstName &&
                          student.lastName == studentToDelete.lastName &&
                          student.emailAddress == studentToDelete.emailAddress)
                    }

                    // Update the user document with the filtered students list
                    firestore.collection("accounts")
                        .document(userId)
                        .update("students", updatedStudents)
                        .addOnSuccessListener {
                            onComplete(true)
                        }
                        .addOnFailureListener {
                            onComplete(false)
                        }
                } else {
                    onComplete(false)
                }
            } else {
                onComplete(false)
            }
        }
        .addOnFailureListener {
            onComplete(false)
        }
}

