package com.numisproerp.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalAtm
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.numisproerp.R
import com.numisproerp.ui.navigation.Screen
import com.numisproerp.ui.theme.AccentBlue
import com.numisproerp.ui.theme.AccentGreen
import com.numisproerp.ui.theme.AccentOrange
import com.numisproerp.ui.theme.AccentPurple
import com.numisproerp.ui.theme.AccentRed
import com.numisproerp.ui.theme.AccentTeal

/**
 * Опис однієї дії, яку можна розмістити на плитці швидкого доступу головного
 * екрана. `id` — стабільний ключ, який зберігається в SettingsManager (порядок
 * плиток і користувацькі назви). Має збігатися зі значеннями, які раніше
 * передавалися як `tileId` в [QuickAccessButton] (`purchase`, `sale`, ...).
 *
 * Поля для рендерингу:
 *  - [labelUa] / [labelEn] — стандартна назва, якщо користувач не задав власну;
 *  - [route] — куди веде натискання на плитку. Передається в `onNavigate`;
 *  - [icon] — векторна іконка fallback для тем без власних PNG;
 *  - [tileRes], [lightTileRes], [premiumTileRes] — кастомні PNG плитки для тем
 *    OLEG_SMILE / OLEG_SMILE_LIGHT / OLEG_SMILE_PREMIUM. `null` — fallback на [icon];
 *  - [lightTint] — колір контурної іконки в OLEG_SMILE_LIGHT (та в DEFAULT як
 *    акцент-колір IOSIconChip).
 */
data class QuickAccessAction(
    val id: String,
    val labelUa: String,
    val labelEn: String,
    val route: String,
    val icon: ImageVector,
    val tileRes: Int? = null,
    val lightTileRes: Int? = null,
    val premiumTileRes: Int? = null,
    val lightTint: Color = Color.Unspecified
)

/**
 * Каталог усіх доступних дій для плиток головного меню. Користувач у Налаштуваннях
 * → «Інтерфейс» → «Плитки головного меню» може вибрати, які з них розмістити на
 * робочому столі та в якому порядку. Деякі id (purchase/sale/stock/clients/
 * suppliers/collection) існували й раніше; усі інші — нові, додані разом із
 * редактором плиток.
 *
 * Якщо id у SharedPreferences невідомий — він просто ігнорується при рендерингу.
 */
object QuickAccessActionRegistry {
    val all: List<QuickAccessAction> = listOf(
        QuickAccessAction(
            id = "purchase",
            labelUa = "Закупівля",
            labelEn = "Purchase",
            route = Screen.Purchase.route,
            icon = Icons.Outlined.LocalAtm,
            tileRes = R.drawable.tile_purchase,
            lightTileRes = R.drawable.tile_light_purchase,
            premiumTileRes = R.drawable.tile_premium_purchase,
            lightTint = AccentOrange
        ),
        QuickAccessAction(
            id = "sale",
            labelUa = "Продаж",
            labelEn = "Sale",
            route = Screen.Sale.route,
            icon = Icons.Filled.ShoppingCart,
            tileRes = R.drawable.tile_sale,
            lightTileRes = R.drawable.tile_light_sale,
            premiumTileRes = R.drawable.tile_premium_sale,
            lightTint = AccentGreen
        ),
        QuickAccessAction(
            id = "stock",
            labelUa = "Склад",
            labelEn = "Stock",
            route = Screen.Stock.route,
            icon = Icons.Filled.Store,
            tileRes = R.drawable.tile_stock,
            lightTileRes = R.drawable.tile_light_stock,
            premiumTileRes = R.drawable.tile_premium_stock,
            lightTint = AccentBlue
        ),
        QuickAccessAction(
            id = "clients",
            labelUa = "Клієнти",
            labelEn = "Clients",
            route = Screen.Clients.route,
            icon = Icons.Filled.People,
            tileRes = R.drawable.tile_clients,
            lightTileRes = R.drawable.tile_light_clients,
            premiumTileRes = R.drawable.tile_premium_clients,
            lightTint = AccentTeal
        ),
        QuickAccessAction(
            id = "suppliers",
            labelUa = "Постачальники",
            labelEn = "Suppliers",
            route = Screen.Suppliers.route,
            icon = Icons.Filled.People,
            tileRes = R.drawable.tile_suppliers,
            lightTileRes = R.drawable.tile_light_suppliers,
            premiumTileRes = R.drawable.tile_premium_suppliers,
            lightTint = AccentPurple
        ),
        QuickAccessAction(
            id = "collection",
            labelUa = "Моя колекція",
            labelEn = "Collection",
            route = Screen.MyCollection.route,
            icon = Icons.Outlined.BarChart,
            tileRes = R.drawable.tile_collection,
            lightTileRes = R.drawable.tile_light_collection,
            premiumTileRes = R.drawable.tile_premium_collection,
            lightTint = AccentBlue
        ),
        QuickAccessAction(
            id = "reports",
            labelUa = "Звіти",
            labelEn = "Reports",
            route = Screen.Reports.route,
            icon = Icons.Outlined.BarChart,
            lightTint = AccentBlue
        ),
        QuickAccessAction(
            id = "expenses",
            labelUa = "Витрати",
            labelEn = "Expenses",
            route = Screen.Expenses.route,
            icon = Icons.Outlined.Receipt,
            lightTint = AccentOrange
        ),
        QuickAccessAction(
            id = "documents",
            labelUa = "Документи",
            labelEn = "Documents",
            route = Screen.Documents.route,
            icon = Icons.Outlined.Description,
            lightTint = AccentTeal
        ),
        QuickAccessAction(
            id = "products",
            labelUa = "Товари",
            labelEn = "Products",
            route = Screen.Products.route,
            icon = Icons.Outlined.Inventory2,
            lightTint = AccentBlue
        ),
        QuickAccessAction(
            id = "catalog",
            labelUa = "Каталог",
            labelEn = "Catalog",
            route = Screen.Catalog.route,
            icon = Icons.Filled.Store,
            lightTint = AccentTeal
        ),
        QuickAccessAction(
            id = "bundle",
            labelUa = "Моя збірка",
            labelEn = "My bundle",
            route = Screen.MyBundle.route,
            icon = Icons.Filled.Build,
            lightTint = AccentPurple
        ),
        QuickAccessAction(
            id = "writeoff",
            labelUa = "Списання",
            labelEn = "Writeoff",
            route = Screen.Writeoff.route,
            icon = Icons.Outlined.Delete,
            lightTint = AccentRed
        ),
        QuickAccessAction(
            id = "history",
            labelUa = "Історія",
            labelEn = "History",
            route = Screen.History.route,
            icon = Icons.Outlined.History,
            lightTint = AccentBlue
        ),
        QuickAccessAction(
            id = "sales_history",
            labelUa = "Історія продажів",
            labelEn = "Sales history",
            route = Screen.SalesHistory.route,
            icon = Icons.Outlined.Sell,
            lightTint = AccentGreen
        ),
        QuickAccessAction(
            id = "add_product",
            labelUa = "Додати товар",
            labelEn = "Add product",
            route = Screen.Products.routeWithOpenAdd(true),
            icon = Icons.Filled.Add,
            lightTint = AccentGreen
        ),
        QuickAccessAction(
            id = "my_notes",
            labelUa = "Мої замітки",
            labelEn = "My notes",
            route = Screen.MyNotes.route,
            icon = Icons.Filled.Edit,
            lightTint = AccentOrange
        ),
        QuickAccessAction(
            id = "notifications",
            labelUa = "Сповіщення",
            labelEn = "Notifications",
            route = Screen.Notifications.route,
            icon = Icons.Filled.Notifications,
            lightTint = AccentRed
        ),
        QuickAccessAction(
            id = "help",
            labelUa = "Допомога",
            labelEn = "Help",
            route = Screen.Help.route,
            icon = Icons.AutoMirrored.Outlined.Help,
            lightTint = AccentBlue
        ),
        QuickAccessAction(
            id = "settings",
            labelUa = "Налаштування",
            labelEn = "Settings",
            route = Screen.Settings.route,
            icon = Icons.Filled.Settings,
            lightTint = AccentBlue
        )
    )

    private val byId: Map<String, QuickAccessAction> = all.associateBy { it.id }

    fun findById(id: String): QuickAccessAction? = byId[id]
}
