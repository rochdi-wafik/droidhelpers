# ***********************************************************************
#   Consumer-Rules
# ***********************************************************************
# - When an app includes this module, Gradle will automatically consume
#   these rules and merge them with the app's own rules, so that the
#   developer won't need to manually add ProGuard/R8 rules for this module.
#
# - NOTE: This file is intentionally left empty.
#   shared-rules.pro is used instead, and is declared in build.gradle
#   via consumerProguardFiles, so its rules are automatically passed
#   to any app that depends on this module.
# ***********************************************************************