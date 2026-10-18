package com.appropriate.riskauditor.core.rules

import com.appropriate.riskauditor.core.model.SpecialAccessState

/**
 * Plain-language explanation generator adhering strictly to Section 7.4:
 * 1. Second person, no jargon, <= 2 sentences each.
 * 2. Mentions the app label in every explanation.
 * 3. Never uses "malware", "virus", or "definitely".
 * 4. All explanations <= 240 characters.
 */
object Explanations {

    data class Text(
        val title: String,
        val explanation: String,
        val recommendation: String
    )

    fun saAccessibility(app: String, state: SpecialAccessState): Text = when (state) {
        SpecialAccessState.ENABLED -> Text(
            title = "Can read and control your screen",
            explanation = "$app has an accessibility service turned on. This lets an app read what is on your screen and tap buttons for you, which abusive apps exploit.",
            recommendation = "If you don't need $app for accessibility, turn it off in Accessibility settings."
        )
        SpecialAccessState.UNKNOWN -> Text(
            title = "Accessibility service status unverified",
            explanation = "Android did not tell us whether this is on for $app. If enabled, it could inspect screens and perform actions on your behalf.",
            recommendation = "Check Accessibility settings to verify whether $app is turned on."
        )
        else -> Text(
            title = "Asks for screen control",
            explanation = "$app asks for accessibility access, but you haven't allowed it. If turned on, it could inspect screen contents.",
            recommendation = "Keep accessibility turned off unless essential for assistive needs."
        )
    }

    fun saDeviceAdmin(app: String, state: SpecialAccessState): Text = when (state) {
        SpecialAccessState.ENABLED -> Text(
            title = "Holds device administrator privileges",
            explanation = "$app is active as a device administrator. This allows an app to lock your device, erase data, or prevent uninstallation.",
            recommendation = "Review Device Administrators in Security settings and remove $app if not needed."
        )
        SpecialAccessState.UNKNOWN -> Text(
            title = "Device admin status unverified",
            explanation = "Android did not tell us whether this is on for $app. Active admin privileges could enforce device lock or wipe policies.",
            recommendation = "Check Device Administrators in Security settings."
        )
        else -> Text(
            title = "Asks for device administrator",
            explanation = "$app asks for device administrator access, but you haven't allowed it. This access could control device lock rules.",
            recommendation = "Do not activate device admin privileges for $app."
        )
    }

    fun saNotifListener(app: String, state: SpecialAccessState): Text = when (state) {
        SpecialAccessState.ENABLED -> Text(
            title = "Can read your notifications",
            explanation = "$app can read all incoming notifications on this phone. This includes verification codes sent by banks and messaging apps.",
            recommendation = "Turn off notification access for $app in Notification settings."
        )
        SpecialAccessState.UNKNOWN -> Text(
            title = "Notification access unverified",
            explanation = "Android did not tell us whether this is on for $app. Notification access could read two-factor codes and messages.",
            recommendation = "Review Notification Access in system settings."
        )
        else -> Text(
            title = "Asks to read notifications",
            explanation = "$app asks for notification access, but you haven't allowed it. This could read incoming one-time verification codes.",
            recommendation = "Keep notification access disabled for $app."
        )
    }

    fun saOverlay(app: String, state: SpecialAccessState): Text = when (state) {
        SpecialAccessState.ENABLED -> Text(
            title = "Can display over other apps",
            explanation = "$app can draw floating windows over other apps. This capability could be used to obscure alerts or display deceptive overlays.",
            recommendation = "Turn off Display Over Other Apps for $app in Special App Access."
        )
        SpecialAccessState.UNKNOWN -> Text(
            title = "Overlay permission unverified",
            explanation = "Android did not tell us whether this is on for $app. Drawing over apps could place floating interfaces on top of screens.",
            recommendation = "Review Display Over Other Apps in system settings."
        )
        else -> Text(
            title = "Asks to display over other apps",
            explanation = "$app asks for overlay permissions, but you haven't allowed it. Overlays could draw windows above your active apps.",
            recommendation = "Keep display over other apps disabled for $app."
        )
    }

    fun saInstallPackages(app: String, state: SpecialAccessState): Text = when (state) {
        SpecialAccessState.ENABLED -> Text(
            title = "Can install unknown apps",
            explanation = "$app is permitted to prompt the installation of new app packages. This could trigger installation of untracked software.",
            recommendation = "Turn off Install Unknown Apps for $app in Special App Access."
        )
        SpecialAccessState.UNKNOWN -> Text(
            title = "Package installation unverified",
            explanation = "Android did not tell us whether this is on for $app. If enabled, it could initiate installation of new packages.",
            recommendation = "Check Install Unknown Apps in Special App Access settings."
        )
        else -> Text(
            title = "Asks to install unknown apps",
            explanation = "$app asks to install packages, but you haven't allowed it. This permission would permit downloading new software.",
            recommendation = "Keep install unknown apps disabled for $app."
        )
    }

    fun saUsageStats(app: String, state: SpecialAccessState): Text = when (state) {
        SpecialAccessState.ENABLED -> Text(
            title = "Can track your app usage",
            explanation = "$app can monitor how frequently you launch and use other apps. This provides visibility into your daily app activity habits.",
            recommendation = "Turn off Usage Access for $app in system settings."
        )
        SpecialAccessState.UNKNOWN -> Text(
            title = "Usage access unverified",
            explanation = "Android did not tell us whether this is on for $app. Usage tracking could monitor app launch history.",
            recommendation = "Review Usage Access in system settings."
        )
        else -> Text(
            title = "Asks for usage tracking",
            explanation = "$app asks for usage tracking, but you haven't allowed it. This permission would reveal app launch history.",
            recommendation = "Keep usage tracking disabled for $app."
        )
    }

    fun catSms(app: String, hasGranted: Boolean, evidenceText: String): Text = if (hasGranted) {
        Text(
            title = "Can access SMS messages",
            explanation = "$app can read, send, or receive SMS messages ($evidenceText). This could expose sensitive private conversations and codes.",
            recommendation = "Review SMS permission for $app in App Settings and revoke if not essential."
        )
    } else {
        Text(
            title = "Asks for SMS permissions",
            explanation = "$app asks for SMS access ($evidenceText), but you haven't allowed it. SMS access could expose private text messages.",
            recommendation = "Keep SMS access denied for $app."
        )
    }

    fun catContactsCalls(app: String, hasGranted: Boolean, evidenceText: String): Text = if (hasGranted) {
        Text(
            title = "Can access contacts and call logs",
            explanation = "$app can access your address book and call history ($evidenceText). This could expose your personal contact network.",
            recommendation = "Review Contacts and Call Logs permissions in App Settings."
        )
    } else {
        Text(
            title = "Asks for contacts and call history",
            explanation = "$app asks for contacts or call history ($evidenceText), but you haven't allowed it.",
            recommendation = "Keep contacts and call logs denied for $app."
        )
    }

    fun catCameraMic(app: String, hasGranted: Boolean, evidenceText: String): Text = if (hasGranted) {
        Text(
            title = "Can record audio and video",
            explanation = "$app can record camera video or microphone audio ($evidenceText). This could capture private sounds and surroundings.",
            recommendation = "Revoke Camera and Microphone permissions if not actively using media capture."
        )
    } else {
        Text(
            title = "Asks for camera or microphone",
            explanation = "$app asks for camera or microphone access ($evidenceText), but you haven't allowed it.",
            recommendation = "Keep camera and microphone denied for $app."
        )
    }

    fun catLocation(app: String, hasGranted: Boolean, evidenceText: String): Text = if (hasGranted) {
        Text(
            title = "Can track your location",
            explanation = "$app can pinpoint your physical coordinates ($evidenceText). This could build a record of your location history.",
            recommendation = "Change Location permission to 'Only while using the app' or revoke it."
        )
    } else {
        Text(
            title = "Asks for location access",
            explanation = "$app asks for your physical location ($evidenceText), but you haven't allowed it.",
            recommendation = "Keep location access denied for $app."
        )
    }

    fun catPhoneIdentity(app: String, hasGranted: Boolean, evidenceText: String): Text = if (hasGranted) {
        Text(
            title = "Can access phone identity",
            explanation = "$app can read your phone state and identifiers ($evidenceText). This could track device identity and active calls.",
            recommendation = "Review Phone permissions for $app in App Settings."
        )
    } else {
        Text(
            title = "Asks for phone identity",
            explanation = "$app asks for phone state and numbers ($evidenceText), but you haven't allowed it.",
            recommendation = "Keep phone identity access denied for $app."
        )
    }

    fun catStorageMedia(app: String, hasGranted: Boolean, evidenceText: String): Text = if (hasGranted) {
        Text(
            title = "Can access photos and storage",
            explanation = "$app can read shared files, photos, and media ($evidenceText). This could expose saved personal images and documents.",
            recommendation = "Limit storage access to selected media or revoke in App Settings."
        )
    } else {
        Text(
            title = "Asks for photos and storage",
            explanation = "$app asks for file storage or media access ($evidenceText), but you haven't allowed it.",
            recommendation = "Keep storage access denied for $app."
        )
    }

    fun catBackground(app: String, hasGranted: Boolean, evidenceText: String): Text = if (hasGranted) {
        Text(
            title = "Can run continuously in background",
            explanation = "$app can start on boot and run background tasks ($evidenceText). This allows ongoing background activity.",
            recommendation = "Review background usage and battery optimization for $app."
        )
    } else {
        Text(
            title = "Asks for persistent background access",
            explanation = "$app asks to start automatically or run background tasks ($evidenceText), but you haven't allowed it.",
            recommendation = "Keep battery optimization enabled for $app."
        )
    }

    // Combinations
    fun comboAccessOverlay(app: String): Text = Text(
        title = "Can draw fake screens over other apps",
        explanation = "$app can both see your screen and draw on top of other apps. Together this is how fake login screens are shown over real apps.",
        recommendation = "Turn off Accessibility or Overlay permissions for $app immediately."
    )

    fun comboSmsNetBootSideload(app: String, isSideloaded: Boolean): Text = if (isSideloaded) {
        Text(
            title = "Can read texts, send them out, and restart itself",
            explanation = "$app was installed outside an app store, can read your SMS, connect to the internet, and start itself after a reboot.",
            recommendation = "Uninstall $app unless you fully trust its origin."
        )
    } else {
        Text(
            title = "Can read texts, connect online, and restart itself",
            explanation = "$app can read text messages, connect to the internet, and start itself on boot. Origin couldn't be fully confirmed.",
            recommendation = "Review SMS and Background permissions for $app."
        )
    }

    fun comboAdminNonMgmt(app: String): Text = Text(
        title = "Holds unmanaged device administrator access",
        explanation = "$app holds device admin privileges without being registered as a work profile or device owner. This grants deep device control.",
        recommendation = "Deactivate Device Administrator privileges for $app in Security settings."
    )

    fun comboNotifNet(app: String): Text = Text(
        title = "Can read notifications and connect to internet",
        explanation = "$app can read notifications containing two-factor codes and transmit data online. This combination can intercept messages.",
        recommendation = "Disable notification access for $app in system settings."
    )

    fun comboDropper(app: String): Text = Text(
        title = "Can install apps with elevated screen privileges",
        explanation = "$app was installed outside a store, can request package installations, and holds screen control capabilities.",
        recommendation = "Revoke Install Unknown Apps permission for $app."
    )

    fun comboSurveillance(app: String): Text = Text(
        title = "Surveillance capability combination",
        explanation = "$app combines background location, camera or audio capture, internet access, and automatic restart on boot.",
        recommendation = "Revoke Background Location and Microphone permissions for $app."
    )

    fun comboHarvest(app: String): Text = Text(
        title = "Can collect and transmit personal records",
        explanation = "$app can read contacts or call logs alongside text messages, with internet access available to transmit collected data.",
        recommendation = "Revoke Contacts and SMS permissions in App Settings."
    )

    fun ctxMismatch(app: String): Text = Text(
        title = "Suspicious sensitive access for utility app",
        explanation = "$app appears to be a simple utility based on its name, yet asks for sensitive personal data or device privileges.",
        recommendation = "Verify why a utility app requires personal records or screen control."
    )

    fun provSideloaded(app: String): Text = Text(
        title = "Installed outside an official app store",
        explanation = "$app was installed outside an app store and holds active sensitive permissions or special access capabilities.",
        recommendation = "Confirm that you trust the source from which $app was downloaded."
    )
}
