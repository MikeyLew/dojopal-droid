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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lilrhino.dojopal.ui.theme.DojoPalTheme

class AccountSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DojoPalTheme {
                AccountSettingsScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen() {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser
    
    // Form fields
    var newEmail by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    var isLoading by remember { mutableStateOf(false) }
    
    // Error states
    var emailError by remember { mutableStateOf("") }
    var currentPasswordError by remember { mutableStateOf("") }
    var newPasswordError by remember { mutableStateOf("") }
    var confirmPasswordError by remember { mutableStateOf("") }
    var generalError by remember { mutableStateOf("") }
    
    // Delete account states
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showDeleteFinalWarning by remember { mutableStateOf(false) }
    var showReauthForDelete by remember { mutableStateOf(false) }
    var deletePassword by remember { mutableStateOf("") }
    var isDeletingAccount by remember { mutableStateOf(false) }
    
    // Initialize with current email
    LaunchedEffect(currentUser?.email) {
        newEmail = currentUser?.email ?: ""
    }
    fun finishUpdate(updates: List<String>) {
        isLoading = false
        Toast.makeText(context, updates.joinToString(", "), Toast.LENGTH_SHORT).show()
        val intent = Intent(context, MainActivity::class.java)
        context.startActivity(intent)
        (context as ComponentActivity).finish()
    }
    fun validateEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
        return email.matches(emailRegex.toRegex())
    }
    
    fun validatePassword(password: String): Boolean {
        if (password.length < 8) return false
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { "!@#$%^&*()_+-=[]{}|;:,.<>?".contains(it) }
        return hasUppercase && hasLowercase && hasDigit && hasSpecialChar
    }
    
    fun getPasswordStrengthMessage(password: String): String {
        if (password.isEmpty()) return ""
        val issues = mutableListOf<String>()
        if (password.length < 8) issues.add("at least 8 characters")
        if (!password.any { it.isUpperCase() }) issues.add("one uppercase letter")
        if (!password.any { it.isLowerCase() }) issues.add("one lowercase letter")
        if (!password.any { it.isDigit() }) issues.add("one digit")
        if (!password.any { "!@#$%^&*()_+-=[]{}|;:,.<>?".contains(it) }) issues.add("one special character")
        return if (issues.isEmpty()) "Strong password ✓" else "Password must contain: ${issues.joinToString(", ")}"
    }
    
    fun validateForm(): Boolean {
        emailError = ""
        currentPasswordError = ""
        newPasswordError = ""
        confirmPasswordError = ""
        generalError = ""
        
        var isValid = true
        var hasChanges = false
        
        // Check if email changed
        if (newEmail != currentUser?.email) {
            hasChanges = true
            if (newEmail.isEmpty()) {
                emailError = "Email is required"
                isValid = false
            } else if (!validateEmail(newEmail)) {
                emailError = "Please enter a valid email address"
                isValid = false
            }
        }
        
        // Check if password changed
        if (newPassword.isNotEmpty() || confirmNewPassword.isNotEmpty()) {
            hasChanges = true
            if (currentPassword.isEmpty()) {
                currentPasswordError = "Current password is required to change password"
                isValid = false
            }
            
            if (newPassword.isEmpty()) {
                newPasswordError = "New password is required"
                isValid = false
            } else if (!validatePassword(newPassword)) {
                newPasswordError = "Password must be strong (8+ chars, upper/lower case, digit, special char)"
                isValid = false
            }
            
            if (confirmNewPassword.isEmpty()) {
                confirmPasswordError = "Please confirm your new password"
                isValid = false
            } else if (newPassword != confirmNewPassword) {
                confirmPasswordError = "Passwords do not match"
                isValid = false
            }
        }
        
        if (!hasChanges) {
            generalError = "No changes to save"
            isValid = false
        }
        
        return isValid
    }

    fun updatePassword() {
        val currentUser = auth.currentUser
        if (currentUser == null || currentUser.email == null) {
            generalError = "User not authenticated"
            isLoading = false
            return
        }

        // Re-authenticate user
        val credential = EmailAuthProvider.getCredential(currentUser.email!!, currentPassword)
        currentUser.reauthenticate(credential)
            .addOnCompleteListener { reauthTask ->
                if (reauthTask.isSuccessful) {
                    // Update password
                    currentUser.updatePassword(newPassword)
                        .addOnCompleteListener { passwordTask ->
                            if (passwordTask.isSuccessful) {
                                val updates = mutableListOf("Password updated")
                                finishUpdate(updates)
                            } else {
                                val error = passwordTask.exception?.message ?: "Failed to update password"
                                generalError = error
                                isLoading = false
                            }
                        }
                } else {
                    val error = reauthTask.exception?.message ?: "Current password is incorrect"
                    currentPasswordError = error
                    isLoading = false
                }
            }
    }

    fun deleteAccount() {
        val currentUser = auth.currentUser
        if (currentUser == null || currentUser.email == null) {
            generalError = "No user is currently signed in"
            deletePassword = ""
            return
        }

        if (deletePassword.isEmpty()) {
            generalError = "Password is required"
            return
        }

        isDeletingAccount = true
        generalError = ""

        // Re-authenticate first (required for account deletion)
        val credential = EmailAuthProvider.getCredential(currentUser.email!!, deletePassword)
        currentUser.reauthenticate(credential)
            .addOnSuccessListener {
                // First delete Firestore data
                firestore.collection("accounts")
                    .document(currentUser.uid)
                    .delete()
                    .addOnSuccessListener {
                        // Then delete Firebase Auth account
                        currentUser.delete()
                            .addOnSuccessListener {
                                isDeletingAccount = false
                                deletePassword = ""
                                Toast.makeText(context, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                                // Sign out and navigate to sign in
                                auth.signOut()
                                val intent = Intent(context, SignInActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                context.startActivity(intent)
                                (context as ComponentActivity).finish()
                            }
                            .addOnFailureListener { exception ->
                                isDeletingAccount = false
                                deletePassword = ""
                                generalError = "Failed to delete account: ${exception.message}"
                                Toast.makeText(context, generalError, Toast.LENGTH_LONG).show()
                            }
                    }
                    .addOnFailureListener { exception ->
                        isDeletingAccount = false
                        deletePassword = ""
                        generalError = "Failed to delete account data: ${exception.message}"
                        Toast.makeText(context, generalError, Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { exception ->
                isDeletingAccount = false
                deletePassword = ""
                generalError = "Authentication failed: ${exception.message}"
                Toast.makeText(context, generalError, Toast.LENGTH_LONG).show()
            }
    }


    fun updateAccount() {
        if (!validateForm() || currentUser == null) return
        
        isLoading = true
        generalError = ""
        
        val updates = mutableListOf<String>()

        // Update email if changed
        if (newEmail != currentUser.email) {
            currentUser.updateEmail(newEmail)
                .addOnCompleteListener { emailTask ->
                    if (emailTask.isSuccessful) {
                        updates.add("Email updated")
                        // Update email in Firestore
                        firestore.collection("accounts")
                            .document(currentUser.uid)
                            .update("emailAddress", newEmail)
                            .addOnCompleteListener {
                                // Continue with password update if needed
                                if (newPassword.isNotEmpty()) {
                                    updatePassword()
                                } else {
                                    finishUpdate(updates)
                                }
                            }
                    } else {
                        val error = emailTask.exception?.message ?: "Failed to update email"
                        generalError = error
                        isLoading = false
                    }
                }
        } else if (newPassword.isNotEmpty()) {
            updatePassword()
        } else {
            isLoading = false
        }
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Account Settings") },
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
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Account Information",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Email Section
            Text(
                text = "Email Address",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 8.dp)
            )

            OutlinedTextField(
                value = newEmail,
                onValueChange = { newEmail = it },
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = emailError.isNotEmpty(),
                supportingText = {
                    if (emailError.isNotEmpty()) {
                        Text(emailError, color = MaterialTheme.colorScheme.error)
                    } else if (newEmail != currentUser?.email && newEmail.isNotEmpty()) {
                        Text("Email will be updated")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Password Section
            Text(
                text = "Change Password",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 8.dp)
            )

            Text(
                text = "Leave password fields empty if you don't want to change your password.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = currentPassword,
                onValueChange = { currentPassword = it },
                label = { Text("Current Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Current Password") },
                trailingIcon = {
                    IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
                        Icon(
                            imageVector = if (currentPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (currentPasswordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                visualTransformation = if (currentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = currentPasswordError.isNotEmpty(),
                supportingText = if (currentPasswordError.isNotEmpty()) {
                    { Text(currentPasswordError, color = MaterialTheme.colorScheme.error) }
                } else null,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("New Password") },
                placeholder = { Text("Strong password required") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "New Password") },
                trailingIcon = {
                    IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                        Icon(
                            imageVector = if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (newPasswordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = newPasswordError.isNotEmpty(),
                supportingText = {
                    if (newPasswordError.isNotEmpty()) {
                        Text(newPasswordError, color = MaterialTheme.colorScheme.error)
                    } else if (newPassword.isNotEmpty()) {
                        val strengthMessage = getPasswordStrengthMessage(newPassword)
                        Text(
                            strengthMessage,
                            color = if (validatePassword(newPassword)) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = confirmNewPassword,
                onValueChange = { confirmNewPassword = it },
                label = { Text("Confirm New Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Confirm Password") },
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = confirmPasswordError.isNotEmpty(),
                supportingText = if (confirmPasswordError.isNotEmpty()) {
                    { Text(confirmPasswordError, color = MaterialTheme.colorScheme.error) }
                } else null,
                modifier = Modifier.fillMaxWidth()
            )

            if (generalError.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = generalError,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Delete Account Section
            Spacer(modifier = Modifier.height(24.dp))

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Danger Zone",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            Text(
                text = "Permanently delete your account and all associated data. This action cannot be undone.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Button(
                onClick = { showDeleteConfirmation = true },
                enabled = !isDeletingAccount && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (isDeletingAccount) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onError
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Deleting Account...", fontSize = 16.sp)
                } else {
                    Text("Delete Account", fontSize = 16.sp)
                }
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
                    onClick = { updateAccount() },
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
                        Text("Save Changes", fontSize = 16.sp)
                    }
                }
            }
        }
    }
    
    // Delete Account Confirmation Dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Account", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to delete your account? This will permanently delete all your data including:\n\n" +
                    "• Your profile information\n" +
                    "• All students and their records\n" +
                    "• All grading history\n" +
                    "• License applications\n\n" +
                    "This action cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        showDeleteFinalWarning = true
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Delete Account Final Warning Dialog
    if (showDeleteFinalWarning) {
        AlertDialog(
            onDismissRequest = { showDeleteFinalWarning = false },
            title = { Text("Final Warning", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = {
                Text(
                    "This is your final warning. Once you delete your account, all data will be permanently lost and cannot be recovered. Are you absolutely sure you want to proceed?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteFinalWarning = false
                        showReauthForDelete = true
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete Forever")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteFinalWarning = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Re-authentication Dialog for Account Deletion
    if (showReauthForDelete) {
        AlertDialog(
            onDismissRequest = { 
                showReauthForDelete = false
                deletePassword = ""
            },
            title = { Text("Re-authenticate to Delete Account", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Please enter your password to confirm account deletion.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = deletePassword,
                        onValueChange = { deletePassword = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (deletePassword.isNotEmpty()) {
                            showReauthForDelete = false
                            deleteAccount()
                        }
                    },
                    enabled = deletePassword.isNotEmpty(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showReauthForDelete = false
                    deletePassword = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }
    

}
