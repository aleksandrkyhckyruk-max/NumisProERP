package com.numisproerp.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.numisproerp.data.dao.BundleDao
import com.numisproerp.data.dao.CatalogDao
import com.numisproerp.data.dao.ClientDao
import com.numisproerp.data.dao.OtherExpenseDao
import com.numisproerp.data.dao.ProductDao
import com.numisproerp.data.dao.PurchaseDao
import com.numisproerp.data.dao.SaleDao
import com.numisproerp.data.dao.SupplierDao
import com.numisproerp.data.dao.WriteoffDao
import com.numisproerp.data.dao.CollectionItemDao
import com.numisproerp.data.dao.NoteDao
import com.numisproerp.data.entities.Bundle
import com.numisproerp.data.entities.BundleComponent
import com.numisproerp.data.entities.CatalogItem
import com.numisproerp.data.entities.Client
import com.numisproerp.data.entities.OtherExpense
import com.numisproerp.data.entities.Product
import com.numisproerp.data.entities.Purchase
import com.numisproerp.data.entities.Sale
import com.numisproerp.data.entities.Supplier
import com.numisproerp.data.entities.Writeoff
import com.numisproerp.data.entities.CollectionItem
import com.numisproerp.data.entities.Note

@Database(
    entities = [
        Product::class,
        Client::class,
        Supplier::class,
        Purchase::class,
        Sale::class,
        OtherExpense::class,
        CatalogItem::class,
        Writeoff::class,
        CollectionItem::class,
        Note::class,
        Bundle::class,
        BundleComponent::class
    ],
    version = 18,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun clientDao(): ClientDao
    abstract fun supplierDao(): SupplierDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun saleDao(): SaleDao
    abstract fun otherExpenseDao(): OtherExpenseDao
    abstract fun catalogDao(): CatalogDao
    abstract fun writeoffDao(): WriteoffDao
    abstract fun collectionItemDao(): CollectionItemDao
    abstract fun noteDao(): NoteDao
    abstract fun bundleDao(): BundleDao

    companion object {
        /**
         * Реєстр міграцій між версіями схеми БД.
         *
         * Поточна базова версія — 11 (з цього моменту схема експортується у
         * `app/schemas/com.numisproerp.data.database.AppDatabase/11.json`).
         *
         * При будь-якій майбутній зміні entities:
         *   1) збільшити `version` у `@Database`,
         *   2) додати у цей масив новий `Migration(from, to) { db -> ... }`.
         *
         * Не використовуй більше `fallbackToDestructiveMigration()` — він знищує
         * дані користувача. Якщо абсолютно немає сенсу мігрувати — додавай
         * `fallbackToDestructiveMigrationFrom(versionN)` тільки на конкретні
         * старі версії.
         */
        val MIGRATIONS: Array<Migration> = arrayOf(
            // 11 → 12: додано таблицю writeoffs (списання товарів зі складу).
            object : Migration(11, 12) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `writeoffs` (
                            `writeoffId` TEXT NOT NULL,
                            `date` INTEGER NOT NULL,
                            `catalogId` TEXT NOT NULL,
                            `quantity` INTEGER NOT NULL,
                            `pricePerUnit` REAL NOT NULL,
                            `totalAmount` REAL NOT NULL,
                            `reason` TEXT NOT NULL,
                            `comment` TEXT NOT NULL,
                            PRIMARY KEY(`writeoffId`)
                        )
                        """.trimIndent()
                    )
                }
            },
            // 12 → 13: додано таблицю collection_items (Моя колекція, п. 12-13 ТЗ).
            object : Migration(12, 13) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `collection_items` (
                            `collectionId` TEXT NOT NULL,
                            `name` TEXT NOT NULL,
                            `series` TEXT NOT NULL,
                            `category` TEXT NOT NULL,
                            `material` TEXT NOT NULL,
                            `nominal` TEXT NOT NULL,
                            `quality` TEXT NOT NULL,
                            `description` TEXT NOT NULL,
                            `photoPath` TEXT NOT NULL,
                            `estimatedValue` REAL NOT NULL,
                            `quantity` INTEGER NOT NULL,
                            `dateAdded` INTEGER NOT NULL,
                            PRIMARY KEY(`collectionId`)
                        )
                        """.trimIndent()
                    )
                }
            },
            // 13 → 14: додано таблицю notes (Мої замітки з нагадуваннями).
            object : Migration(13, 14) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `notes` (
                            `noteId` TEXT NOT NULL,
                            `title` TEXT NOT NULL,
                            `text` TEXT NOT NULL,
                            `reminderDate` INTEGER,
                            `isCompleted` INTEGER NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            PRIMARY KEY(`noteId`)
                        )
                        """.trimIndent()
                    )
                }
            },
            // 14 → 15: додано колонку attachments до notes для прикріплення
            // PDF/Excel-файлів (зберігається як `name|uri\nname|uri`).
            object : Migration(14, 15) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE `notes` ADD COLUMN `attachments` TEXT NOT NULL DEFAULT ''"
                    )
                }
            },
            // 15 → 16: додано колонку sourceUrl до collection_items
            // — посилання на зовнішній ресурс (фото/відео в браузері).
            object : Migration(15, 16) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE `collection_items` ADD COLUMN `sourceUrl` TEXT NOT NULL DEFAULT ''"
                    )
                }
            },
            // 16 → 17: додано «Моя збірка» — таблиця bundles + bundle_components.
            object : Migration(16, 17) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `bundles` (
                            `bundleId` TEXT NOT NULL,
                            `name` TEXT NOT NULL,
                            `assembledDate` INTEGER NOT NULL,
                            `totalCost` REAL NOT NULL,
                            `suggestedPrice` REAL NOT NULL,
                            `photoPath` TEXT NOT NULL,
                            `comment` TEXT NOT NULL,
                            PRIMARY KEY(`bundleId`)
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `bundle_components` (
                            `bundleComponentId` TEXT NOT NULL,
                            `bundleId` TEXT NOT NULL,
                            `componentCatalogId` TEXT NOT NULL,
                            `quantity` INTEGER NOT NULL,
                            `unitCost` REAL NOT NULL,
                            PRIMARY KEY(`bundleComponentId`),
                            FOREIGN KEY(`bundleId`) REFERENCES `bundles`(`bundleId`)
                                ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_bundle_components_bundleId` ON `bundle_components` (`bundleId`)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_bundle_components_componentCatalogId` ON `bundle_components` (`componentCatalogId`)"
                    )
                }
            },
            // 17 → 18: позначка `isBundleOp` для purchases і writeoffs.
            // Записи з покажчиком true (P_BUNDLE_*, WO_BUNDLE_*) — це внутрішні
            // складські перетворення «Моя збірка», а не реальні
            // закупівлі/списання. Вони мають впливати на залишок, але НЕ
            // з'являтися в історії та звітах.
            object : Migration(17, 18) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE `purchases` ADD COLUMN `isBundleOp` INTEGER NOT NULL DEFAULT 0"
                    )
                    db.execSQL(
                        "ALTER TABLE `writeoffs` ADD COLUMN `isBundleOp` INTEGER NOT NULL DEFAULT 0"
                    )
                    // Перенесення вже створених «збіркових» записів старими версіями.
                    db.execSQL(
                        "UPDATE `purchases` SET `isBundleOp` = 1 WHERE `purchaseId` LIKE 'P_BUNDLE_%'"
                    )
                    db.execSQL(
                        "UPDATE `writeoffs` SET `isBundleOp` = 1 WHERE `writeoffId` LIKE 'WO_BUNDLE_%'"
                    )
                }
            }
        )
    }
}
