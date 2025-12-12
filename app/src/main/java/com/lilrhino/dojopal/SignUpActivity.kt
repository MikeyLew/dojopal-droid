package com.lilrhino.dojopal

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sports
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lilrhino.dojopal.ui.theme.DojoPalTheme

class SignUpActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DojoPalTheme {
                SignUpScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen() {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var emailAddress by remember { mutableStateOf("") }
    var clubName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var firstNameError by remember { mutableStateOf("") }
    var lastNameError by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf("") }
    var clubNameError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var confirmPasswordError by remember { mutableStateOf("") }
    var agreedToTerms by remember { mutableStateOf(false) }
    var termsError by remember { mutableStateOf("") }
    var showPDFViewer by remember { mutableStateOf(false) }
    var pdfFileName by remember { mutableStateOf("") }

    fun validateEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
        return email.matches(emailRegex.toRegex())
    }

    fun validatePassword(password: String): Boolean {
        // Strong password requirements:
        // - At least 8 characters
        // - At least one uppercase letter
        // - At least one lowercase letter
        // - At least one digit
        // - At least one special character
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
        
        if (password.length < 8) {
            issues.add("at least 8 characters")
        }
        if (!password.any { it.isUpperCase() }) {
            issues.add("one uppercase letter")
        }
        if (!password.any { it.isLowerCase() }) {
            issues.add("one lowercase letter")
        }
        if (!password.any { it.isDigit() }) {
            issues.add("one digit")
        }
        if (!password.any { "!@#$%^&*()_+-=[]{}|;:,.<>?".contains(it) }) {
            issues.add("one special character")
        }
        
        return if (issues.isEmpty()) {
            "Strong password ✓"
        } else {
            "Password must contain: ${issues.joinToString(", ")}"
        }
    }

    fun validateForm(): Boolean {
        firstNameError = ""
        lastNameError = ""
        emailError = ""
        clubNameError = ""
        passwordError = ""
        confirmPasswordError = ""

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

        if (clubName.isEmpty()) {
            clubNameError = "Club name is required"
            isValid = false
        }

        if (password.isEmpty()) {
            passwordError = "Password is required"
            isValid = false
        } else if (!validatePassword(password)) {
            passwordError = "Password must be strong (8+ chars, upper/lower case, digit, special char)"
            isValid = false
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordError = "Please confirm your password"
            isValid = false
        } else if (password != confirmPassword) {
            confirmPasswordError = "Passwords do not match"
            isValid = false
        }

        if (!agreedToTerms) {
            termsError = "You must agree to the Privacy Policy and Terms & Conditions"
            isValid = false
        } else {
            termsError = ""
        }

        return isValid
    }

    fun signUp() {
        if (!validateForm()) return
        
        isLoading = true
        auth.createUserWithEmailAndPassword(emailAddress, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        // Create user document in Firestore
                        val userData = User(
                            id = user.uid,
                            firstName = firstName.trim(),
                            lastName = lastName.trim(),
                            emailAddress = emailAddress.trim(),
                            clubName = clubName.trim()
                        )
                        
                        firestore.collection("accounts")
                            .document(user.uid)
                            .set(userData)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Account created successfully! Awaiting approval.", Toast.LENGTH_LONG).show()
                                val intent = Intent(context, PendingApprovalActivity::class.java)
                                context.startActivity(intent)
                                isLoading = false
                            }
                            .addOnFailureListener { exception ->
                                Toast.makeText(context, "Failed to save user data: ${exception.message}", Toast.LENGTH_LONG).show()
                                isLoading = false
                            }
                    }
                } else {
                    val errorMessage = task.exception?.message ?: "Sign up failed"
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    isLoading = false
                }
            }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Create Account",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // First Name Field
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("First Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "First Name") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                isError = firstNameError.isNotEmpty(),
                supportingText = if (firstNameError.isNotEmpty()) { { Text(firstNameError) } } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            // Last Name Field
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Last Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Last Name") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                isError = lastNameError.isNotEmpty(),
                supportingText = if (lastNameError.isNotEmpty()) { { Text(lastNameError) } } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            // Email Field
            OutlinedTextField(
                value = emailAddress,
                onValueChange = { emailAddress = it },
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = emailError.isNotEmpty(),
                supportingText = if (emailError.isNotEmpty()) { { Text(emailError) } } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            // Club Name Field
            OutlinedTextField(
                value = clubName,
                onValueChange = { clubName = it },
                label = { Text("Club Name") },
                leadingIcon = { Icon(Icons.Default.Sports, contentDescription = "Club Name") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                isError = clubNameError.isNotEmpty(),
                supportingText = if (clubNameError.isNotEmpty()) { { Text(clubNameError) } } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                placeholder = { Text("Strong password required") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = passwordError.isNotEmpty(),
                supportingText = {
                    if (passwordError.isNotEmpty()) {
                        Text(passwordError, color = MaterialTheme.colorScheme.error)
                    } else if (password.isNotEmpty()) {
                        val strengthMessage = getPasswordStrengthMessage(password)
                        Text(
                            strengthMessage,
                            color = if (validatePassword(password)) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
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
                supportingText = if (confirmPasswordError.isNotEmpty()) { { Text(confirmPasswordError) } } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            // Terms of Use Agreement
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Agree to Terms & Conditions and Privacy Policy",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "By creating an account, you confirm that:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "• The information you enter is accurate and relates to you or a child/dependent you are responsible for.",
                            fontSize = 12.sp
                        )
                        Text(
                            text = "• You understand the App is used only for managing Welsh Karate Collective membership and student records.",
                            fontSize = 12.sp
                        )
                        Text(
                            text = "• You agree to use the App respectfully and not access or share information about other members without permission.",
                            fontSize = 12.sp
                        )
                        Text(
                            text = "• You have read and agree to the Privacy Policy and Terms & Conditions, including how your data is stored and how you can request deletion at any time.",
                            fontSize = 12.sp
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Checkbox(
                            checked = agreedToTerms,
                            onCheckedChange = { 
                                agreedToTerms = it
                                if (agreedToTerms) {
                                    termsError = ""
                                }
                            }
                        )
                        Text(
                            text = "I agree to the Privacy Policy and Terms & Conditions",
                            fontSize = 14.sp,
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
                }
            }

            Button(
                onClick = { signUp() },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 16.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Sign Up", fontSize = 16.sp)
                }
            }
            
            // PDF Viewing Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        pdfFileName = "PrivacyPolicy.pdf"
                        showPDFViewer = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("View Privacy Policy", fontSize = 14.sp)
                }
                
                OutlinedButton(
                    onClick = {
                        pdfFileName = "TermsAndConditions.pdf"
                        showPDFViewer = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("View Terms and Conditions", fontSize = 14.sp)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Already have an account? ")
                TextButton(
                    onClick = {
                        val intent = Intent(context, SignInActivity::class.java)
                        context.startActivity(intent)
                    }
                ) {
                    Text("Sign In")
                }
            }
            
            // PDF Viewer Sheet
            if (showPDFViewer) {
                PDFViewerSheet(
                    fileName = pdfFileName,
                    onDismiss = { showPDFViewer = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PDFViewerSheet(
    fileName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header with title and close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = fileName.replace(".pdf", ""),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // PDF Viewer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webViewClient = WebViewClient()
                            settings.javaScriptEnabled = true
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            settings.setSupportZoom(true)
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            
                            try {
                                // Read PDF from assets
                                val inputStream = ctx.assets.open(fileName)
                                val tempFile = File(ctx.cacheDir, fileName)
                                if (!tempFile.exists()) {
                                    tempFile.createNewFile()
                                }
                                
                                val outputStream = FileOutputStream(tempFile)
                                inputStream.copyTo(outputStream)
                                inputStream.close()
                                outputStream.close()
                                
                                // Create HTML wrapper to display PDF
                                val pdfPath = tempFile.absolutePath
                                val htmlContent = """
                                    <!DOCTYPE html>
                                    <html>
                                    <head>
                                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                        <style>
                                            body { margin: 0; padding: 0; overflow: hidden; }
                                            embed { width: 100%; height: 100vh; }
                                        </style>
                                    </head>
                                    <body>
                                        <embed src="file://$pdfPath" type="application/pdf" />
                                    </body>
                                    </html>
                                """.trimIndent()

                                loadDataWithBaseURL("file://$pdfPath", htmlContent, "text/html", "UTF-8", null)
                            } catch (e: Exception) {
                                loadData(
                                    "<html><body style='padding:20px;text-align:center;'><p>Error loading PDF: ${e.message}</p><p>Please ensure the PDF file is in the assets folder.</p></body></html>",
                                    "text/html",
                                    "UTF-8"
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
