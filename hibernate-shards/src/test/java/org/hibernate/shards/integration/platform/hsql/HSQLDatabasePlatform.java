/**
 * Copyright (C) 2007 Google Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package org.hibernate.shards.integration.platform.hsql;

import org.hibernate.shards.integration.IdGenType;
import org.hibernate.shards.integration.platform.BaseDatabasePlatform;
import org.hibernate.shards.integration.platform.DatabasePlatform;
import org.hibernate.shards.util.Lists;
import org.hsqldb.jdbcDriver;

import java.util.List;

/**
 * @author maxr@google.com (Max Ross)
 */
public class HSQLDatabasePlatform extends BaseDatabasePlatform {

  private static final String DRIVER_CLASS = jdbcDriver.class.getName();
  private static final String IN_MEMORY_DB_URL_PREFIX = "jdbc:hsqldb:mem:";
  private static final String IN_MEMORY_DB_USER = "sa";
  private static final String IN_MEMORY_DB_PASSWORD = "";

  protected static final DatabasePlatform PLATFORM = new HSQLDatabasePlatform();


  private static final Iterable<String> CREATE_TABLE_STATEMENTS = Lists.newArrayList(
     "CREATE TABLE sample_table (id DECIMAL(40,0) PRIMARY KEY, str_col VARCHAR(256))"
    ,"CREATE TABLE sample_table2 (id DECIMAL(40,0) PRIMARY KEY, str_col VARCHAR(256))"
    ,"CREATE TABLE hibernate_unique_key (id IDENTITY, next_hi DECIMAL(40,0))"
    ,"INSERT INTO hibernate_unique_key(next_hi) VALUES(1)"
    ,"CREATE TABLE Elevator (elevatorId DECIMAL(40,0) PRIMARY KEY, buildingId DECIMAL(40,0))"
    ,"CREATE TABLE Building (buildingId DECIMAL(40,0) PRIMARY KEY, name VARCHAR(50))"
    ,"CREATE TABLE Floor (floorId DECIMAL(40,0) PRIMARY KEY, buildingId DECIMAL(40,0), upEscalatorId DECIMAL(40,0), downEscalatorId DECIMAL(40,0), number DECIMAL(40,0), squareFeet DECIMAL(40, 4))"
    ,"CREATE TABLE Tenant (tenantId DECIMAL(40,0) PRIMARY KEY, name VARCHAR(50))"
    ,"CREATE TABLE BuildingTenant (buildingId DECIMAL(40,0), tenantId DECIMAL(40,0), PRIMARY KEY(buildingId, tenantId))"
    ,"CREATE TABLE Office (officeId DECIMAL(40,0) PRIMARY KEY, floorId DECIMAL(40,0), label VARCHAR(50), windowId DECIMAL(40,0))"
    ,"CREATE TABLE Window (windowId DECIMAL(40,0) PRIMARY KEY, opens DECIMAL(5,0))"
    ,"CREATE TABLE FloorElevator (floorId DECIMAL(40,0), elevatorId DECIMAL(40,0), PRIMARY KEY(floorId, elevatorId))"
    ,"CREATE TABLE Escalator (escalatorId DECIMAL(40,0) PRIMARY KEY, bottomFloorId DECIMAL(40,0), topFloorId DECIMAL(40,0))"
    ,"CREATE TABLE Person (personId DECIMAL(40,0) PRIMARY KEY, name VARCHAR(50), tenantId DECIMAL(40,0), officeId DECIMAL(40,0))"
    ,"CREATE TABLE IdIsBaseType (idIsBaseTypeId DECIMAL(40,0) PRIMARY KEY, value VARCHAR(50))"
    ,"CREATE TABLE WEATHER_REPORT (REPORT_ID DECIMAL(40,0) PRIMARY KEY, CONTINENT VARCHAR(50), LATITUDE DECIMAL(10,2), LONGITUDE DECIMAL(10,2), TEMPERATURE DECIMAL(10,0), REPORT_TIME TIMESTAMP)"
  );

  protected static final List<String> DROP_TABLE_STATEMENTS = Lists.newArrayList(
      "SHUTDOWN" // this deletes the whole database!
  );

  protected HSQLDatabasePlatform() {
  }

  public Iterable<String> getCreateTableStatements(IdGenType idGenType) {
    return CREATE_TABLE_STATEMENTS;
  }

  public Iterable<String> getDropTableStatements(IdGenType idGenType) {
    return DROP_TABLE_STATEMENTS;
  }

  public static DatabasePlatform getInstance() {
    return PLATFORM;
  }

  public String getUrl(int index) {
    return IN_MEMORY_DB_URL_PREFIX + "shard" + index;
  }

  public String getUser() {
    return IN_MEMORY_DB_USER;
  }

  public String getPassword() {
    return IN_MEMORY_DB_PASSWORD;
  }

  public String getName() {
    return "hsql";
  }

  @Override
  protected String getDriverClass() {
    return DRIVER_CLASS;
  }
}
