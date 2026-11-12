# Sanitizes user home on persistent (self-hosted) runners

# We use the GitHub cache for the relevant parts of these directories.
# Also, we do not want to keep things like ~/.gradle/build-scan-data.
# Preserve wrapper/ — OTP runners ship pre-installed Gradle and Maven
# distributions there, and re-downloading them is wasteful.
find ~/.gradle/ -mindepth 1 -maxdepth 1 -not -name 'wrapper' -exec rm -rf {} + 2>/dev/null || true
find ~/.m2/ -mindepth 1 -maxdepth 1 -not -name 'wrapper' -exec rm -rf {} + 2>/dev/null || true
