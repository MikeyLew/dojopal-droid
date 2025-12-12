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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Work
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

class ApplyLicenseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DojoPalTheme {
                ApplyLicenseScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplyLicenseScreen() {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser
    
    // Get student data from intent
    val studentId = remember {
        (context as? ComponentActivity)?.intent?.getStringExtra("studentId") ?: ""
    }
    
    var userData by remember { mutableStateOf<User?>(null) }
    var selectedStudent by remember { mutableStateOf<Student?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Form fields - initialized from student data
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var emailAddress by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var postcode by remember { mutableStateOf("") }
    var occupation by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var clubName by remember { mutableStateOf("") }
    var agreedToMembershipTerms by remember { mutableStateOf(false) }
    var agreedToPhotography by remember { mutableStateOf(false) }
    
    var isSubmitting by remember { mutableStateOf(false) }
    var showPaymentAlert by remember { mutableStateOf(false) }
    
    // Error states
    var firstNameError by remember { mutableStateOf("") }
    var lastNameError by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf("") }
    var addressError by remember { mutableStateOf("") }
    var postcodeError by remember { mutableStateOf("") }
    var occupationError by remember { mutableStateOf("") }
    var birthDateError by remember { mutableStateOf("") }
    var clubNameError by remember { mutableStateOf("") }
    var termsError by remember { mutableStateOf("") }
    
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
                        
                        // Populate form fields with existing student data
                        selectedStudent?.let { student ->
                            firstName = student.firstName
                            lastName = student.lastName
                            emailAddress = student.emailAddress
                            phone = student.phone
                            address = student.address
                            postcode = student.postcode
                            occupation = student.occupation
                            birthDate = student.birthDate
                            clubName = student.clubName
                            agreedToMembershipTerms = student.agreedToMembershipTerms
                            agreedToPhotography = student.agreedToPhotography
                        }
                    }
                    isLoading = false
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(context, "Failed to load student data: ${exception.message}", Toast.LENGTH_LONG).show()
                    isLoading = false
                }
        } else {
            isLoading = false
        }
    }
    
    fun validateEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
        return email.matches(emailRegex.toRegex())
    }
    
    fun validatePhone(phone: String): Boolean {
        val cleanPhone = phone.replace(Regex("[\\s\\-\\(\\)]"), "")
        return cleanPhone.matches(Regex("\\d{10,15}"))
    }
    
    fun validatePostcode(postcode: String): Boolean {
        val postcodeRegex = "^[A-Z]{1,2}[0-9R][0-9A-Z]? [0-9][A-Z]{2}$"
        return postcode.matches(postcodeRegex.toRegex())
    }
    
    fun validateDate(date: String): Boolean {
        val dateRegex = "^(0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[0-2])/(19|20)\\d{2}$"
        return date.matches(dateRegex.toRegex())
    }
    
    fun validateForm(): Boolean {
        firstNameError = ""
        lastNameError = ""
        emailError = ""
        phoneError = ""
        addressError = ""
        postcodeError = ""
        occupationError = ""
        birthDateError = ""
        clubNameError = ""
        termsError = ""
        
        var isValid = true
        
        if (firstName.isEmpty()) {
            firstNameError = "First name is required"
            isValid = false
        }
        
        if (lastName.isEmpty()) {
            lastNameError = "Last name is required"
            isValid = false
        }
        
        if (emailAddress.isEmpty()) {
            emailError = "Email is required"
            isValid = false
        } else if (!validateEmail(emailAddress)) {
            emailError = "Please enter a valid email address"
            isValid = false
        }
        
        if (phone.isEmpty()) {
            phoneError = "Phone number is required"
            isValid = false
        } else if (!validatePhone(phone)) {
            phoneError = "Please enter a valid phone number (10-15 digits)"
            isValid = false
        }
        
        if (address.isEmpty()) {
            addressError = "Address is required"
            isValid = false
        }
        
        if (postcode.isEmpty()) {
            postcodeError = "Postcode is required"
            isValid = false
        } else if (!validatePostcode(postcode)) {
            postcodeError = "Please enter a valid UK postcode (e.g., SW1A 1AA)"
            isValid = false
        }
        
        if (occupation.isEmpty()) {
            occupationError = "Occupation is required"
            isValid = false
        }
        
        if (birthDate.isEmpty()) {
            birthDateError = "Birth date is required"
            isValid = false
        } else if (!validateDate(birthDate)) {
            birthDateError = "Please enter date in DD/MM/YYYY format"
            isValid = false
        }
        
        if (clubName.isEmpty()) {
            clubNameError = "Club name is required"
            isValid = false
        }
        
        if (!agreedToMembershipTerms) {
            termsError = "You must agree to membership terms"
            isValid = false
        }
        
        return isValid
    }
    
    fun submitLicenseApplication() {
        if (!validateForm() || selectedStudent == null || userData == null || currentUser?.uid == null) return
        
        isSubmitting = true
        
        // Update student with new values, preserving license dates and grading history
        // Set license application status to pending
        val updatedStudent = selectedStudent!!.copy(
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            emailAddress = emailAddress.trim(),
            phone = phone.trim(),
            address = address.trim(),
            postcode = postcode.trim(),
            occupation = occupation.trim(),
            birthDate = birthDate.trim(),
            clubName = clubName.trim(),
            agreedToMembershipTerms = agreedToMembershipTerms,
            agreedToPhotography = agreedToPhotography,
            licenseApplicationStatus = "pending"
            // licDate, licExpDate, and gradingHistory are preserved from original student
        )
        
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
                    isSubmitting = false
                    showPaymentAlert = true
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(context, "Failed to submit application: ${exception.message}", Toast.LENGTH_LONG).show()
                    isSubmitting = false
                }
        } else {
            Toast.makeText(context, "Student not found", Toast.LENGTH_LONG).show()
            isSubmitting = false
        }
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (selectedStudent != null) {
                            "Apply for License - ${selectedStudent!!.getFullName()}"
                        } else {
                            "Apply for License"
                        }
                    )
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
        } else if (selectedStudent == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Student not found",
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Information banner
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "License Application",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Please review and update the student information below. License dates will be set upon approval of this application.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                Text(
                    text = "Student Information",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Personal Information Section
                Text(
                    text = "Personal Information",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("First Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = "First Name") },
                        isError = firstNameError.isNotEmpty(),
                        supportingText = if (firstNameError.isNotEmpty()) { { Text(firstNameError) } } else null,
                        modifier = Modifier.weight(1f)
                    )
                    
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Last Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Last Name") },
                        isError = lastNameError.isNotEmpty(),
                        supportingText = if (lastNameError.isNotEmpty()) { { Text(lastNameError) } } else null,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                OutlinedTextField(
                    value = emailAddress,
                    onValueChange = { emailAddress = it },
                    label = { Text("Email Address") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = emailError.isNotEmpty(),
                    supportingText = if (emailError.isNotEmpty()) { { Text(emailError) } } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Phone") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = phoneError.isNotEmpty(),
                    supportingText = if (phoneError.isNotEmpty()) { { Text(phoneError) } } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = birthDate,
                    onValueChange = { birthDate = it },
                    label = { Text("Birth Date (DD/MM/YYYY)") },
                    placeholder = { Text("e.g., 15/03/1990") },
                    isError = birthDateError.isNotEmpty(),
                    supportingText = if (birthDateError.isNotEmpty()) { { Text(birthDateError) } } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = occupation,
                    onValueChange = { occupation = it },
                    label = { Text("Occupation") },
                    leadingIcon = { Icon(Icons.Default.Work, contentDescription = "Occupation") },
                    isError = occupationError.isNotEmpty(),
                    supportingText = if (occupationError.isNotEmpty()) { { Text(occupationError) } } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Address Section
                Text(
                    text = "Address Information",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    leadingIcon = { Icon(Icons.Default.Home, contentDescription = "Address") },
                    isError = addressError.isNotEmpty(),
                    supportingText = if (addressError.isNotEmpty()) { { Text(addressError) } } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = postcode,
                    onValueChange = { postcode = it },
                    label = { Text("Postcode") },
                    placeholder = { Text("e.g., SW1A 1AA") },
                    isError = postcodeError.isNotEmpty(),
                    supportingText = if (postcodeError.isNotEmpty()) { { Text(postcodeError) } } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Club Information Section
                Text(
                    text = "Club Information",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                OutlinedTextField(
                    value = clubName,
                    onValueChange = { clubName = it },
                    label = { Text("Club Name") },
                    isError = clubNameError.isNotEmpty(),
                    supportingText = if (clubNameError.isNotEmpty()) { { Text(clubNameError) } } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Terms and Conditions Section
                Text(
                    text = "Terms and Conditions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = agreedToMembershipTerms,
                        onCheckedChange = { agreedToMembershipTerms = it }
                    )
                    Text(
                        text = "Agreed to Membership Terms",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                
                if (termsError.isNotEmpty()) {
                    Text(
                        text = termsError,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = agreedToPhotography,
                        onCheckedChange = { agreedToPhotography = it }
                    )
                    Text(
                        text = "Agreed to Photography",
                        modifier = Modifier.padding(start = 8.dp)
                    )
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
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {
                        Text("Cancel", fontSize = 16.sp)
                    }
                    
                    Button(
                        onClick = { submitLicenseApplication() },
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Submit Application", fontSize = 16.sp)
                        }
                    }
                }
            }
        }
        
        // Payment alert dialog
        if (showPaymentAlert) {
            AlertDialog(
                onDismissRequest = {
                    showPaymentAlert = false
                    val intent = Intent(context, StudentsActivity::class.java)
                    context.startActivity(intent)
                    (context as ComponentActivity).finish()
                },
                title = {
                    Text(
                        text = "Application Submitted",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "License application has been submitted successfully!",
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "⚠️ Important:",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Please arrange payment with your club instructor to complete the license renewal process.",
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Your license status is now set to 'Pending' until payment is received and processed.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showPaymentAlert = false
                            val intent = Intent(context, StudentsActivity::class.java)
                            context.startActivity(intent)
                            (context as ComponentActivity).finish()
                        }
                    ) {
                        Text("OK")
                    }
                }
            )
        }
    }
}
