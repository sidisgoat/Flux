package com.example.platform

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Process

data class AvailableWidgetProvider(
    val providerComponent: ComponentName,
    val label: String,
    val packageName: String,
    val minWidthDp: Int,
    val minHeightDp: Int,
    val info: AppWidgetProviderInfo
)

class WidgetHostHelper(context: Context) {

    private val appContext = context.applicationContext
    val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(appContext)
    val appWidgetHost: AppWidgetHost = AppWidgetHost(appContext, HOST_ID)

    private var isListening = false

    fun startListening() {
        if (!isListening) {
            try {
                appWidgetHost.startListening()
                isListening = true
            } catch (_: Exception) {
            }
        }
    }

    fun stopListening() {
        if (isListening) {
            try {
                appWidgetHost.stopListening()
                isListening = false
            } catch (_: Exception) {
            }
        }
    }

    fun discoverInstalledWidgetProviders(): List<AvailableWidgetProvider> {
        return try {
            val providers = appWidgetManager.getInstalledProvidersForProfile(Process.myUserHandle())
            providers.mapNotNull { info ->
                val provider = info.provider ?: return@mapNotNull null
                val label = info.loadLabel(appContext.packageManager)?.toString()?.ifBlank {
                    provider.shortClassName
                } ?: provider.shortClassName
                AvailableWidgetProvider(
                    providerComponent = provider,
                    label = label,
                    packageName = provider.packageName,
                    minWidthDp = info.minWidth.coerceAtLeast(110),
                    minHeightDp = info.minHeight.coerceAtLeast(110),
                    info = info
                )
            }.sortedBy { it.label.lowercase() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun allocateWidgetId(): Int {
        return try {
            appWidgetHost.allocateAppWidgetId()
        } catch (_: Exception) {
            -1
        }
    }

    fun deleteWidgetId(appWidgetId: Int) {
        if (appWidgetId >= 0) {
            try {
                appWidgetHost.deleteAppWidgetId(appWidgetId)
            } catch (_: Exception) {
            }
        }
    }

    fun tryBindWidget(appWidgetId: Int, provider: ComponentName): Boolean {
        return try {
            appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, provider)
        } catch (_: Exception) {
            false
        }
    }

    fun buildBindPermissionIntent(appWidgetId: Int, provider: ComponentName): Intent {
        return Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
        }
    }

    fun createHostView(context: Context, appWidgetId: Int, providerFlatten: String): AppWidgetHostView? {
        if (appWidgetId < 0) return null
        return try {
            val info = appWidgetManager.getAppWidgetInfo(appWidgetId)
                ?: discoverInstalledWidgetProviders()
                    .firstOrNull { it.providerComponent.flattenToString() == providerFlatten }
                    ?.info
                ?: return null
            appWidgetHost.createView(context, appWidgetId, info)
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        const val HOST_ID = 2048
    }
}
