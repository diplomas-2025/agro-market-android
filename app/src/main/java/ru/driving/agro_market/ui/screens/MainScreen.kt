package ru.driving.agro_market.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import ru.driving.agro_market.api.CartEntityDto
import ru.driving.agro_market.api.CategoryEntityDto
import ru.driving.agro_market.api.OrderEntityDetailsDto
import ru.driving.agro_market.api.ProductEntityDto
import ru.driving.agro_market.api.RetrofitClient
import ru.driving.agro_market.api.SharedPrefManager
import ru.driving.agro_market.ui.BottomNavItem
import ru.driving.agro_market.ui.BottomNavigationBar

@Composable
fun MainScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val products = remember { mutableStateListOf<ProductEntityDto>() }
    var categories by remember { mutableStateOf<List<CategoryEntityDto>>(emptyList()) }
    val cartItems = remember { mutableStateListOf<CartEntityDto>() }
    var orders = remember { mutableStateListOf<OrderEntityDetailsDto>() }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var route by remember { mutableStateOf<BottomNavItem>(BottomNavItem.Home) }
    val sharedPrefManager = remember { SharedPrefManager(context) }

    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.getApiService().getAllProducts()
            if (response.isSuccessful) {
                products.clear()
                products.addAll(response.body() ?: emptyList())
            } else {
                errorMessage = "Ошибка загрузки: ${response.errorBody()?.string()}"
            }
            val responseCategories = RetrofitClient.getApiService().getAllCategories()
            categories = responseCategories.body() ?: emptyList()
            val responseCarts = RetrofitClient.getApiService().getAllCarts()
            cartItems.clear()
            cartItems.addAll(responseCarts.body() ?: emptyList())
            val responseOrders = if (sharedPrefManager.getIsAdmin())
                RetrofitClient.getApiService().getAllOrders()
            else
                RetrofitClient.getApiService().getAllUserOrders()
            orders.clear()
            orders.addAll(responseOrders.body() ?: emptyList())
        } catch (e: Exception) {
            errorMessage = "Ошибка сети: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(onUpdateRoute = {
                route = it
            })
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (route) {
                    BottomNavItem.Cart -> {
                        CartScreen(
                            cartItems = cartItems,
                            onUpdateQuantity = { id, count ->
                                scope.launch {
                                    RetrofitClient.getApiService().updateQuantityCart(
                                        id, count
                                    )
                                    cartItems.forEachIndexed { index, cartEntityDto ->
                                        if (cartEntityDto.product.id == id) {
                                            cartItems[index] = cartEntityDto.copy(quantity = count)
                                        }
                                    }
                                    products.forEachIndexed { index, productEntityDto ->
                                        if (productEntityDto.id == id) {
                                            products[index] =
                                                productEntityDto.copy(countInCart = count)
                                        }
                                    }
                                }
                            },
                            onRemoveItem = { id ->
                                scope.launch {
                                    RetrofitClient.getApiService().updateQuantityCart(
                                        id, 0
                                    )
                                    cartItems.removeIf { it.product.id == id }
                                    products.forEachIndexed { index, productEntityDto ->
                                        if (productEntityDto.id == id) {
                                            products[index] = productEntityDto.copy(countInCart = 0)
                                        }
                                    }
                                }
                            },
                            onCheckout = { address, phone ->
                                scope.launch {
                                    RetrofitClient.getApiService().createOrder(address, phone)
                                        .body()?.let {
                                        cartItems.clear()
                                        products.forEachIndexed { index, productEntityDto ->
                                            if (productEntityDto.countInCart > 0) {
                                                products[index] =
                                                    productEntityDto.copy(countInCart = 0)
                                            }
                                        }
                                        orders.add(it)
                                    }
                                }
                            }
                        )
                    }

                    BottomNavItem.Home -> {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        } else if (errorMessage != null) {
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        } else {
                            ProductGrid(
                                products = products,
                                categories = categories,
                                navController = navController,
                                onUpdateCountInCart = { product, value, index ->
                                    scope.launch {
                                        if (product.countInCart != value) {
                                            if (product.countInCart <= 0) {
                                                RetrofitClient.getApiService()
                                                    .addToCart(product.id, 1)
                                                products[index] = product.copy(countInCart = 1)
                                            } else {
                                                RetrofitClient.getApiService()
                                                    .updateQuantityCart(product.id, value)
                                                products[index] = product.copy(countInCart = value)
                                            }

                                            val responseCarts =
                                                RetrofitClient.getApiService().getAllCarts()
                                            cartItems.clear()
                                            cartItems.addAll(responseCarts.body() ?: emptyList())
                                        }
                                    }
                                },
                                onUpdateFavorite = { product, value, index ->
                                    scope.launch {
                                        if (product.favorite != value) {
                                            RetrofitClient.getApiService()
                                                .updateFavorite(product.id)
                                            products[index] = product.copy(favorite = value)
                                        }
                                    }
                                }
                            )
                        }
                    }

                    BottomNavItem.Orders -> {
                        OrderHistoryScreen(
                            orders = orders,
                            isAdmin = sharedPrefManager.getIsAdmin(),
                            onUpdateStatus = { orderId, status ->
                                scope.launch {
                                    RetrofitClient.getApiService()
                                        .updateStatusOrder(orderId, status)
                                    orders.forEachIndexed { index, dto ->
                                        if (dto.id == orderId) {
                                            orders[index] = dto.copy(status = status)
                                        }
                                    }
                                }
                            },
                            onLogout = {
                                sharedPrefManager.saveAccessToken(null)
                                navController.navigate("auth")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProductGrid(
    products: SnapshotStateList<ProductEntityDto>,
    categories: List<CategoryEntityDto>,
    navController: NavController,
    onUpdateCountInCart: (ProductEntityDto, Int, Int) -> Unit,
    onUpdateFavorite: (ProductEntityDto, Boolean, Int) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<CategoryEntityDto?>(null) }
    var sortOption by remember { mutableStateOf("Цена (по возрастанию)") }
    var showFavoritesOnly by remember { mutableStateOf(false) }

    val filteredProducts = remember { mutableStateListOf<ProductEntityDto>() }

    LaunchedEffect(products, searchQuery, selectedCategory, sortOption, showFavoritesOnly) {
        filteredProducts.clear()
        filteredProducts.addAll(
            products.filter {
                it.name.contains(searchQuery, ignoreCase = true) &&
                        (selectedCategory == null || it.categoryId == selectedCategory?.id) &&
                        (!showFavoritesOnly || it.favorite)
            }.sortedWith(
                when (sortOption) {
                    "Цена (по возрастанию)" -> compareBy { it.price }
                    "Цена (по убыванию)" -> compareByDescending { it.price }
                    "Алфавит (A-Z)" -> compareBy { it.name }
                    "Алфавит (Z-A)" -> compareByDescending { it.name }
                    else -> compareBy { it.id }
                }
            )
        )
    }


    // Сетка товаров
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(span = { GridItemSpan(2) }) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                // Поиск
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Поиск") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Поиск",
                            tint = Color(0xFF4A3FC6)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = AbsoluteRoundedCornerShape(15.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4A3FC6),
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Фильтр по категориям + кнопка "Избранное"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCategory == null,
                                onClick = { selectedCategory = null },
                                label = {
                                    Text(
                                        "Все категории",
                                        color = if (selectedCategory == null) Color.White else Color.Black
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF4A3FC6),
                                    containerColor = Color(0xFFE0E0E0)
                                )
                            )
                        }
                        items(categories) { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = {
                                    selectedCategory =
                                        if (selectedCategory == category) null else category
                                },
                                label = {
                                    Text(
                                        category.name,
                                        color = if (selectedCategory == category) Color.White else Color.Black
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF4A3FC6),
                                    containerColor = Color(0xFFE0E0E0)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Кнопка "Избранное"
                    IconButton(
                        onClick = { showFavoritesOnly = !showFavoritesOnly },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (showFavoritesOnly) Color(0xFFFFC107) else Color(
                                    0xFFE0E0E0
                                )
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Избранное",
                            tint = if (showFavoritesOnly) Color.Red else Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Сортировка
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Сортировка:", fontWeight = FontWeight.Bold, color = Color(0xFF4A3FC6))

                    DropdownMenuComponent(
                        selectedOption = sortOption,
                        options = listOf(
                            "Цена (по возрастанию)",
                            "Цена (по убыванию)",
                            "Алфавит (A-Z)",
                            "Алфавит (Z-A)"
                        )
                    ) { sortOption = it }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        itemsIndexed(filteredProducts) { index, product ->
            ProductItem(product = product, onClick = {
                navController.navigate("products/" + product.id)
            }, onUpdateFavorite = { onUpdateFavorite(product, it, index) },
                onUpdateCountInCart = {
                    onUpdateCountInCart(product, it, index)
                }
            )
        }
    }

}

@Composable
fun DropdownMenuComponent(
    selectedOption: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Button(
            onClick = { expanded = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A3FC6))
        ) {
            Text(selectedOption, color = Color.White)
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = "Открыть меню",
                tint = Color.White
            )
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = Color.Black) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ProductItem(
    product: ProductEntityDto, onClick: () -> Unit,
    onUpdateCountInCart: (Int) -> Unit,
    onUpdateFavorite: (Boolean) -> Unit,
) {
    var isFavorite by remember { mutableStateOf(product.favorite) }
    var countInCart by remember { mutableIntStateOf(product.countInCart) }

    LaunchedEffect(isFavorite) {
        onUpdateFavorite(isFavorite)
    }

    LaunchedEffect(countInCart) {
        onUpdateCountInCart(countInCart)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                Image(
                    painter = painterResource(product.getImageResource()),
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                )

                IconButton(
                    onClick = { isFavorite = !isFavorite },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.White.copy(alpha = 0.85f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Избранное",
                        tint = if (isFavorite) Color(0xFFFF5A5F) else Color(0xFFB0B0B0)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = product.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = product.description,
                fontSize = 14.sp,
                color = Color.Gray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${product.price} ₽",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4A3FC6)
            )

            Spacer(Modifier.height(5.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (countInCart > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (countInCart > 0) countInCart--
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
                            text = countInCart.toString(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4A3FC6)
                        )

                        IconButton(
                            onClick = { if (countInCart < product.stock) countInCart++ },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Увеличить количество",
                                tint = Color(0xFF4A3FC6)
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = { countInCart = 1 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4A3FC6),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "В корзину",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "В корзину")
                    }
                }
            }
        }
    }
}