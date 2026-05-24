package com.numisproerp.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.numisproerp.ui.i18n.tr
import com.numisproerp.ui.theme.AccentOrange
import com.numisproerp.ui.theme.AccentRed
import com.numisproerp.ui.theme.IOSDesign
import com.numisproerp.ui.viewmodel.NotificationsViewModel
import com.numisproerp.ui.viewmodel.NotificationItem
import com.numisproerp.ui.viewmodel.NotificationSeverity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    navController: NavHostController,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val notifications by viewModel.notifications.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = tr("Назад", "Back"),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = tr("Сповіщення", "Notifications"),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            // Дві іконки в правому куті: «Очистити всі видимі» (DeleteSweep) і
            // «Повернути приховані» (Restore). Друга має сенс, лише коли є що
            // повертати — інакше зайва іконка лише захаращує панель.
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (notifications.isNotEmpty()) {
                    IconButton(onClick = {
                        viewModel.dismissAll(notifications.map { it.id })
                    }) {
                        Icon(
                            Icons.Filled.DeleteSweep,
                            contentDescription = tr("Очистити всі", "Clear all"),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = { viewModel.restoreAll() }) {
                    Icon(
                        Icons.Filled.Restore,
                        contentDescription = tr("Повернути приховані", "Restore dismissed"),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }

        if (notifications.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 72.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = tr("Сповіщень немає", "No notifications"),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tr(
                        "Свайпніть сповіщення, щоб приховати його. Воно зʼявиться знову, коли стан складу зміниться.",
                        "Swipe a notification to dismiss it. It will reappear when stock changes."
                    ),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 72.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notifications, key = { it.id }) { notification ->
                    SwipeableNotificationCard(
                        notification = notification,
                        onDismiss = { viewModel.dismiss(notification.id) }
                    )
                }
            }
        }
    }
}

/**
 * Загортає [NotificationCard] у [SwipeToDismissBox] з підтримкою свайпу
 * в обидва боки. Фоном при свайпі — червоний прямокутник з іконкою кошика,
 * щоб користувач бачив, що дія — «видалити з UI», а не «архівувати».
 *
 * Після завершення анімації свайпу викликаємо `onDismiss()`. Жест підтримує
 * лише видимий рух у спокій (`Settled`) — коротка прокрутка повертає картку
 * на місце і не змінює стан.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableNotificationCard(
    notification: NotificationItem,
    onDismiss: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { target ->
            target == SwipeToDismissBoxValue.StartToEnd ||
                target == SwipeToDismissBoxValue.EndToStart
        }
    )
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            onDismiss()
        }
    }
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { SwipeDismissBackground() },
        content = { NotificationCard(notification = notification, onDelete = onDismiss) }
    )
}

@Composable
private fun SwipeDismissBackground() {
    // fillMaxSize щоб червоний фон покрив усю висоту картки — інакше при
    // більших шрифтах або довгих описах текстовий блок виходить вище фону.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(IOSDesign.CardCornerRadius))
            .background(AccentRed.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color.White
            )
            Icon(
                Icons.Filled.Delete,
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color.White
            )
        }
    }
}

@Composable
private fun NotificationCard(notification: NotificationItem, onDelete: (() -> Unit)? = null) {
    val (icon, tint) = when (notification.severity) {
        NotificationSeverity.CRITICAL -> Icons.Outlined.ErrorOutline to AccentRed
        NotificationSeverity.WARNING -> Icons.Outlined.WarningAmber to AccentOrange
    }
    val titleText = tr(notification.titleUa, notification.titleEn)
    val descText = tr(notification.descriptionUa, notification.descriptionEn)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(IOSDesign.CardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = IOSDesign.CardElevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titleText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = descText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            if (onDelete != null) {
                // Явна кнопка «Видалити»: окрім свайпу — щоб користувач не мусив
                // вгадувати жест. Натискання остаточно прибирає поточне сповіщення
                // (з відповідним id) у dismissed-сет.
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = tr("Видалити", "Delete"),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
