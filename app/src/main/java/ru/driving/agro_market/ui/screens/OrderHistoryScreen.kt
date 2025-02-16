package ru.driving.agro_market.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import ru.driving.agro_market.api.OrderEntityDetailsDto
import ru.driving.agro_market.api.OrderItemEntityDto
import ru.driving.agro_market.api.OrderStatus

@Composable
fun OrderHistoryScreen(orders: List<OrderEntityDetailsDto>, isAdmin: Boolean, onUpdateStatus: (Int, OrderStatus) -> Unit, onLogout: () -> Unit) {
    var expandedOrderId by remember { mutableStateOf<Int?>(null) }
    var selectedOrder by remember { mutableStateOf<OrderEntityDetailsDto?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var newStatus by remember { mutableStateOf(OrderStatus.CREATED) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Title and Logout Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "История заказов",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { onLogout() }) {
                Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
            }
        }

        // Order List
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(orders) { order ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    onClick = {
                        expandedOrderId = if (expandedOrderId == order.id) null else order.id
                    }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Заказ #${order.id}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = order.status.toRussian(),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Дата: ${order.createdAt()}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Сумма: ${order.orderItems.sumOf { it.price * it.quantity }} ₽",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )

                        AnimatedVisibility(visible = expandedOrderId == order.id) {
                            Column {
                                LazyRow(modifier = Modifier.padding(top = 8.dp)) {
                                    items(order.orderItems) { orderItem ->
                                        OrderItemCard(orderItem)
                                    }
                                }
                                if (isAdmin) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            selectedOrder = order
                                            newStatus = order.status
                                            showDialog = true
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Изменить статус")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog && selectedOrder != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Изменение статуса заказа") },
            text = {
                Column {
                    Text("Выберите новый статус:")
                    DropdownMenuBox(
                        selectedStatus = newStatus,
                        onStatusChange = { newStatus = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateStatus(selectedOrder!!.id, newStatus)
                        showDialog = false
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDialog = false }
                ) {
                    Text("Отмена")
                }
            }
        )
    }
}


@Composable
fun DropdownMenuBox(selectedStatus: OrderStatus, onStatusChange: (OrderStatus) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Button(onClick = { expanded = true }) {
            Text(selectedStatus.toRussian())
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            OrderStatus.entries.forEach { status ->
                DropdownMenuItem(
                    text = { Text(status.toRussian()) },
                    onClick = {
                        onStatusChange(status)
                        expanded = false
                    }
                )
            }
        }
    }
}


@Composable
fun OrderItemCard(orderItem: OrderItemEntityDto) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .width(140.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(orderItem.product.getImageResource()),
                contentDescription = orderItem.product.name,
                modifier = Modifier
                    .height(80.dp)
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = orderItem.product.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                text = "Цена: ${orderItem.price} ₽",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Кол-во: ${orderItem.quantity}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}