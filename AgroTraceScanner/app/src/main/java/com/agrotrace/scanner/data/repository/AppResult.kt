package com.agrotrace.scanner.data.repository

sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Failure(val message: String, val statusCode: Int? = null) : AppResult<Nothing>
}
