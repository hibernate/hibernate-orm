# Reclaims disk space and sanitizes user home on Atlas infrastructure

# We use the GitHub cache for the relevant parts of these directories.
# Also, we do not want to keep things like ~/.gradle/build-scan-data.
rm -rf ~/.gradle/
rm -rf ~/.m2/
