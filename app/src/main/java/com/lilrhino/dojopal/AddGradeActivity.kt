package com.lilrhino.dojopal

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lilrhino.dojopal.ui.theme.DojoPalTheme

class AddGradeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DojoPalTheme {
                AddGradeScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGradeScreen() {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser
    
    // Get student data from intent
    val studentId = remember {
        (context as? ComponentActivity)?.intent?.getStringExtra("studentId") ?: ""
    }
    
    // Form fields
    var datePassed by remember { mutableStateOf("") }
    var examiner by remember { mutableStateOf("Bo Channon") }
    var grade by remember { mutableStateOf("") }
    
    var isLoading by remember { mutableStateOf(false) }
    var userData by remember { mutableStateOf<User?>(null) }
    var selectedStudent by remember { mutableStateOf<Student?>(null) }
    
    // Error states
    var datePassedError by remember { mutableStateOf("") }
    var examinerError by remember { mutableStateOf("") }
    var gradeError by remember { mutableStateOf("") }
    
    // Grade options
    val gradeOptions = listOf(
        "10th Kyu", "9th Kyu", "8th Kyu", "7th Kyu", "6th Kyu",
        "5th Kyu", "4th Kyu", "3rd Kyu", "2nd Kyu", "1st Kyu",
        "1st Dan", "2nd Dan", "3rd Dan", "4th Dan", "5th Dan",
        "6th Dan", "7th Dan", "8th Dan", "9th Dan", "10th Dan"
    )
    
    var expanded by remember { mutableStateOf(false) }
    
    fun validateDate(date: String): Boolean {
        // Basic date validation for DD/MM/YYYY format
        val dateRegex = "^(0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[0-2])/(19|20)\\d{2}$"
        return date.matches(dateRegex.toRegex())
    }
    
    fun validateForm(): Boolean {
        datePassedError = ""
        examinerError = ""
        gradeError = ""
        
        var isValid = true
        
        if (datePassed.isEmpty()) {
            datePassedError = "Date passed is required"
            isValid = false
        } else if (!validateDate(datePassed)) {
            datePassedError = "Please enter date in DD/MM/YYYY format"
            isValid = false
        }
        
        if (examiner.isEmpty()) {
            examinerError = "Examiner name is required"
            isValid = false
        }
        
        if (grade.isEmpty()) {
            gradeError = "Grade is required"
            isValid = false
        }
        
        return isValid
    }
    
    // Load user data and find student
    LaunchedEffect(currentUser?.uid, studentId) {
        if (currentUser?.uid != null && studentId.isNotEmpty()) {
            firestore.collection("accounts")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        userData = document.toObject(User::class.java)
                        selectedStudent = userData?.students?.find { 
                            it.firstName + it.lastName + it.emailAddress == studentId 
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(context, "Failed to load student data: ${exception.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
    
    fun addGrade() {
        if (!validateForm() || selectedStudent == null || userData == null || currentUser?.uid == null) return
        
        isLoading = true
        
        val newGrade = Grade(
            datePassed = datePassed.trim(),
            examiner = examiner.trim(),
            grade = grade.trim(),
            gradeId = ""
        )
        
        val updatedGradingHistory = selectedStudent!!.gradingHistory + newGrade
        val updatedStudent = selectedStudent!!.copy(gradingHistory = updatedGradingHistory)
        
        val studentIndex = userData!!.students.indexOfFirst { 
            it.firstName + it.lastName + it.emailAddress == studentId 
        }
        
        if (studentIndex != -1) {
            val updatedStudents = userData!!.students.toMutableList()
            updatedStudents[studentIndex] = updatedStudent
            val updatedUser = userData!!.copy(students = updatedStudents)
            
            firestore.collection("accounts")
                .document(currentUser.uid)
                .set(updatedUser)
                .addOnSuccessListener {
                    Toast.makeText(context, "Grade added successfully!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(context, StudentsActivity::class.java)
                    context.startActivity(intent)
                    (context as ComponentActivity).finish()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(context, "Failed to add grade: ${exception.message}", Toast.LENGTH_LONG).show()
                    isLoading = false
                }
        } else {
            Toast.makeText(context, "Student not found", Toast.LENGTH_LONG).show()
            isLoading = false
        }
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (selectedStudent != null) {
                            "Add Grade - ${selectedStudent!!.getFullName()}"
                        } else {
                            "Add Grade"
                        }
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Grade Information",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Grade Details Section
            Text(
                text = "Grade Details",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            OutlinedTextField(
                value = datePassed,
                onValueChange = { datePassed = it },
                label = { Text("Date Passed") },
                placeholder = { Text("e.g., 15/03/2024") },
                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = "Date") },
                isError = datePassedError.isNotEmpty(),
                supportingText = if (datePassedError.isNotEmpty()) { { Text(datePassedError) } } else null,
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = examiner,
                onValueChange = { examiner = it },
                label = { Text("Examiner") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Examiner") },
                isError = examinerError.isNotEmpty(),
                supportingText = if (examinerError.isNotEmpty()) { { Text(examinerError) } } else null,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Grade Dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = grade,
                    onValueChange = { grade = it },
                    readOnly = true,
                    label = { Text("Grade") },
                    leadingIcon = { Icon(Icons.Default.School, contentDescription = "Grade") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    isError = gradeError.isNotEmpty(),
                    supportingText = if (gradeError.isNotEmpty()) { { Text(gradeError) } } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    gradeOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                grade = option
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val intent = Intent(context, StudentsActivity::class.java)
                        context.startActivity(intent)
                        (context as ComponentActivity).finish()
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Text("Cancel", fontSize = 16.sp)
                }
                
                Button(
                    onClick = { addGrade() },
                    enabled = !isLoading,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Add Grade", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
