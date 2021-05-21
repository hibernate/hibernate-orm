#!/bin/bash


SCRIPT_DIR=$(cd `dirname $0` && pwd)
IMG_NAME=${NUODB_IMAGE:-"nuodb/nuodb-ce:latest"}
DB_NAME=hibernate_orm_test
NET_NAME=nuodb-net

echo "Running up a NuoDB container for testing using ${IMG_NAME}"
echo "Database will be ${hibernate_orm_test}, DBA user and password are also ${hibernate_orm_test}"

# Cleaning up
echo "Running sudo rm -fr "$SCRIPT_DIR"/{vol1,vol2}"
sudo rm -fr "$SCRIPT_DIR"/{vol1,vol2}
mkdir "$SCRIPT_DIR"/{vol1,vol2}
chmod a+rw "$SCRIPT_DIR"/{vol1,vol2}

# Remove any previous container
docker rm -f ad1

echo "Create ${NET_NAME} network?"
XXX=$(docker network ls | grep ${NET_NAME})

if [ ${#XXX} == 0 ]; then
  echo "Not found, creating ${NET_NAME}"
  docker network create ${NET_NAME}
else
  echo "${NET_NAME} already exist"
fi

# Create a container running an AP
echo "Create NuoDB container"
docker run -d --name ad1 \
    --hostname ad1 \
    --net ${NET_NAME} \
    -p 48004-48010:48004-48010 \
    -p 8888:8888 \
    -e "NUODB_DOMAIN_ENTRYPOINT=ad1" \
    "${IMG_NAME}" nuoadmin

sleep 3

# Check AP is running
docker run -t --net "${NET_NAME}" "${IMG_NAME}" nuocmd --api-server ad1:8888 show domain

# Create archive
echo "Creating archive for ${DB_NAME}"
docker run -t --net "${NET_NAME}" "${IMG_NAME}" \
    nuocmd --api-server ad1:8888 create archive --db-name "${DB_NAME}" --server-id ad1 --archive-path /var/opt/nuodb/archive/hibernate_orm_test

# Create database
echo "Creating database ${DB_NAME}"
docker run -t --net "${NET_NAME}" "${IMG_NAME}" \
    nuocmd --api-server ad1:8888 create database --db-name "${DB_NAME}" --dba-user "${DB_NAME}" --dba-password "${DB_NAME}" --te-server-ids ad1

sleep 3
# Check Database is running
docker run -t --net "${NET_NAME}" "${IMG_NAME}" \
    nuocmd --api-server ad1:8888 show domain

echo "You must add ad1 to your /etc/hosts file, something like this:"
echo "127.0.0.1       localhost ad1"
