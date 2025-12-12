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

class AddStudentActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DojoPalTheme {
                AddStudentScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStudentScreen() {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser
    
    // Form fields
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var emailAddress by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var postcode by remember { mutableStateOf("") }
    var occupation by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var licDate by remember { mutableStateOf("") }
    var licExpDate by remember { mutableStateOf("") }
    var clubName by remember { mutableStateOf("") }
    var agreedToMembershipTerms by remember { mutableStateOf(false) }
    var agreedToPhotography by remember { mutableStateOf(false) }
    
    var isLoading by remember { mutableStateOf(false) }
    
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
    var licDateError by remember { mutableStateOf("") }
    var licExpDateError by remember { mutableStateOf("") }
    var termsError by remember { mutableStateOf("") }
    
    fun validateEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
        return email.matches(emailRegex.toRegex())
    }
    
    fun validatePhone(phone: String): Boolean {
        // Remove spaces, dashes, and parentheses for validation
        val cleanPhone = phone.replace(Regex("[\\s\\-\\(\\)]"), "")
        // Check if it contains only digits and is between 10-15 characters
        return cleanPhone.matches(Regex("\\d{10,15}"))
    }
    
    fun validatePostcode(postcode: String): Boolean {
        // Basic UK postcode validation (simplified)
        val postcodeRegex = "^[A-Z]{1,2}[0-9R][0-9A-Z]? [0-9][A-Z]{2}$"
        return postcode.matches(postcodeRegex.toRegex())
    }
    
    fun validateDate(date: String): Boolean {
        // Basic date validation for DD/MM/YYYY format
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
        licDateError = ""
        licExpDateError = ""
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
        
        // Validate license dates if provided
        if (licDate.isNotEmpty() && !validateDate(licDate)) {
            licDateError = "Please enter license date in DD/MM/YYYY format"
            isValid = false
        }
        
        if (licExpDate.isNotEmpty() && !validateDate(licExpDate)) {
            licExpDateError = "Please enter license expiry date in DD/MM/YYYY format"
            isValid = false
        }
        
        if (!agreedToMembershipTerms) {
            termsError = "You must agree to membership terms"
            isValid = false
        }
        
        return isValid
    }
    
    fun addStudent() {
        if (!validateForm()) return
        
        isLoading = true
        
        // Get current user data
        if (currentUser?.uid != null) {
            firestore.collection("accounts")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val userData = document.toObject(User::class.java)
                        if (userData != null) {
                            val newStudent = Student(
                                address = address.trim(),
                                agreedToMembershipTerms = agreedToMembershipTerms,
                                agreedToPhotography = agreedToPhotography,
                                birthDate = birthDate.trim(),
                                clubName = clubName.trim(),
                                emailAddress = emailAddress.trim(),
                                firstName = firstName.trim(),
                                lastName = lastName.trim(),
                                licDate = licDate.trim(),
                                licExpDate = licExpDate.trim(),
                                occupation = occupation.trim(),
                                phone = phone.trim(),
                                postcode = postcode.trim()
                            )
                            
                            val updatedStudents = userData.students + newStudent
                            val updatedUser = userData.copy(students = updatedStudents)
                            
                            firestore.collection("accounts")
                                .document(currentUser.uid)
                                .set(updatedUser)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Student added successfully!", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(context, StudentsActivity::class.java)
                                    context.startActivity(intent)
                                    (context as ComponentActivity).finish()
                                }
                                .addOnFailureListener { exception ->
                                    Toast.makeText(context, "Failed to add student: ${exception.message}", Toast.LENGTH_LONG).show()
                                    isLoading = false
                                }
                        }
                    } else {
                        Toast.makeText(context, "User data not found", Toast.LENGTH_LONG).show()
                        isLoading = false
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(context, "Failed to load user data: ${exception.message}", Toast.LENGTH_LONG).show()
                    isLoading = false
                }
        }
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Add Student") }
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
            
            OutlinedTextField(
                value = licDate,
                onValueChange = { licDate = it },
                label = { Text("License Date (DD/MM/YYYY)") },
                placeholder = { Text("e.g., 01/01/2023") },
                isError = licDateError.isNotEmpty(),
                supportingText = if (licDateError.isNotEmpty()) { { Text(licDateError) } } else null,
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = licExpDate,
                onValueChange = { licExpDate = it },
                label = { Text("License Expiry Date (DD/MM/YYYY)") },
                placeholder = { Text("e.g., 01/01/2024") },
                isError = licExpDateError.isNotEmpty(),
                supportingText = if (licExpDateError.isNotEmpty()) { { Text(licExpDateError) } } else null,
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
                        val intent = Intent(context, MainActivity::class.java)
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
                    onClick = { addStudent() },
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
                        Text("Add Student", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
