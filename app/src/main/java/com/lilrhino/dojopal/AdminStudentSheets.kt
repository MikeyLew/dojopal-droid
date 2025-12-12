package com.lilrhino.dojopal

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
//import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAddStudentSheet(
    userId: String,
    firestore: FirebaseFirestore,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit
) {
    val context = LocalContext.current
    
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
    
    fun saveStudent() {
        if (!validateForm()) return
        
        isLoading = true
        
        // Fetch current user data
        firestore.collection("accounts")
            .document(userId)
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
                            dateJoined = Timestamp.now(),
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
                            .document(userId)
                            .set(updatedUser)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Student added successfully!", Toast.LENGTH_SHORT).show()
                                isLoading = false
                                onUpdate()
                            }
                            .addOnFailureListener { exception ->
                                Toast.makeText(context, "Failed to add student: ${exception.message}", Toast.LENGTH_LONG).show()
                                isLoading = false
                            }
                    } else {
                        Toast.makeText(context, "User data not found", Toast.LENGTH_LONG).show()
                        isLoading = false
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.95f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Add Student",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            // Personal Information
            Text(
                text = "Personal Information",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("First Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "First Name") },
                isError = firstNameError.isNotEmpty(),
                supportingText = if (firstNameError.isNotEmpty()) { { Text(firstNameError) } } else null,
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Last Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Last Name") },
                isError = lastNameError.isNotEmpty(),
                supportingText = if (lastNameError.isNotEmpty()) { { Text(lastNameError) } } else null,
                modifier = Modifier.fillMaxWidth()
            )
            
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
            
            // Address Information
            Text(
                text = "Address Information",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
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
            
            // Club Information
            Text(
                text = "Club Information",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
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
            
            // Terms
            Text(
                text = "Terms and Conditions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Agreed to Membership Terms")
                Switch(
                    checked = agreedToMembershipTerms,
                    onCheckedChange = { agreedToMembershipTerms = it }
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Agreed to Photography")
                Switch(
                    checked = agreedToPhotography,
                    onCheckedChange = { agreedToPhotography = it }
                )
            }
            
            if (termsError.isNotEmpty()) {
                Text(
                    text = termsError,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }
            
            // Save Button
            Button(
                onClick = { saveStudent() },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save Student", fontSize = 16.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEditStudentSheet(
    userId: String,
    student: Student,
    firestore: FirebaseFirestore,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit
) {
    val context = LocalContext.current
    
    // Form fields - initialized from student
    var firstName by remember { mutableStateOf(student.firstName) }
    var lastName by remember { mutableStateOf(student.lastName) }
    var emailAddress by remember { mutableStateOf(student.emailAddress) }
    var phone by remember { mutableStateOf(student.phone) }
    var address by remember { mutableStateOf(student.address) }
    var postcode by remember { mutableStateOf(student.postcode) }
    var occupation by remember { mutableStateOf(student.occupation) }
    var birthDate by remember { mutableStateOf(student.birthDate) }
    var licDate by remember { mutableStateOf(student.licDate) }
    var licExpDate by remember { mutableStateOf(student.licExpDate) }
    var clubName by remember { mutableStateOf(student.clubName) }
    var agreedToMembershipTerms by remember { mutableStateOf(student.agreedToMembershipTerms) }
    var agreedToPhotography by remember { mutableStateOf(student.agreedToPhotography) }
    
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
        licDateError = ""
        licExpDateError = ""
        
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
        
        if (licDate.isNotEmpty() && !validateDate(licDate)) {
            licDateError = "Please enter license date in DD/MM/YYYY format"
            isValid = false
        }
        
        if (licExpDate.isNotEmpty() && !validateDate(licExpDate)) {
            licExpDateError = "Please enter license expiry date in DD/MM/YYYY format"
            isValid = false
        }
        
        return isValid
    }
    
    fun saveStudent() {
        if (!validateForm()) return
        
        isLoading = true
        
        // Fetch current user data
        firestore.collection("accounts")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val userData = document.toObject(User::class.java)
                    if (userData != null) {
                        // Find and update the student
                        val updatedStudents = userData.students.map { s ->
                            if (s.firstName == student.firstName &&
                                s.lastName == student.lastName &&
                                s.emailAddress == student.emailAddress) {
                                // Update this student, preserve grading history and other fields
                                s.copy(
                                    firstName = firstName.trim(),
                                    lastName = lastName.trim(),
                                    emailAddress = emailAddress.trim(),
                                    phone = phone.trim(),
                                    address = address.trim(),
                                    postcode = postcode.trim(),
                                    occupation = occupation.trim(),
                                    birthDate = birthDate.trim(),
                                    clubName = clubName.trim(),
                                    licDate = licDate.trim(),
                                    licExpDate = licExpDate.trim(),
                                    agreedToMembershipTerms = agreedToMembershipTerms,
                                    agreedToPhotography = agreedToPhotography
                                )
                            } else {
                                s
                            }
                        }
                        
                        val updatedUser = userData.copy(students = updatedStudents)
                        
                        firestore.collection("accounts")
                            .document(userId)
                            .set(updatedUser)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Student updated successfully!", Toast.LENGTH_SHORT).show()
                                isLoading = false
                                onUpdate()
                            }
                            .addOnFailureListener { exception ->
                                Toast.makeText(context, "Failed to update student: ${exception.message}", Toast.LENGTH_LONG).show()
                                isLoading = false
                            }
                    } else {
                        Toast.makeText(context, "User data not found", Toast.LENGTH_LONG).show()
                        isLoading = false
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
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.95f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Edit Student",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            // Personal Information
            Text(
                text = "Personal Information",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("First Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "First Name") },
                isError = firstNameError.isNotEmpty(),
                supportingText = if (firstNameError.isNotEmpty()) { { Text(firstNameError) } } else null,
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Last Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Last Name") },
                isError = lastNameError.isNotEmpty(),
                supportingText = if (lastNameError.isNotEmpty()) { { Text(lastNameError) } } else null,
                modifier = Modifier.fillMaxWidth()
            )
            
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
            
            // Address Information
            Text(
                text = "Address Information",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
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
            
            // Club Information
            Text(
                text = "Club Information",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
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
            
            // Terms
            Text(
                text = "Terms and Conditions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Agreed to Membership Terms")
                Switch(
                    checked = agreedToMembershipTerms,
                    onCheckedChange = { agreedToMembershipTerms = it }
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Agreed to Photography")
                Switch(
                    checked = agreedToPhotography,
                    onCheckedChange = { agreedToPhotography = it }
                )
            }
            
            // Save Button
            Button(
                onClick = { saveStudent() },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save Changes", fontSize = 16.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRenewLicenseSheet(
    userId: String,
    student: Student,
    firestore: FirebaseFirestore,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit
) {
    val context = LocalContext.current
    
    // Default to today's date in DD/MM/YYYY format
    val today = java.util.Calendar.getInstance()
    val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
    val todayString = dateFormat.format(today.time)
    
    // Default expiry to one year from today
    val oneYearLater = java.util.Calendar.getInstance()
    oneYearLater.add(java.util.Calendar.YEAR, 1)
    val oneYearLaterString = dateFormat.format(oneYearLater.time)
    
    // Form fields - initialized with today's date
    var licDate by remember { mutableStateOf(todayString) }
    var licExpDate by remember { mutableStateOf(oneYearLaterString) }
    var isLoading by remember { mutableStateOf(false) }
    
    // Error states
    var licDateError by remember { mutableStateOf("") }
    var licExpDateError by remember { mutableStateOf("") }
    
    fun validateDate(date: String): Boolean {
        val dateRegex = "^(0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[0-2])/(19|20)\\d{2}$"
        return date.matches(dateRegex.toRegex())
    }
    
    fun validateForm(): Boolean {
        licDateError = ""
        licExpDateError = ""
        
        var isValid = true
        
        if (licDate.isEmpty()) {
            licDateError = "License date is required"
            isValid = false
        } else if (!validateDate(licDate)) {
            licDateError = "Please enter date in DD/MM/YYYY format"
            isValid = false
        }
        
        if (licExpDate.isEmpty()) {
            licExpDateError = "License expiry date is required"
            isValid = false
        } else if (!validateDate(licExpDate)) {
            licExpDateError = "Please enter date in DD/MM/YYYY format"
            isValid = false
        }
        
        return isValid
    }
    
    fun renewLicense() {
        if (!validateForm()) return
        
        isLoading = true
        
        // Fetch current user data
        firestore.collection("accounts")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val userData = document.toObject(User::class.java)
                    if (userData != null) {
                        // Find and update the student
                        val updatedStudents = userData.students.map { s ->
                            if (s.firstName == student.firstName &&
                                s.lastName == student.lastName &&
                                s.emailAddress == student.emailAddress) {
                                // Update license dates and clear pending status
                                s.copy(
                                    licDate = licDate.trim(),
                                    licExpDate = licExpDate.trim(),
                                    licenseApplicationStatus = "" // Clear pending status
                                )
                            } else {
                                s
                            }
                        }
                        
                        val updatedUser = userData.copy(students = updatedStudents)
                        
                        firestore.collection("accounts")
                            .document(userId)
                            .set(updatedUser)
                            .addOnSuccessListener {
                                Toast.makeText(context, "License renewed successfully!", Toast.LENGTH_SHORT).show()
                                isLoading = false
                                onUpdate()
                            }
                            .addOnFailureListener { exception ->
                                Toast.makeText(context, "Failed to renew license: ${exception.message}", Toast.LENGTH_LONG).show()
                                isLoading = false
                            }
                    } else {
                        Toast.makeText(context, "User data not found", Toast.LENGTH_LONG).show()
                        isLoading = false
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
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Renew License",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Set new license dates for ${student.getFullName()}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // License Dates
            Text(
                text = "License Dates",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            
            OutlinedTextField(
                value = licDate,
                onValueChange = { licDate = it },
                label = { Text("License Date (DD/MM/YYYY)") },
                placeholder = { Text("e.g., 01/01/2024") },
                isError = licDateError.isNotEmpty(),
                supportingText = if (licDateError.isNotEmpty()) { { Text(licDateError) } } else null,
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = licExpDate,
                onValueChange = { licExpDate = it },
                label = { Text("License Expiry Date (DD/MM/YYYY)") },
                placeholder = { Text("e.g., 01/01/2025") },
                isError = licExpDateError.isNotEmpty(),
                supportingText = if (licExpDateError.isNotEmpty()) { { Text(licExpDateError) } } else null,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Renew Button
            Button(
                onClick = { renewLicense() },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Renew License", fontSize = 16.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAddGradeSheet(
    userId: String,
    student: Student,
    firestore: FirebaseFirestore,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit
) {
    val context = LocalContext.current
    
    // Form fields
    var datePassed by remember { mutableStateOf("") }
    var examiner by remember { mutableStateOf("Bo Channon") }
    var grade by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
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
    
    fun addGrade() {
        if (!validateForm()) return
        
        isLoading = true
        
        // Fetch current user data
        firestore.collection("accounts")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val userData = document.toObject(User::class.java)
                    if (userData != null) {
                        // Find and update the student
                        val updatedStudents = userData.students.map { s ->
                            if (s.firstName == student.firstName &&
                                s.lastName == student.lastName &&
                                s.emailAddress == student.emailAddress) {
                                val newGrade = Grade(
                                    datePassed = datePassed.trim(),
                                    examiner = examiner.trim(),
                                    grade = grade.trim(),
                                    gradeId = ""
                                )
                                s.copy(gradingHistory = s.gradingHistory + newGrade)
                            } else {
                                s
                            }
                        }
                        
                        val updatedUser = userData.copy(students = updatedStudents)
                        
                        firestore.collection("accounts")
                            .document(userId)
                            .set(updatedUser)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Grade added successfully!", Toast.LENGTH_SHORT).show()
                                isLoading = false
                                onUpdate()
                            }
                            .addOnFailureListener { exception ->
                                Toast.makeText(context, "Failed to add grade: ${exception.message}", Toast.LENGTH_LONG).show()
                                isLoading = false
                            }
                    } else {
                        Toast.makeText(context, "User data not found", Toast.LENGTH_LONG).show()
                        isLoading = false
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
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.7f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Add Grade",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Add a new grade for ${student.getFullName()}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Grade Details
            Text(
                text = "Grade Details",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            
            OutlinedTextField(
                value = datePassed,
                onValueChange = { datePassed = it },
                label = { Text("Date Passed (DD/MM/YYYY)") },
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
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = grade,
                    onValueChange = { },
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
            
            // Add Button
            Button(
                onClick = { addGrade() },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPromoteStudentSheet(
    userId: String,
    student: Student,
    firestore: FirebaseFirestore,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit
) {
    val context = LocalContext.current
    
    // Default to today's date
    val today = java.util.Calendar.getInstance()
    val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
    val todayString = dateFormat.format(today.time)
    
    // Get next grade
    fun getNextGrade(): String {
        val highestGrade = student.getHighestGrade()
        if (highestGrade == null) {
            return "10th Kyu"
        }
        
        val gradeOptions = listOf(
            "10th Kyu", "9th Kyu", "8th Kyu", "7th Kyu", "6th Kyu",
            "5th Kyu", "4th Kyu", "3rd Kyu", "2nd Kyu", "1st Kyu",
            "1st Dan", "2nd Dan", "3rd Dan", "4th Dan", "5th Dan",
            "6th Dan", "7th Dan", "8th Dan", "9th Dan", "10th Dan"
        )
        
        val currentIndex = gradeOptions.indexOf(highestGrade.grade)
        if (currentIndex >= 0 && currentIndex < gradeOptions.size - 1) {
            return gradeOptions[currentIndex + 1]
        }
        
        return highestGrade.grade
    }
    
    // Form fields - initialized with today's date and next grade
    var datePassed by remember { mutableStateOf(todayString) }
    var examiner by remember { mutableStateOf("Bo Channon") }
    var grade by remember { mutableStateOf(getNextGrade()) }
    var isLoading by remember { mutableStateOf(false) }
    
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
    
    fun promoteStudent() {
        if (!validateForm()) return
        
        isLoading = true
        
        // Fetch current user data
        firestore.collection("accounts")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val userData = document.toObject(User::class.java)
                    if (userData != null) {
                        // Find and update the student
                        val updatedStudents = userData.students.map { s ->
                            if (s.firstName == student.firstName &&
                                s.lastName == student.lastName &&
                                s.emailAddress == student.emailAddress) {
                                val newGrade = Grade(
                                    datePassed = datePassed.trim(),
                                    examiner = examiner.trim(),
                                    grade = grade.trim(),
                                    gradeId = ""
                                )
                                s.copy(gradingHistory = s.gradingHistory + newGrade)
                            } else {
                                s
                            }
                        }
                        
                        val updatedUser = userData.copy(students = updatedStudents)
                        
                        firestore.collection("accounts")
                            .document(userId)
                            .set(updatedUser)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Student promoted successfully!", Toast.LENGTH_SHORT).show()
                                isLoading = false
                                onUpdate()
                            }
                            .addOnFailureListener { exception ->
                                Toast.makeText(context, "Failed to promote student: ${exception.message}", Toast.LENGTH_LONG).show()
                                isLoading = false
                            }
                    } else {
                        Toast.makeText(context, "User data not found", Toast.LENGTH_LONG).show()
                        isLoading = false
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
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.7f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Promote Student",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Promote ${student.getFullName()} to a new grade",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Promotion Details
            Text(
                text = "Promotion Details",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            
            OutlinedTextField(
                value = datePassed,
                onValueChange = { datePassed = it },
                label = { Text("Date Passed (DD/MM/YYYY)") },
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
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = grade,
                    onValueChange = { },
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
            
            // Promote Button
            Button(
                onClick = { promoteStudent() },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Promote Student", fontSize = 16.sp)
                }
            }
        }
    }
}

