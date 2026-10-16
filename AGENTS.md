# Project: PS-01 On-Device App Risk Auditor (Android, Kotlin, Compose)
Package: com.ps01.riskauditor   minSdk 26   targetSdk 34   compileSdk 35   JDK 17

## Rules
- Only edit files you own (see docs/IMPLEMENTATION_PLAN.md Section 3). If you need a change elsewhere, stop and report.
- Do not change anything under core/model/ or the interfaces in core/contract/. Those are frozen contracts.
- No network code. The app declares NO INTERNET permission. No analytics, no cloud, no LLM calls.
- Never log package names, app lists, or scan results. No Log.d of inventory data.
- Unknown is not safe. Use UNKNOWN states; never default an unreadable value to DISABLED/DENIED/safe.
- Never crash a scan because one app fails: wrap per-app work in try/catch, add a warning, continue.
- Never block the main thread. Scanning runs on Dispatchers.Default/IO.
- Scoring is deterministic and lives only in core/rules. UI never computes scores. Scanner never computes scores.
- After each task run the command in "Done when" and show its output.
- Kotlin only. No Hilt/Dagger (manual DI via AppContainer). No Java.

## Android API facts (use these, do not improvise)
- Installed apps: PackageManager.getInstalledPackages(PackageInfoFlags.of(GET_PERMISSIONS.toLong())) on API 33+;
  deprecated getInstalledPackages(int) below 33. Branch on Build.VERSION.SDK_INT.
- Declared permissions: packageInfo.requestedPermissions (nullable). Granted: packageInfo.requestedPermissionsFlags[i]
  and PackageInfo.REQUESTED_PERMISSION_GRANTED (value 2). Declared != granted.
- System app: (flags and FLAG_SYSTEM) != 0 && (flags and FLAG_UPDATED_SYSTEM_APP) == 0.
- Installer: API 30+ pm.getInstallSourceInfo(pkg).installingPackageName; below 30 pm.getInstallerPackageName(pkg).
- Protection level: pm.getPermissionInfo(name, 0); API 28+ use .protection (PROTECTION_NORMAL/DANGEROUS/SIGNATURE) ;
  NameNotFoundException => UNKNOWN. Cache results per permission name for the whole scan.
- Accessibility: AccessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK) = enabled;
  AccessibilityManager.getInstalledAccessibilityServiceList() = declared. Package = it.resolveInfo.serviceInfo.packageName.
- Device admin: DevicePolicyManager.getActiveAdmins() (nullable) = active. isDeviceOwnerApp(pkg)/isProfileOwnerApp(pkg) = managed.
  Declared: queryBroadcastReceivers(Intent("android.app.action.DEVICE_ADMIN_ENABLED")) with receiver permission BIND_DEVICE_ADMIN.
- Notification listener: NotificationManagerCompat.getEnabledListenerPackages(context) = enabled.
  Declared: queryIntentServices(Intent("android.service.notification.NotificationListenerService")).
- Overlay / install-unknown-apps / usage-stats / all-files access for OTHER apps: AppOpsManager
  (unsafeCheckOpNoThrow on API 29+, checkOpNoThrow below), op strings "android:system_alert_window",
  "android:request_install_packages", "android:get_usage_stats", "android:manage_external_storage".
  MODE_ALLOWED => ENABLED; MODE_IGNORED/MODE_ERRORED => DISABLED; MODE_DEFAULT or any exception => UNKNOWN.
- Settings.canDrawOverlays / canRequestPackageInstalls only answer for the calling app. Never use them for other apps.
- Uninstall: Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")); needs REQUEST_DELETE_PACKAGES in manifest.
  Always user-confirmed by the system dialog. Never silent.
- App settings: Settings.ACTION_APPLICATION_DETAILS_SETTINGS with Uri "package:$pkg".
- Special-access screens: ACTION_ACCESSIBILITY_SETTINGS, ACTION_NOTIFICATION_LISTENER_SETTINGS, ACTION_USAGE_ACCESS_SETTINGS,
  ACTION_MANAGE_OVERLAY_PERMISSION (+package uri), ACTION_MANAGE_UNKNOWN_APP_SOURCES (+package uri), ACTION_SECURITY_SETTINGS (device admin).
  Wrap every startActivity in try/catch(ActivityNotFoundException) and fall back to app details.
- Icons: pm.getApplicationIcon(pkg) on a background dispatcher; convert with Drawable.toBitmap(96, 96). Never store icons in the DB.
- PDF: android.graphics.pdf.PdfDocument, A4 = 595x842 pt. Share with FileProvider + FLAG_GRANT_READ_URI_PERMISSION.
