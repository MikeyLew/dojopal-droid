package com.lilrhino.dojopal

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
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

class PendingApprovalActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DojoPalTheme {
                PendingApprovalScreen()
            }
        }
    }
}

@Composable
fun PendingApprovalScreen() {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser
    
    var userData by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isCheckingApproval by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    // Function to check approval status
    fun checkApprovalStatus() {
        if (currentUser?.uid != null) {
            isCheckingApproval = true
            firestore.collection("accounts")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val user = document.toObject(User::class.java)
                        userData = user
                        
                        if (user?.approved == true) {
                            // User is approved, navigate to MainActivity
                            val intent = Intent(context, MainActivity::class.java)
                            context.startActivity(intent)
                            (context as ComponentActivity).finish()
                        }
                    } else {
                        errorMessage = "User data not found"
                    }
                    isCheckingApproval = false
                }
                .addOnFailureListener { exception ->
                    errorMessage = "Failed to check approval status: ${exception.message}"
                    isCheckingApproval = false
                }
        } else {
            isCheckingApproval = false
        }
    }
    
    // Initial load
    LaunchedEffect(Unit) {
        checkApprovalStatus()
    }
    
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Pending Approval",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Account Pending Approval",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (userData != null) {
                Text(
                    text = "Hello ${userData!!.getFullName()}!",
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "Your account is currently under review.",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "Club: ${userData!!.clubName}",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }
            
            Text(
                text = "We'll notify you once your account has been approved by an administrator. This usually takes 24-48 hours.",
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            Button(
                onClick = { checkApprovalStatus() },
                enabled = !isCheckingApproval,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 16.dp)
            ) {
                if (isCheckingApproval) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Check Status", fontSize = 16.sp)
                }
            }
            
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            TextButton(
                onClick = {
                    auth.signOut()
                    val intent = Intent(context, SignInActivity::class.java)
                    context.startActivity(intent)
                    (context as ComponentActivity).finish()
                }
            ) {
                Text("Sign Out")
            }
        }
    }
}
