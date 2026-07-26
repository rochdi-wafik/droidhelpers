# ***********************************************************************
#   ProGuard-Rules
# ***********************************************************************
# - For a library module, proguard-rules.pro takes effect only if:
#    -> minifyEnabled is set to true on the library module itself
#
# - NOTE: shrinkResources is NOT applicable to library modules.
#   It is only supported in application modules (com.android.application).
#
# - NOTE: This file is intentionally left empty.
#   shared-rules.pro is used instead, and is declared in build.gradle
#   via proguardFiles, to avoid duplicating rules across both files.
# ***********************************************************************