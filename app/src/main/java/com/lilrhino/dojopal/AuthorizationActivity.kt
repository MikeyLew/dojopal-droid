package com.lilrhino.dojopal

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
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
import com.lilrhino.dojopal.ui.theme.DojoPalTheme
import kotlinx.coroutines.launch

class AuthorizationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DojoPalTheme {
                AuthorizationScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorizationScreen() {
    val context = LocalContext.current
    
    var authorizationCode by remember { mutableStateOf("") }
    var codeError by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    // Hardcoded authorization code
    val validCode = "WKC2006"
    
    fun validateCode(code: String): Boolean {
        return code.trim().uppercase() == validCode.uppercase()
    }
    
    fun validateForm(): Boolean {
        codeError = ""
        
        if (authorizationCode.isEmpty()) {
            codeError = "Authorization code is required"
            return false
        }
        
        if (!validateCode(authorizationCode)) {
            codeError = "Invalid authorization code"
            return false
        }
        
        return true
    }
    
    fun proceedToSignUp() {
        if (!validateForm()) return
        
        isLoading = true
        
        // Simulate a brief loading for better UX
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            kotlinx.coroutines.delay(500) // Brief delay
            
            val intent = Intent(context, SignUpActivity::class.java)
            context.startActivity(intent)
            (context as ComponentActivity).finish()
            
            isLoading = false
        }
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Authorization") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            val intent = Intent(context, SignInActivity::class.java)
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Authorization",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Authorization Required",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "To create an account, please enter the authorization code provided by your instructor.",
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            OutlinedTextField(
                value = authorizationCode,
                onValueChange = { authorizationCode = it },
                label = { Text("Authorization Code") },
                placeholder = { Text("Enter code") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Code") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                isError = codeError.isNotEmpty(),
                supportingText = if (codeError.isNotEmpty()) { 
                    { Text(codeError, color = MaterialTheme.colorScheme.error) } 
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )
            
            Button(
                onClick = { proceedToSignUp() },
                enabled = !isLoading && authorizationCode.isNotEmpty(),
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
                    Text("Proceed to Sign Up", fontSize = 16.sp)
                }
            }
            
            Text(
                text = "Don't have an authorization code? Contact your instructor.",
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            TextButton(
                onClick = {
                    val intent = Intent(context, SignInActivity::class.java)
                    context.startActivity(intent)
                    (context as ComponentActivity).finish()
                }
            ) {
                Text("Back to Sign In")
            }
        }
    }
}
