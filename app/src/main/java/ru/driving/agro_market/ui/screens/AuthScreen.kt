package ru.driving.agro_market.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import ru.driving.agro_market.R
import ru.driving.agro_market.api.JwtResponseDto
import ru.driving.agro_market.api.RetrofitClient
import ru.driving.agro_market.api.SharedPrefManager
import ru.driving.agro_market.api.SignInParams
import ru.driving.agro_market.api.SignUpParams

@Composable
fun AuthScreen(navController: NavController) {
    val context = LocalContext.current
    val sharedPrefManager = remember { SharedPrefManager(context) }
    var showSignUp by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF6A11CB), Color(0xFF2575FC))
                ),
            ), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Логотип
                    Image(
                        painter = painterResource(id = R.drawable.logo), // Замените на ваш логотип
                        contentDescription = "Logo",
                        modifier = Modifier.size(120.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Анимация перехода между экранами
                    AnimatedVisibility(
                        visible = !showSignUp
                    ) {
                        SignInScreen(
                            onSignInSuccess = {
                                sharedPrefManager.saveAccessToken(it.accessToken)
                                sharedPrefManager.saveUsername(it.username)
                                sharedPrefManager.saveIsAdmin(it.isAdmin)
                                sharedPrefManager.savaUserId(it.userId)
                                navController.navigate("main")
                            },
                            onNavigateToSignUp = { showSignUp = true }
                        )
                    }

                    AnimatedVisibility(
                        visible = showSignUp
                    ) {
                        SignUpScreen(
                            onSignUpSuccess = {
                                sharedPrefManager.saveAccessToken(it.accessToken)
                                sharedPrefManager.saveUsername(it.username)
                                sharedPrefManager.saveIsAdmin(it.isAdmin)
                                sharedPrefManager.savaUserId(it.userId)
                                navController.navigate("main")
                            },
                            onNavigateToSignIn = { showSignUp = false }
                        )
                    }
                }
            }
}

@Composable
fun SignInScreen(
    onSignInSuccess: (JwtResponseDto) -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope() // Создаем scope для корутин

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Вход",
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFF333333)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Поле для email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = "Email")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6A11CB),
                    unfocusedBorderColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Поле для пароля
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = "Пароль")
                },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6A11CB),
                    unfocusedBorderColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Кнопка входа
            Button(
                onClick = {
                    coroutineScope.launch {
                        val signInParams = SignInParams(email, password)
                        try {
                            val response = RetrofitClient.getApiService().signIn(signInParams)
                            if (response.isSuccessful) {
                                onSignInSuccess(response.body()!!) // Успешный вход
                            } else {
                                errorMessage = "Ошибка входа: ${response.errorBody()?.string()}"
                            }
                        } catch (e: Exception) {
                            errorMessage = "Ошибка сети: ${e.message}"
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6A11CB),
                    contentColor = Color.White
                )
            ) {
                Text("Войти", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Ссылка на регистрацию
            Text(
                text = "Нет аккаунта? Зарегистрироваться",
                color = Color(0xFF6A11CB),
                modifier = Modifier.clickable { onNavigateToSignUp() }
            )
        }
    }
}

@Composable
fun SignUpScreen(
    onSignUpSuccess: (JwtResponseDto) -> Unit,
    onNavigateToSignIn: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Регистрация",
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFF333333)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Поле для имени пользователя
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Имя пользователя") },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = "Имя пользователя")
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6A11CB),
                    unfocusedBorderColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Поле для email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = "Email")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6A11CB),
                    unfocusedBorderColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Поле для пароля
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = "Пароль")
                },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6A11CB),
                    unfocusedBorderColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Кнопка регистрации
            Button(
                onClick = {
                    coroutineScope.launch {
                        val signUpParams = SignUpParams(username, email, password)
                        try {
                            val response = RetrofitClient.getApiService().signUp(signUpParams)
                            if (response.isSuccessful) {
                                onSignUpSuccess(response.body()!!) // Успешная регистрация
                            } else {
                                errorMessage = "Ошибка регистрации: ${response.errorBody()?.string()}"
                            }
                        } catch (e: Exception) {
                            errorMessage = "Ошибка сети: ${e.message}"
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6A11CB),
                    contentColor = Color.White
                )
            ) {
                Text("Зарегистрироваться", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Ссылка на вход
            Text(
                text = "Уже есть аккаунт? Войти",
                color = Color(0xFF6A11CB),
                modifier = Modifier.clickable { onNavigateToSignIn() }
            )
        }
    }
}