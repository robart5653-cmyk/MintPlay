# MintPlay app-specific R8 rules.
# Keep this file minimal: broad -keep rules prevent shrinking, optimization,
# and obfuscation. Add a rule only when a runtime feature (such as reflection
# or JNI) genuinely requires it.

# Preserve useful source line information in release crash stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
