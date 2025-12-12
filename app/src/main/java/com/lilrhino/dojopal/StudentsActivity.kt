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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lilrhino.dojopal.ui.theme.DojoPalTheme

class StudentsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DojoPalTheme {
                StudentsScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentsScreen() {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser
    
    var userData by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var studentToDelete by remember { mutableStateOf<Student?>(null) }
    
    // Fetch user data with students
    LaunchedEffect(currentUser?.uid) {
        if (currentUser?.uid != null) {
            firestore.collection("accounts")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        userData = document.toObject(User::class.java)
                    } else {
                        errorMessage = "User data not found"
                    }
                    isLoading = false
                }
                .addOnFailureListener { exception ->
                    errorMessage = "Failed to load students: ${exception.message}"
                    isLoading = false
                }
        } else {
            isLoading = false
        }
    }
    
    fun refreshData() {
        if (currentUser?.uid != null) {
            isLoading = true
            firestore.collection("accounts")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        userData = document.toObject(User::class.java)
                    }
                    isLoading = false
                }
                .addOnFailureListener { exception ->
                    errorMessage = "Failed to refresh data: ${exception.message}"
                    isLoading = false
                }
        }
    }
    
    fun deleteStudent(student: Student) {
        if (userData != null && currentUser?.uid != null) {
            val updatedStudents = userData!!.students.filter { it != student }
            val updatedUser = userData!!.copy(students = updatedStudents)
            
            firestore.collection("accounts")
                .document(currentUser.uid)
                .set(updatedUser)
                .addOnSuccessListener {
                    userData = updatedUser
                    Toast.makeText(context, "Student removed successfully", Toast.LENGTH_SHORT).show()
                    showDeleteDialog = false
                    studentToDelete = null
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(context, "Failed to remove student: ${exception.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Students") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            val intent = Intent(context, MainActivity::class.java)
                            context.startActivity(intent)
                            (context as ComponentActivity).finish()
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val intent = Intent(context, AddStudentActivity::class.java)
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Student")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (userData?.students?.isEmpty() == true) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "No Students",
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No students yet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap the + button to add your first student",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(userData?.students ?: emptyList()) { student ->
                        StudentCard(
                            student = student,
                            onDelete = {
                                studentToDelete = student
                                showDeleteDialog = true
                            },
                            onAddGrade = {
                                val intent = Intent(context, AddGradeActivity::class.java)
                                intent.putExtra("studentId", student.firstName + student.lastName + student.emailAddress)
                                context.startActivity(intent)
                            },
                            onEdit = {
                                val intent = Intent(context, EditStudentActivity::class.java)
                                intent.putExtra("studentId", student.firstName + student.lastName + student.emailAddress)
                                context.startActivity(intent)
                            },
                            onApplyLicense = {
                                val intent = Intent(context, ApplyLicenseActivity::class.java)
                                intent.putExtra("studentId", student.firstName + student.lastName + student.emailAddress)
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog && studentToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                studentToDelete = null
            },
            title = { Text("Remove Student") },
            text = { Text("Are you sure you want to remove ${studentToDelete!!.getFullName()} from your students?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteStudent(studentToDelete!!)
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        studentToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentCard(
    student: Student,
    onDelete: () -> Unit,
    onAddGrade: () -> Unit,
    onEdit: () -> Unit,
    onApplyLicense: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = student.getFullName(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = student.emailAddress,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (student.phone.isNotEmpty()) {
                        Text(
                            text = student.phone,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Show highest grade if available
                    student.getHighestGrade()?.let { highestGrade ->
                        Text(
                            text = "Highest Grade: ${highestGrade.grade}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    // Show license expiration warning or pending status
                    if (student.isLicenseApplicationPending()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.padding(top = 4.dp)
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
                        Spacer(modifier = Modifier.height(4.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            onClick = onApplyLicense,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚠️ License Expired - Tap to Apply",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else if (student.licExpDate.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "License valid until: ${student.licExpDate}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row {
                    IconButton(
                        onClick = onEdit
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Student",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onAddGrade
                    ) {
                        Icon(
                            Icons.Default.School,
                            contentDescription = "Add Grade",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDelete
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Student",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (student.getFullAddress().isNotEmpty()) {
                Text(
                    text = student.getFullAddress(),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (student.occupation.isNotEmpty()) {
                Text(
                    text = "Occupation: ${student.occupation}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (student.birthDate.isNotEmpty()) {
                    Text(
                        text = "DOB: ${student.birthDate}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (student.licExpDate.isNotEmpty()) {
                    Text(
                        text = "License Exp: ${student.licExpDate}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
