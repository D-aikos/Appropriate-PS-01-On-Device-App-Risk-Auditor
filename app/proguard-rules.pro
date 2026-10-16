# Proguard rules for On-Device App Risk Auditor
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
