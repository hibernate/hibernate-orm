#!/usr/bin/env bash

WD=$(dirname $0)

docker run -h db2server_db2_22 \
 --name db2_11  \
 -p 50000:50000  -p 55000:55000\
 --privileged=true \
 --env-file db2_11_env \
 -v ${WD}/../../../hibernate-spatial-docker-dbs/db2_11 \
 --detach ibmcom/db2express-c:latest

#The followin steps need to be executed
#(This will need to end up in a script)
#
## First connect to the docker and switch to user db2inst
# docker exec -i -t db2 /bin/bash
#
# su - db2inst1

#
## create the database with 8K pagesize , connect to it and enable
#
#db2 create database hibern8 pagesize 8 k

#db2 connect to hibern8

#db2se enable_db hibern8

## generate the ewkt.sql script:
#cat > ewkt.sql <<EOF
#create or replace function db2gse.asewkt(geometry db2gse.st_geometry)
#returns clob(2G)
#specific db2gse.asewkt1
#language sql
#deterministic
#no external action
#reads sql data
#return 'srid=' || varchar(db2gse.st_srsid(geometry)) || ';' || db2gse.st_astext(geometry);
#
#create or replace function db2gse.geomfromewkt(instring varchar(32000))
#returns db2gse.st_geometry
#specific db2gse.fromewkt1
#language sql
#deterministic
#no external action
#reads sql data
#return db2gse.st_geometry(
#substr(instring,posstr(instring,';')+1, length(instring) - posstr(instring,';')),
#integer(substr(instring,posstr(instring,'=')+1,posstr(instring,';')-(posstr(instring,'=')+1))));
#
#create transform for db2gse.st_geometry ewkt (
# from sql with function db2gse.asewkt(db2gse.st_geometry),
# to   sql with function db2gse.geomfromewkt(varchar(32000)) );
#
#drop transform db2_program for db2gse.st_geometry;
#create transform for db2gse.st_geometry db2_program (
# from sql with function db2gse.asewkt(db2gse.st_geometry),
# to   sql with function db2gse.geomfromewkt(varchar(32000)) );
#EOF

## run the ewkt.sql script (see hibernate documentation)
#db2 -tvf ./ewkt.sql


### generate the EPSPG:4326 SRS
#db2se create_srs hibern8  \
# -srsName EPSG4326  \
# -srsId   4326  \
# -coordsysName GCS_WGS_1984 \
# -xOffset      -180   \
# -xScale       1000000  \
# -yOffset      -90   \
# -zOffset     0   \
# -zScale       1 \
# -mOffset     0 \
# -mScale 1
#
