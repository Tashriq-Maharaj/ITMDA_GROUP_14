package com.halohome.iotresearch.data.api

import android.content.Context
import android.content.SharedPreferences
import com.halohome.iotresearch.HaloHomeApplication
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    private const val PREFS_NAME = "haloguard_prefs"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_AUTH_TOKEN = "auth_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_PARTICIPANT_ID = "participant_id"

    // Default loopback for Android Studio emulator. Can be changed in app settings.
    const val DEFAULT_BASE_URL = "http://10.0.2.2:5000/"

    private val prefs: SharedPreferences by lazy {
        HaloHomeApplication.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
        set(value) {
            val formatted = if (value.endsWith("/")) value else "$value/"
            prefs.edit().putString(KEY_BASE_URL, formatted).apply()
            retrofitInstance = null
        }

    var authToken: String?
        get() = prefs.getString(KEY_AUTH_TOKEN, null)
        set(value) {
            prefs.edit().putString(KEY_AUTH_TOKEN, value).apply()
        }

    var userId: Int
        get() = prefs.getInt(KEY_USER_ID, -1)
        set(value) = prefs.edit().putInt(KEY_USER_ID, value).apply()

    var participantId: String
        get() = prefs.getString(KEY_PARTICIPANT_ID, "P-001") ?: "P-001"
        set(value) = prefs.edit().putString(KEY_PARTICIPANT_ID, value).apply()

    fun clearSession() {
        prefs.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_PARTICIPANT_ID)
            .apply()
    }

    private class AuthInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val original = chain.request()
            val builder = original.newBuilder()
            authToken?.let { token ->
                if (token.isNotBlank()) {
                    builder.addHeader("Authorization", "Bearer $token")
                }
            }
            builder.addHeader("Accept", "application/json")
            builder.addHeader("Content-Type", "application/json")
            return chain.proceed(builder.build())
        }
    }

    private var retrofitInstance: Retrofit? = null

    val apiService: ApiService
        get() {
            if (retrofitInstance == null) {
                val logging = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }

                val client = OkHttpClient.Builder()
                    .addInterceptor(AuthInterceptor())
                    .addInterceptor(logging)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .build()

                retrofitInstance = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
            }
            return retrofitInstance!!.create(ApiService::class.java)
        }
}
