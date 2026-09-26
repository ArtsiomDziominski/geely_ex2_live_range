# Keep reflective Car API access.
-keep class android.car.** { *; }
-dontwarn android.car.**

# R8 нужен ради оптимизаций (рантайм Compose), а не ради переименования классов:
# без обфускации стектрейсы из logcat ГУ читаются без mapping.txt.
-dontobfuscate
