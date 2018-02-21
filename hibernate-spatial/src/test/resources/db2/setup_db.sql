-- pagesize of 8k needed for enable_db operation
create database sample pagesize 8 k;
connect to sample;

-- spatially-enable the database; create spatial types, functions and stored procedures
!db2se enable_db sample;

-- create the spatial reference system for EPSG 4326 (WGS84)
-- this is the same as the Spatial Extender default srid 1003
!db2se drop_srs sample
 -srsName EPSG4326
 ;

!db2se create_srs sample
 -srsName EPSG4326
 -srsId   4326
 -coordsysName GCS_WGS_1984
 -xOffset      -180
 -xScale       1000000
 -yOffset      -90
 -zOffset     0
 -zScale       1
 -mOffset     0
 -mScale 1
 ;

-- Create SQL function to return EWKT format from a geometry
create or replace function db2gse.asewkt(geometry db2gse.st_geometry)
returns clob(2G)
specific db2gse.asewkt1
language sql
deterministice
no external action
reads sql data
return 'srid=' || varchar(db2gse.st_srsid(geometry)) || ';' || db2gse.st_astext(geometry)
;

-- Create SQL function to create a geometry from EWKT format
create or replace function db2gse.geomfromewkt(instring varchar(32000))
returns db2gse.st_geometry
specific db2gse.fromewkt1
language sql
deterministic
no external action
reads sql data
return db2gse.st_geometry(
substr(instring,posstr(instring,';')+1, length(instring) - posstr(instring,';')),
integer(substr(instring,posstr(instring,'=')+1,posstr(instring,';')-(posstr(instring,'=')+1)))
) 
;

-- Create a DB2 transform group to return and accept EWKT
CREATE TRANSFORM FOR db2gse.ST_Geometry EWKT (
       FROM SQL WITH FUNCTION db2gse.asewkt(db2gse.ST_Geometry), 
       TO   SQL WITH FUNCTION db2gse.geomfromewkt(varchar(32000)) )
;

-- Redefine the default DB2_PROGRAM to return and accept EWKT instead of WKT
DROP TRANSFORM DB2_PROGRAM FOR db2gse.ST_Geometry;
CREATE TRANSFORM FOR db2gse.ST_Geometry DB2_PROGRAM (
       FROM SQL WITH FUNCTION db2gse.asewkt(db2gse.ST_Geometry), 
       TO   SQL WITH FUNCTION db2gse.geomfromewkt(varchar(32000)) )
;
 
-- Give the test userid authority to create and access tables 
grant dataaccess on database to hstest;

