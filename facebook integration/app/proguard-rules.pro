# Add project specific ProGuard rules here.
# By default, the active rules are defined in the SDK path.

# Maintain model mappings for Facebook Graph API JSON parses
-keepclassmembers class com.mentor.fbauth.data.model.** { *; }

# Retain AndroidX Security crypto elements
-keep class androidx.security.crypto.** { *; }
