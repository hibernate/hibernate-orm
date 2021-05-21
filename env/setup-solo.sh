#!/bin/bash

# Variable declarations
DB_NAME=hibernate_orm_test
CTR_NAME=hibtest1
IMG_NAME=${NUODB_IMAGE:-"nuodb/nuodb-ce:latest"}
NET_NAME=nuodb-net

# Getting started
echo "Running up a NuoDB container '${CTR_NAME}' using ${IMG_NAME}"
echo "Database will be ${DB_NAME}, DBA user and password are also ${DB_NAME}"

# Remove any previous container
docker rm -f "${CTR_NAME}"

# Create a dedicated network if it doesn't exist already
#echo "Create ${NET_NAME} network?"
XXX=$(docker network ls | grep ${NET_NAME})

if [ ${#XXX} == 0 ]; then
  echo "Not found, creating ${NET_NAME}"
  docker network create ${NET_NAME}
else
  echo "${NET_NAME} already exists"
fi

# Create a container running an AP
echo "Create NuoDB container"
docker run -d --name ${CTR_NAME} \
    --hostname ${CTR_NAME} \
    --net ${NET_NAME} \
    -p 48004-48010:48004-48010 \
    -p 8888:8888 \
    -e "NUODB_DOMAIN_ENTRYPOINT=${CTR_NAME}" \
    "${IMG_NAME}" nuoadmin

# Wait whilst nuoadmin starts up
sleep 3

# Check AP is running
docker run -t --net "${NET_NAME}" "${IMG_NAME}" nuocmd --api-server "${CTR_NAME}:8888" show domain

# Create archive
echo "Creating archive for ${DB_NAME}"
docker run -t --net "${NET_NAME}" "${IMG_NAME}" \
    nuocmd --api-server ${CTR_NAME}:8888 create archive --db-name "${DB_NAME}" --server-id ${CTR_NAME} --archive-path "/var/opt/nuodb/archive/${DB_NAME}"

# Create database
echo "Creating database ${DB_NAME}"
docker run -t --net "${NET_NAME}" "${IMG_NAME}" \
    nuocmd --api-server ${CTR_NAME}:8888 create database --db-name "${DB_NAME}" --dba-user "${DB_NAME}" --dba-password "${DB_NAME}" --te-server-ids ${CTR_NAME}

sleep 3
# Check Database is running
docker run -t --net "${NET_NAME}" "${IMG_NAME}" \
    nuocmd --api-server "${CTR_NAME}:8888" show domain

# When you request a connection from the AP, it will return ${CTR_NAME}:48006 - the address of the TE
# Your machine needs to know that ${CTR_NAME} is mapped from localhost
echo "You must add ${CTR_NAME} to your /etc/hosts file, something like this:"
echo "127.0.0.1       localhost ${CTR_NAME}"
