package ru.driving.agro_market.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import ru.driving.agro_market.api.CartEntityDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    cartItems: SnapshotStateList<CartEntityDto>, // Список товаров в корзине
    onUpdateQuantity: (productId: Int, quantity: Int) -> Unit, // Колбэк для обновления количества
    onRemoveItem: (productId: Int) -> Unit, // Колбэк для удаления товара
    onCheckout: (String, String) -> Unit // Колбэк для оформления заказа
) {
    var totalAmount by remember { mutableDoubleStateOf(0.0) }
    var showCheckoutDialog by remember { mutableStateOf(false) } // Состояние для отображения диалога оформления заказа

    // Вычисляем общую сумму
    LaunchedEffect(cartItems) {
        totalAmount = cartItems.sumOf { it.product.price * it.quantity }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Корзина") }
            )
        },
        bottomBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Общая сумма: ${String.format("%.2f", totalAmount)} ₽",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        showCheckoutDialog = true
                    },
                    enabled = cartItems.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4A3FC6),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Оформить заказ")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (cartItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Корзина пуста",
                        fontSize = 18.sp,
                        color = Color.Gray
                    )
                }
            } else {
                cartItems.forEach { cartItem ->
                    CartItem(
                        cartItem = cartItem,
                        onUpdateQuantity = { quantity ->
                            onUpdateQuantity(cartItem.product.id, quantity)
                            totalAmount = cartItems.sumOf { it.product.price * it.quantity }
                        },
                        onRemoveItem = {
                            onRemoveItem(cartItem.product.id)
                            totalAmount = cartItems.sumOf { it.product.price * it.quantity }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    if (showCheckoutDialog) {
        CheckoutDialog(
            onDismiss = { showCheckoutDialog = false },
            onConfirm = { address, phone ->
                onCheckout(address, phone)
                showCheckoutDialog = false
                totalAmount = 0.0
            }
        )
    }
}

@Composable
fun CheckoutDialog(
    onDismiss: () -> Unit,
    onConfirm: (address: String, phone: String) -> Unit
) {
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Оформление заказа") },
        text = {
            Column {
                // Поле для адреса
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Адрес доставки") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4A3FC6),
                        unfocusedBorderColor = Color.Gray
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Поле для номера телефона
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Номер телефона") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4A3FC6),
                        unfocusedBorderColor = Color.Gray
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (address.isNotBlank() && phone.isNotBlank()) {
                        onConfirm(address, phone)
                    }
                }
            ) {
                Text(text = "Подтвердить")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(text = "Отмена")
            }
        }
    )
}

@Composable
fun CartItem(
    cartItem: CartEntityDto,
    onUpdateQuantity: (quantity: Int) -> Unit,
    onRemoveItem: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Изображение товара
            Image(
                painter = painterResource(cartItem.product.getImageResource()),
                contentDescription = cartItem.product.name,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Информация о товаре
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cartItem.product.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${cartItem.product.price} ₽",
                    fontSize = 14.sp,
                    color = Color(0xFF4A3FC6)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Количество: ${cartItem.quantity}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            // Управление количеством и удаление
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (cartItem.quantity > 1) {
                                onUpdateQuantity(cartItem.quantity - 1)
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Уменьшить количество",
                            tint = Color(0xFF4A3FC6)
                        )
                    }

                    Text(
                        text = cartItem.quantity.toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4A3FC6)
                    )

                    IconButton(
                        onClick = {
                            if (cartItem.quantity < cartItem.product.stock) {
                                onUpdateQuantity(cartItem.quantity + 1)
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Увеличить количество",
                            tint = Color(0xFF4A3FC6)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                IconButton(
                    onClick = onRemoveItem,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = Color.Red
                    )
                }
            }
        }
    }
}