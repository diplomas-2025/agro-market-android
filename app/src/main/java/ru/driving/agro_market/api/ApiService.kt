package ru.driving.agro_market.api

import android.content.Context
import android.content.SharedPreferences
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import ru.driving.agro_market.R

// Модели данных
data class SignUpParams(
    val username: String,
    val email: String,
    val password: String
)

data class JwtResponseDto(
    val userId: Int,
    val accessToken: String,
    val refreshToken: String,
    val isAdmin: Boolean,
    val username: String
)

data class SignInParams(
    val email: String,
    val password: String
)

data class ProductEntityDto(
    val id: Int,
    val name: String,
    val description: String,
    val price: Double,
    val stock: Int,
    val image: String,
    val countInCart: Int,
    val categoryId: Int,
    val favorite: Boolean
) {
    fun getImageResource(): Int {
        return when(id) {
            15 -> R.drawable.img_1
            9 -> R.drawable.img_2
            16 -> R.drawable.img_3
            5 -> R.drawable.img_4
            else -> R.drawable.img
        }
    }
}

data class ProductEntityDetailsDto(
    val product: ProductEntityDto,
    val reviews: List<ReviewEntityDto>
)

data class ReviewEntityDto(
    val id: Int,
    val user: UserEntityDto,
    val rating: Int,
    val comment: String,
    val createdAt: String
) {
    fun createdAt(): String {
        try {
            // Парсим строку в объект Date
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(createdAt)

            // Форматируем дату в нужный формат
            val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm")

            // Устанавливаем временную зону, если необходимо
            sdf.timeZone = TimeZone.getTimeZone("UTC")

            // Преобразуем в строку
            return sdf.format(date)
        }catch (_: Exception) {
            return createdAt
        }
    }

}

data class UserEntityDto(
    val id: Int,
    val username: String
)

data class OrderEntityDetailsDto(
    val id: Int,
    val user: UserEntityDto,
    val totalPrice: Double,
    val status: OrderStatus,
    val createdAt: String,
    val address: String,
    val phone: String,
    val orderItems: List<OrderItemEntityDto>
) {
    fun createdAt(): String {
        // Парсим строку с микросекундами, но учитываем только миллисекунды
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")

        val outputFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
        outputFormat.timeZone = TimeZone.getTimeZone("UTC")

        val date = inputFormat.parse(createdAt) ?: return createdAt  // На случай ошибки парсинга
        return outputFormat.format(date)
    }
}

enum class OrderStatus {
    CREATED,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    fun toRussian(): String {
        return when (this) {
            CREATED -> "Создан"
            SHIPPED -> "Отправлен"
            DELIVERED -> "Доставлен"
            CANCELLED -> "Отменен"
        }
    }
}


data class OrderItemEntityDto(
    val id: Int,
    val product: ProductEntityDto,
    val quantity: Int,
    val price: Double
)

data class CategoryEntityDto(
    val id: Int,
    val name: String
)

data class CartEntityDto(
    val id: Int,
    val product: ProductEntityDto,
    val quantity: Int
)

// Интерфейс Retrofit
interface ApiService {

    @POST("users/security/sign-up")
    suspend fun signUp(@Body signUpParams: SignUpParams): Response<JwtResponseDto>

    @POST("users/security/sign-in")
    suspend fun signIn(@Body signInParams: SignInParams): Response<JwtResponseDto>

    @GET("base/products")
    suspend fun getAllProducts(): Response<List<ProductEntityDto>>

    @GET("base/products/{id}")
    suspend fun getProductById(@Path("id") id: Int): Response<ProductEntityDetailsDto>

    @POST("base/products/{id}/cart")
    suspend fun addToCart(
        @Path("id") id: Int,
        @Query("quantity") quantity: Int
    ): Response<Void>

    @PUT("base/products/{id}/cart")
    suspend fun updateQuantityCart(
        @Path("id") id: Int,
        @Query("quantity") quantity: Int
    ): Response<Void>

    @POST("base/products/{id}/review")
    suspend fun createProductReview(
        @Path("id") id: Int,
        @Query("rating") rating: Int,
        @Query("comment") comment: String
    ): Response<Void>

    @POST("base/products/{id}/favorite")
    suspend fun updateFavorite(@Path("id") id: Int): Response<Void>

    @GET("base/orders")
    suspend fun getAllOrders(): Response<List<OrderEntityDetailsDto>>

    @POST("base/orders")
    suspend fun createOrder(
        @Query("address") address: String,
        @Query("phone") phone: String
    ): Response<OrderEntityDetailsDto>

    @PATCH("base/orders/{id}")
    suspend fun updateStatusOrder(
        @Path("id") id: Int,
        @Query("status") status: OrderStatus
    ): Response<Void>

    @GET("base/orders/user")
    suspend fun getAllUserOrders(): Response<List<OrderEntityDetailsDto>>

    @GET("base/categories")
    suspend fun getAllCategories(): Response<List<CategoryEntityDto>>

    @GET("base/carts")
    suspend fun getAllCarts(): Response<List<CartEntityDto>>
}

class SharedPrefManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("user", Context.MODE_PRIVATE)

    fun saveAccessToken(token: String?) {
        sharedPreferences.edit().putString("access_token", token).apply()
    }

    fun saveUsername(username: String) {
        sharedPreferences.edit().putString("username", username).apply()
    }

    fun saveIsAdmin(isAdmin: Boolean) {
        sharedPreferences.edit().putBoolean("is_admin", isAdmin).apply()
    }

    fun savaUserId(userId: Int) {
        sharedPreferences.edit().putInt("user_id", userId).apply()
    }

    fun getIsAdmin(): Boolean {
        return sharedPreferences.getBoolean("is_admin", false)
    }

    fun getAccessToken(): String? {
        return sharedPreferences.getString("access_token", null)
    }

    fun getUserId(): Int {
        return sharedPreferences.getInt("user_id", 0)
    }

    fun getUsername(): String {
        return sharedPreferences.getString("username", "") ?: ""
    }
}

object RetrofitClient {

    private const val BASE_URL = "https://spotdiff.ru/agro-market/"

    private lateinit var apiService: ApiService

    fun initialize(context: Context) {
        val sharedPrefManager = SharedPrefManager(context)

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                val accessToken = sharedPrefManager.getAccessToken()
                if (accessToken != null) {
                    request.addHeader("Authorization", "Bearer $accessToken")
                }
                chain.proceed(request.build())
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)
    }

    fun getApiService(): ApiService {
        if (!::apiService.isInitialized) {
            throw IllegalStateException("RetrofitClient is not initialized. Call initialize() first.")
        }
        return apiService
    }
}