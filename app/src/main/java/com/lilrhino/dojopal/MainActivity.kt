package com.lilrhino.dojopal

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lilrhino.dojopal.ui.theme.DojoPalTheme
import androidx.compose.foundation.layout.fillMaxWidth


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Check if user is already signed in
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            // User not signed in, redirect to SignInActivity
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }
        
        setContent {
            DojoPalTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser
    val context = LocalContext.current
    var userData by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var isAdmin by remember { mutableStateOf(false) }
    var checkingAdmin by remember { mutableStateOf(true) }
    
    // First check if user is admin
    LaunchedEffect(currentUser?.uid) {
        if (currentUser?.uid != null) {
            // Check admin status with security validation
            val userId = currentUser.uid

            // Security: Validate UID format
            if (userId.isEmpty() || userId.length > 128 || !userId.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
                checkingAdmin = false
                // Proceed with regular user check
            } else {
                firestore.collection("admins")
                    .document(userId)
                    .get()
                    .addOnSuccessListener { adminDoc ->
                        if (adminDoc.exists()) {
                            val adminData = adminDoc.getData()
                            val adminUserId = adminData?.get("userId") as? String

                            // Security: Strict validation - must match exactly
                            if (adminUserId != null &&
                                adminUserId == userId &&
                                adminDoc.id == userId
                            ) {
                                // Security: Additional check - verify document structure
                                val allowedKeys = setOf("userId")
                                val documentKeys = adminData.keys
                                if (documentKeys.all { it in allowedKeys } || documentKeys == allowedKeys) {
                                    // User is admin, redirect to AdminActivity
                                    isAdmin = true
                                    checkingAdmin = false
                                    val intent = Intent(context, AdminActivity::class.java)
                                    context.startActivity(intent)
                                    (context as ComponentActivity).finish()

                                    return@addOnSuccessListener
                                }
                            }
                        }

                        // Not an admin, check regular user data
                        checkingAdmin = false
                        firestore.collection("accounts")
                            .document(currentUser.uid)
                            .get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    val user = document.toObject(User::class.java)
                                    userData = user

                                    // Check if user is approved
                                    if (user?.approved != true) {
                                        // User is not approved, redirect to PendingApprovalActivity
                                        val intent =
                                            Intent(context, PendingApprovalActivity::class.java)
                                        context.startActivity(intent)
                                        (context as ComponentActivity).finish()
                                        return@addOnSuccessListener
                                    }
                                } else {
                                    errorMessage = "User data not found"
                                }
                                isLoading = false
                            }
                            .addOnFailureListener { exception ->
                                errorMessage = "Failed to load user data: ${exception.message}"
                                isLoading = false
                            }
                    }
                    .addOnFailureListener {
                        // Error checking admin, proceed with regular user check
                        checkingAdmin = false
                        firestore.collection("accounts")
                            .document(currentUser.uid)
                            .get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    val user = document.toObject(User::class.java)
                                    userData = user

                                    if (user?.approved != true) {
                                        val intent =
                                            Intent(context, PendingApprovalActivity::class.java)
                                        context.startActivity(intent)
                                        (context as ComponentActivity).finish()
                                        return@addOnSuccessListener
                                    }
                                } else {
                                    errorMessage = "User data not found"
                                }
                                isLoading = false
                            }
                            .addOnFailureListener { exception ->
                                errorMessage = "Failed to load user data: ${exception.message}"
                                isLoading = false
                            }
                    }
            }
        }
    }
    
    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to WKC DojoPal!",
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(bottom = 16.dp))
        } else if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        } else if (userData != null) {
            Text(
                text = "Welcome, ${userData!!.getFullName()}!",
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Email: ${userData!!.emailAddress}",
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Club: ${userData!!.clubName}",
                modifier = Modifier.padding(bottom = 24.dp)
            )
        } else {
            Text(
                text = "Signed in as: ${currentUser?.email ?: "Unknown"}",
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
        
        Button(
            onClick = {
                val intent = Intent(context, StudentsActivity::class.java)
                context.startActivity(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("Manage Students")
        }
        
        Button(
            onClick = {
                val intent = Intent(context, AccountSettingsActivity::class.java)
                context.startActivity(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("Account Settings")
        }
        
        Button(
            onClick = {
                auth.signOut()
                val intent = Intent(context, SignInActivity::class.java)
                context.startActivity(intent)
                (context as ComponentActivity).finish()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("Sign Out")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    DojoPalTheme {
        MainScreen()
    }
}