package com.numisproerp.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * «Моя збірка» — готова продукція, складена з декількох товарів на складі
 * (наприклад «Готовий бокс під роли ЗСУ» з коробки + пластини + капсули + колби).
 *
 * Логіка створення:
 *  1. Користувач обирає компоненти зі складу та їхню кількість.
 *  2. Система рахує `totalCost` = sum(qty × avgPurchasePrice) по компонентах.
 *  3. Для кожного компонента вставляється `Writeoff` (reason = "Збірка: <name>")
 *     — стандартний механізм списання зі складу.
 *  4. Створюється новий `Product` (catalogId = "BUNDLE_<bundleId>", category = "Збірка").
 *  5. Створюється `Purchase` для цього нового продукту з ціною = `totalCost`,
 *     щоб збірка опинилася на складі як 1 шт. готового товару.
 *  6. Подальший продаж — як для звичайного товару; прибуток = (ціна продажу − totalCost).
 *
 * [suggestedPrice] — пропонована користувачем ціна продажу (інформаційна).
 * Реальна ціна фіксується у момент продажу через звичайний механізм Sale.
 */
@Entity(tableName = "bundles")
data class Bundle(
    @PrimaryKey
    val bundleId: String,
    val name: String,
    val assembledDate: Long,
    val totalCost: Double,
    val suggestedPrice: Double = 0.0,
    val photoPath: String = "",
    val comment: String = ""
)

/**
 * Один компонент збірки: яка позиція з каталогу, скільки одиниць пішло, за
 * якою закупочною ціною (зафіксована на момент збірки).
 *
 * `bundleId` має FK на `bundles` з `CASCADE`, щоб видалення збірки чистило і
 * її склад компонентів.
 */
@Entity(
    tableName = "bundle_components",
    foreignKeys = [
        ForeignKey(
            entity = Bundle::class,
            parentColumns = ["bundleId"],
            childColumns = ["bundleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("bundleId"), Index("componentCatalogId")]
)
data class BundleComponent(
    @PrimaryKey
    val bundleComponentId: String,
    val bundleId: String,
    val componentCatalogId: String,
    val quantity: Int,
    val unitCost: Double
)
