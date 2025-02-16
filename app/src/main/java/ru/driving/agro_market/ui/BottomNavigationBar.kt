package ru.driving.agro_market.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun BottomNavigationBar(onUpdateRoute: (BottomNavItem) -> Unit) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Cart,
        BottomNavItem.Orders
    )

    var selectedItem by rememberSaveable { mutableIntStateOf(0) }
    val primaryColor = Color(0xFF4A3FC6)
    val backgroundColor = Color(0xFFF5F5F5)
    val selectedTextColor = Color.White
    val unselectedTextColor = Color(0xFF333333)

    NavigationBar(containerColor = backgroundColor) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        item.icon,
                        contentDescription = item.title,
                        tint = if (selectedItem == index) primaryColor else unselectedTextColor
                    )
                },
                label = {
                    Text(
                        item.title,
                        color = if (selectedItem == index) primaryColor else unselectedTextColor
                    )
                },
                selected = selectedItem == index,
                onClick = {
                    selectedItem = index
                    onUpdateRoute(item)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = selectedTextColor,
                    unselectedIconColor = unselectedTextColor,
                    selectedTextColor = primaryColor,
                    unselectedTextColor = unselectedTextColor,
                    indicatorColor = primaryColor.copy(alpha = 0.2f) // Полупрозрачный фон при выборе
                )
            )
        }
    }
}

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Home : BottomNavItem("home", "Главная", Icons.Filled.Home)
    object Cart : BottomNavItem("cart", "Корзина", Icons.Filled.ShoppingCart)
    object Orders : BottomNavItem("orders", "Заказы", Icons.Filled.List)
}
