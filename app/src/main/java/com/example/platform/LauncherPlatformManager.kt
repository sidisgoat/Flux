package com.example.platform

import android.annotation.SuppressLint
import android.app.Activity
import android.app.WallpaperManager
import android.app.role.RoleManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Process
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledAppInfo(
    val packageName: String,
    val activityName: String,
    val label: String,
    val autoCategory: String,
    val isSystemApp: Boolean,
    val firstInstallTime: Long,
    val versionName: String,
    val shortcuts: List<AppShortcutInfo> = emptyList()
)

data class AppShortcutInfo(
    val id: String,
    val shortLabel: String,
    val packageName: String
)

data class SystemSettingShortcut(
    val id: String,
    val title: String,
    val subtitle: String,
    val settingsAction: String
)

data class DeviceContactItem(
    val id: String,
    val displayName: String,
    val phoneNumber: String,
    val lookupUri: String
)

data class DeviceMediaItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val mimeType: String,
    val contentUri: String,
    val sizeBytes: Long,
    val category: String // PHOTO, MUSIC, VIDEO, DOCUMENT
)

class LauncherPlatformManager(private val context: Context) {

    private val iconCache = object : LruCache<String, ImageBitmap>(120) {}

    val systemSettingsShortcuts: List<SystemSettingShortcut> = listOf(
        SystemSettingShortcut("set_wifi", "Wi-Fi & Internet", "Network connections and hotspot", Settings.ACTION_WIFI_SETTINGS),
        SystemSettingShortcut("set_bt", "Bluetooth Devices", "Paired audio & peripherals", Settings.ACTION_BLUETOOTH_SETTINGS),
        SystemSettingShortcut("set_display", "Display & Brightness", "Refresh rate, dark mode & font scale", Settings.ACTION_DISPLAY_SETTINGS),
        SystemSettingShortcut("set_sound", "Sound & Vibration", "Volume levels, haptics & DND", Settings.ACTION_SOUND_SETTINGS),
        SystemSettingShortcut("set_battery", "Battery & Power", "Usage stats & battery saver", Settings.ACTION_BATTERY_SAVER_SETTINGS),
        SystemSettingShortcut("set_storage", "Storage Space", "Manage local device capacity", Settings.ACTION_INTERNAL_STORAGE_SETTINGS),
        SystemSettingShortcut("set_apps", "Installed Applications", "Permissions & default apps", Settings.ACTION_APPLICATION_SETTINGS),
        SystemSettingShortcut("set_home", "Default Home App", "Choose default Android launcher", Settings.ACTION_HOME_SETTINGS),
        SystemSettingShortcut("set_security", "Security & Privacy", "Biometrics, lock screen & permissions", Settings.ACTION_SECURITY_SETTINGS),
        SystemSettingShortcut("set_accessibility", "Accessibility", "Screen readers, motion & gestures", Settings.ACTION_ACCESSIBILITY_SETTINGS)
    )

    suspend fun loadInstalledApps(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val selfPkg = context.packageName
        val results = mutableMapOf<String, InstalledAppInfo>()

        try {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            val activityList = launcherApps?.getActivityList(null, Process.myUserHandle()).orEmpty()
            for (info in activityList) {
                val pkg = info.applicationInfo.packageName
                if (pkg == selfPkg) continue
                val label = info.label?.toString()?.ifBlank { pkg } ?: pkg
                val appInfo = info.applicationInfo
                val isSys = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val autoCat = classifyPackage(pkg, label, appInfo)
                val shortcuts = queryAppShortcuts(launcherApps, pkg)
                val verName = try {
                    pm.getPackageInfo(pkg, 0).versionName ?: "1.0"
                } catch (_: Exception) {
                    "1.0"
                }
                results[pkg] = InstalledAppInfo(
                    packageName = pkg,
                    activityName = info.componentName.className,
                    label = label,
                    autoCategory = autoCat,
                    isSystemApp = isSys,
                    firstInstallTime = info.firstInstallTime,
                    versionName = verName,
                    shortcuts = shortcuts
                )
            }
        } catch (_: Exception) {
            // Fallback to queryIntentActivities if LauncherApps fails
        }

        if (results.isEmpty()) {
            try {
                val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
                for (ri in resolveInfos) {
                    val activityInfo = ri.activityInfo ?: continue
                    val pkg = activityInfo.packageName ?: continue
                    if (pkg == selfPkg) continue
                    val label = ri.loadLabel(pm)?.toString()?.ifBlank { pkg } ?: pkg
                    val appInfo = activityInfo.applicationInfo
                    val isSys = appInfo != null && (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val autoCat = classifyPackage(pkg, label, appInfo)
                    results[pkg] = InstalledAppInfo(
                        packageName = pkg,
                        activityName = activityInfo.name ?: "",
                        label = label,
                        autoCategory = autoCat,
                        isSystemApp = isSys,
                        firstInstallTime = System.currentTimeMillis(),
                        versionName = "1.0"
                    )
                }
            } catch (_: Exception) {
            }
        }

        results.values.sortedBy { it.label.lowercase() }
    }

    private fun queryAppShortcuts(launcherApps: LauncherApps?, packageName: String): List<AppShortcutInfo> {
        if (launcherApps == null || !launcherApps.hasShortcutHostPermission()) return emptyList()
        return try {
            val query = LauncherApps.ShortcutQuery().apply {
                setPackage(packageName)
                setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                )
            }
            launcherApps.getShortcuts(query, Process.myUserHandle())
                .orEmpty()
                .take(4)
                .map { s ->
                    AppShortcutInfo(
                        id = s.id,
                        shortLabel = s.shortLabel?.toString() ?: s.longLabel?.toString() ?: "Shortcut",
                        packageName = packageName
                    )
                }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getAppIconBitmap(packageName: String): ImageBitmap? {
        iconCache.get(packageName)?.let { return it }
        return try {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            val bmp = drawableToBitmap(drawable, 128)
            val img = bmp.asImageBitmap()
            iconCache.put(packageName, img)
            img
        } catch (_: Exception) {
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable, maxPx: Int): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = drawable.intrinsicWidth.coerceIn(48, maxPx)
        val height = drawable.intrinsicHeight.coerceIn(48, maxPx)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun classifyPackage(pkg: String, label: String, appInfo: ApplicationInfo?): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appInfo != null) {
            when (appInfo.category) {
                ApplicationInfo.CATEGORY_GAME -> return "Games"
                ApplicationInfo.CATEGORY_AUDIO,
                ApplicationInfo.CATEGORY_VIDEO,
                ApplicationInfo.CATEGORY_IMAGE -> return "Media"
                ApplicationInfo.CATEGORY_SOCIAL -> return "Social"
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> return "Work"
                ApplicationInfo.CATEGORY_NEWS -> return "Study"
            }
        }
        val combined = "${pkg.lowercase()} ${label.lowercase()}"
        return when {
            combined.contains("chat") || combined.contains("messag") || combined.contains("dialer") ||
                combined.contains("phone") || combined.contains("contact") || combined.contains("social") ||
                combined.contains("whatsapp") || combined.contains("telegram") || combined.contains("instagram") -> "Social"
            combined.contains("music") || combined.contains("video") || combined.contains("photo") ||
                combined.contains("gallery") || combined.contains("camera") || combined.contains("spotify") ||
                combined.contains("youtube") || combined.contains("player") -> "Media"
            combined.contains("doc") || combined.contains("note") || combined.contains("book") ||
                combined.contains("pdf") || combined.contains("study") || combined.contains("learn") ||
                combined.contains("calc") || combined.contains("read") -> "Study"
            combined.contains("mail") || combined.contains("calendar") || combined.contains("drive") ||
                combined.contains("work") || combined.contains("office") || combined.contains("files") ||
                combined.contains("chrome") || combined.contains("browser") -> "Work"
            combined.contains("game") || combined.contains("play") || combined.contains("arcade") -> "Games"
            combined.contains("shop") || combined.contains("pay") || combined.contains("bank") ||
                combined.contains("wallet") || combined.contains("store") -> "Shopping"
            else -> "Tools"
        }
    }

    fun isFluxDefaultLauncher(): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            resolveInfo?.activityInfo?.packageName == context.packageName
        } catch (_: Exception) {
            false
        }
    }

    fun launchApp(packageName: String): Boolean {
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun launchAppShortcut(packageName: String, shortcutId: String): Boolean {
        return try {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            if (launcherApps != null && launcherApps.hasShortcutHostPermission()) {
                launcherApps.startShortcut(packageName, shortcutId, null, null, Process.myUserHandle())
                true
            } else {
                launchApp(packageName)
            }
        } catch (_: Exception) {
            launchApp(packageName)
        }
    }

    fun openAppDetailsSettings(packageName: String): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun requestUninstallApp(packageName: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openSystemSetting(action: String): Boolean {
        return try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            try {
                val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    fun openDefaultHomeSelector(activity: Activity? = null) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && activity != null) {
                val roleManager = activity.getSystemService(RoleManager::class.java)
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
                    !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
                ) {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                    activity.startActivityForResult(intent, 9401)
                    return
                }
            }
        } catch (_: Exception) {
        }
        openSystemSetting(Settings.ACTION_HOME_SETTINGS)
    }

    @SuppressLint("WrongConstant")
    fun expandNotificationShade(): Boolean {
        return try {
            val statusBarService = context.getSystemService("statusbar")
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            val expandMethod = statusBarManager.getMethod("expandNotificationsPanel")
            expandMethod.invoke(statusBarService)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openWallpaperChooser(): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SET_WALLPAPER).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Select Wallpaper").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            true
        } catch (_: Exception) {
            false
        }
    }

    fun applyProceduralFluxWallpaper(primaryColorArgb: Int, secondaryColorArgb: Int): Boolean {
        return try {
            val wm = WallpaperManager.getInstance(context)
            val bmp = Bitmap.createBitmap(720, 1600, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                shader = android.graphics.LinearGradient(
                    0f, 0f, 720f, 1600f,
                    intArrayOf(0xFF090D14.toInt(), primaryColorArgb, secondaryColorArgb, 0xFF070A0F.toInt()),
                    floatArrayOf(0f, 0.38f, 0.72f, 1f),
                    android.graphics.Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, 720f, 1600f, paint)
            wm.setBitmap(bmp)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun shareTextOrUri(title: String, content: String, mimeType: String = "text/plain"): Boolean {
        return try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = if (mimeType.isBlank()) "text/plain" else mimeType
                putExtra(Intent.EXTRA_TITLE, title)
                if (content.startsWith("content://")) {
                    putExtra(Intent.EXTRA_STREAM, Uri.parse(content))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    putExtra(Intent.EXTRA_TEXT, content)
                }
            }
            val chooser = Intent.createChooser(sendIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openUriOrWebSearch(target: String, mimeType: String = ""): Boolean {
        return try {
            val uri = when {
                target.startsWith("http://") || target.startsWith("https://") || target.startsWith("content://") ->
                    Uri.parse(target)
                target.contains(".") && !target.contains(" ") ->
                    Uri.parse("https://$target")
                else ->
                    Uri.parse("https://www.google.com/search?q=${Uri.encode(target)}")
            }
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                if (mimeType.isNotBlank() && uri.scheme == "content") {
                    setDataAndType(uri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    data = uri
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(viewIntent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun initiateCommunication(actionType: String, handleOrNumber: String): Boolean {
        return try {
            val intent = when (actionType.uppercase()) {
                "DIAL", "CALL" -> Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(handleOrNumber)}"))
                "SMS", "MESSAGE" -> Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(handleOrNumber)}"))
                "EMAIL" -> Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${Uri.encode(handleOrNumber)}"))
                else -> Intent(Intent.ACTION_VIEW, Uri.parse(handleOrNumber))
            }.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun copyToClipboard(label: String, text: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
        } catch (_: Exception) {
        }
    }

    fun readClipboardText(): String? {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val item = clipboard?.primaryClip?.getItemAt(0)
            item?.coerceToText(context)?.toString()?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    fun getDeviceBatteryPercent(): Int {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.coerceIn(1, 100) ?: 78
        } catch (_: Exception) {
            78
        }
    }

    fun discoverInstalledIconPacks(): List<Pair<String, String>> {
        return try {
            val pm = context.packageManager
            val intent = Intent("org.adw.launcher.THEMES")
            pm.queryIntentActivities(intent, 0).mapNotNull { ri ->
                val pkg = ri.activityInfo?.packageName ?: return@mapNotNull null
                val label = ri.loadLabel(pm)?.toString() ?: pkg
                pkg to label
            }.distinctBy { it.first }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun queryDeviceContactsIfPermitted(): List<DeviceContactItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<DeviceContactItem>()
        if (context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return@withContext list
        }
        try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone._ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY
            )
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val lookupIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)
                var count = 0
                while (cursor.moveToNext() && count < 40) {
                    val id = if (idIdx >= 0) cursor.getString(idIdx).orEmpty() else count.toString()
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx).orEmpty() else "Contact"
                    val number = if (numIdx >= 0) cursor.getString(numIdx).orEmpty() else ""
                    val lookup = if (lookupIdx >= 0) cursor.getString(lookupIdx).orEmpty() else ""
                    if (name.isNotBlank()) {
                        list.add(
                            DeviceContactItem(
                                id = id,
                                displayName = name,
                                phoneNumber = number,
                                lookupUri = lookup
                            )
                        )
                        count++
                    }
                }
            }
        } catch (_: Exception) {
        }
        list.distinctBy { "${it.displayName}_${it.phoneNumber}" }
    }

    fun takePersistablePermissionIfPossible(uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
        }
    }

    fun resolveDisplayNameAndSize(uri: Uri): Pair<String, String> {
        return try {
            var name = uri.lastPathSegment ?: "Selected File"
            var sizeStr = ""
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex( android.provider.OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (nameIdx >= 0) {
                        cursor.getString(nameIdx)?.takeIf { it.isNotBlank() }?.let { name = it }
                    }
                    if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                        val bytes = cursor.getLong(sizeIdx)
                        sizeStr = formatFileSize(bytes)
                    }
                }
            }
            name to sizeStr
        } catch (_: Exception) {
            (uri.lastPathSegment ?: "Document") to ""
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return ""
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format("%.1f MB", mb)
    }

    fun launchCameraIntent(): Boolean {
        return try {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
