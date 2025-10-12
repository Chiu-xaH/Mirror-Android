# 基础设置 不混淆，方便复现
-dontobfuscate
-dontoptimize
-keepattributes SourceFile,LineNumberTable,*Annotation*

# Compose / Kotlin / 协程
-keep class androidx.compose.** { *; }
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.**