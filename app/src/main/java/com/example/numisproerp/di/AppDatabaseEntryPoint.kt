package com.numisproerp.di

import com.numisproerp.data.database.AppDatabase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Доступ до [AppDatabase] із не-Hilt-контекстів (наприклад, з Composable, де
 * відсутній ViewModel). Використовуй так:
 *
 * ```
 * val context = LocalContext.current.applicationContext
 * val database = remember {
 *     dagger.hilt.android.EntryPointAccessors
 *         .fromApplication(context, AppDatabaseEntryPoint::class.java)
 *         .appDatabase()
 * }
 * ```
 *
 * Прибирає прив'язку до раніше існуючого `NumisProERPApplication.getInstance()`.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppDatabaseEntryPoint {
    fun appDatabase(): AppDatabase
}
