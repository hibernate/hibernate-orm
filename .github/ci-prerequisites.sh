# Reclaim disk space, otherwise we only have 13 GB free at the start of a job

echo 'Before the cleanup:'
df -h
# Remove the container images for node:
echo 'Docker images (available for possible removal):'
docker images
# That is 18 GB
sudo rm -rf /usr/share/dotnet
# That is 1.2 GB
sudo rm -rf /usr/share/swift

echo 'After the cleanup:'
df -h
