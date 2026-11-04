# Sanitizes user home on persistent (self-hosted) runners

# We use the GitHub cache for the relevant parts of these directories.
# Also, we do not want to keep things like ~/.gradle/build-scan-data.
rm -rf ~/.gradle/
rm -rf ~/.m2/
