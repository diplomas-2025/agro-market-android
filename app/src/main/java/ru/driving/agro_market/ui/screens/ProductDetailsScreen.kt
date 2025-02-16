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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import ru.driving.agro_market.api.ProductEntityDetailsDto
import ru.driving.agro_market.api.ProductEntityDto
import ru.driving.agro_market.api.RetrofitClient
import ru.driving.agro_market.api.ReviewEntityDto
import ru.driving.agro_market.api.SharedPrefManager
import ru.driving.agro_market.api.UserEntityDto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

@Composable
fun ProductDetailsScreen(
    id: Int,
    navController: NavController,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var product by remember { mutableStateOf<ProductEntityDetailsDto?>(null) }
    val reviews = remember { mutableStateListOf<ReviewEntityDto>() }
    val sharedPrefManager = remember { SharedPrefManager(context) }
    var isVisCreateReview by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        product = RetrofitClient.getApiService().getProductById(id).body()
        product?.let {
            reviews.clear()
            reviews.addAll(it.reviews)
        }
        isVisCreateReview = !reviews.any { it.user.id == sharedPrefManager.getUserId() }
    }

    product?.let { product ->
        ProductDetailsScreen(
            product = product.product,
            reviews = reviews,
            isVisCreateReview = isVisCreateReview,
            onBackPressed = {
                navController.navigateUp()
            },
            onAddToCart = { product, isNew ->
                scope.launch {
                    if (isNew) {
                        RetrofitClient.getApiService().addToCart(product.id, 1)
                    } else {
                        RetrofitClient.getApiService()
                            .updateQuantityCart(product.id, product.countInCart)
                    }
                }
            },
            onFavoriteClick = {
                scope.launch {
                    RetrofitClient.getApiService().updateFavorite(it.id)
                }
            },
            onAddReview = { rating, comment ->
                scope.launch {
                    RetrofitClient.getApiService().createProductReview(
                        id = id,
                        rating = rating,
                        comment = comment
                    )

                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                    sdf.timeZone = TimeZone.getTimeZone("UTC")

                    reviews.add(
                        ReviewEntityDto(
                            id = 0,
                            comment = comment,
                            rating = rating,
                            user = UserEntityDto(
                                id = 0,
                                username = sharedPrefManager.getUsername()
                            ),
                            createdAt = sdf.format(Date())
                        )
                    )
                    isVisCreateReview = false
                }
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailsScreen(
    product: ProductEntityDto,
    isVisCreateReview: Boolean,
    reviews: List<ReviewEntityDto>,
    onBackPressed: () -> Unit,
    onAddToCart: (ProductEntityDto, Boolean) -> Unit,
    onFavoriteClick: (ProductEntityDto) -> Unit,
    onAddReview: (rating: Int, comment: String) -> Unit, // Новый колбэк для добавления отзыва
) {
    var countInCart by remember { mutableIntStateOf(product.countInCart) }
    var isFavorite by remember { mutableStateOf(product.favorite) }
    var showAddReviewDialog by remember { mutableStateOf(false) } // Состояние для отображения диалога добавления отзыва

    // Вычисляем средний рейтинг
    val averageRating = if (reviews.isNotEmpty()) {
        reviews.map { it.rating }.average().toFloat()
    } else {
        0f
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = product.name) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Изображение продукта
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                Image(
                    painter = painterResource(product.getImageResource()),
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Название и цена
            Text(
                text = product.name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${product.price} ₽",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF4A3FC6)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Описание продукта
            Text(
                text = product.description,
                fontSize = 16.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Управление количеством и добавление в корзину
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
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
                                onAddToCart(product.copy(countInCart = countInCart), false)
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
                            onClick = {
                                if (countInCart < product.stock) countInCart++
                                onAddToCart(product.copy(countInCart = countInCart), false)
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
                } else {
                    Button(
                        onClick = {
                            countInCart = 1
                            onAddToCart(product.copy(countInCart = countInCart), true)
                        },
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

                // Кнопка "Избранное"
                IconButton(
                    onClick = {
                        isFavorite = !isFavorite
                        onFavoriteClick(product.copy(favorite = isFavorite))
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isFavorite) Color(0xFFFFC107) else Color(0xFFE0E0E0))
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Избранное",
                        tint = if (isFavorite) Color.Red else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Отзывы
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Отзывы",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                // Средний рейтинг
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Средняя оценка:",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    RatingBar(rating = averageRating.toInt())
                    Text(
                        text = "(${String.format("%.1f", averageRating)})",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (isVisCreateReview) {
                Button(
                    onClick = { showAddReviewDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4A3FC6),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "Добавить отзыв")
                }

            }
            // Кнопка "Добавить отзыв"

            Spacer(modifier = Modifier.height(16.dp))

            if (reviews.isEmpty()) {
                Text(
                    text = "Отзывов пока нет.",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Column {
                    reviews.forEach { review ->
                        ReviewItem(review = review)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    // Диалог для добавления отзыва
    if (showAddReviewDialog) {
        AddReviewDialog(
            onDismiss = { showAddReviewDialog = false },
            onAddReview = { rating, comment ->
                onAddReview(rating, comment)
                showAddReviewDialog = false
            }
        )
    }
}

@Composable
fun ReviewItem(review: ReviewEntityDto) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Имя пользователя и рейтинг
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = review.user.username,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                RatingBar(rating = review.rating)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Комментарий
            Text(
                text = review.comment,
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Дата отзыва
            Text(
                text = "Опубликовано: ${review.createdAt()}",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun RatingBar(rating: Int) {
    Row {
        repeat(5) { index ->
            Icon(
                imageVector = if (index < rating) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "Рейтинг",
                tint = if (index < rating) Color(0xFFFFC107) else Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun AddReviewDialog(
    onDismiss: () -> Unit,
    onAddReview: (rating: Int, comment: String) -> Unit,
) {
    var rating by remember { mutableIntStateOf(0) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Добавить отзыв") },
        text = {
            Column {
                Text(text = "Оценка:")
                RatingBarEditable(rating = rating, onRatingChange = { rating = it })
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Комментарий:")
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp), // Закругленные углы
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4A3FC6), // Цвет рамки при фокусе
                        unfocusedBorderColor = Color.Gray, // Цвет рамки без фокуса
                        focusedLabelColor = Color(0xFF4A3FC6), // Цвет лейбла при фокусе
                        unfocusedLabelColor = Color.Gray, // Цвет лейбла без фокуса
                        cursorColor = Color(0xFF4A3FC6) // Цвет курсора
                    ),
                    label = { Text("Ваш комментарий") } // Лейбл
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (rating > 0 && comment.isNotBlank()) {
                        onAddReview(rating, comment)
                    }
                }
            ) {
                Text(text = "Добавить")
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
fun RatingBarEditable(
    rating: Int,
    onRatingChange: (Int) -> Unit,
) {
    Row {
        repeat(5) { index ->
            IconButton(
                onClick = { onRatingChange(index + 1) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (index < rating) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Рейтинг",
                    tint = if (index < rating) Color(0xFFFFC107) else Color.Gray
                )
            }
        }
    }
}

