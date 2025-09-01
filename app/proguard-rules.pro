-keep class android.** { *; }
-keep interface android.** { *; }
-dontwarn java.lang.**
-dontusemixedcaseclassnames
-keep class com.coara.txtphoto.MainActivity {
    public static void main(java.lang.String[]);
}
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}
-adaptresourcefilecontents **.xml
-adaptresourcefilenames **.png
-classobfuscationdictionary obfuscation-dictionary.txt
-renamesourcefileattribute SourceFile
-keepattributes Exceptions, InnerClasses, Signature, Deprecated, EnclosingMethod, Record, PermittedSubclasses, NestHost, NestMembers, Module, ModuleMainClass
-optimizationpasses 20
-mergeinterfacesaggressively
-adaptclassstrings
-repackageclasses ''
-keepclassmembers class * {
    void addTextChangedListener(android.text.TextWatcher);
}
-allowaccessmodification
-dontpreverify

