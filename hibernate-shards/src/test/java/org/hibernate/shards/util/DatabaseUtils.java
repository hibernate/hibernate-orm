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
package org.hibernate.shards.util;

import org.hibernate.shards.integration.IdGenType;
import org.hibernate.shards.integration.platform.DatabasePlatform;
import org.hibernate.shards.integration.platform.DatabasePlatformFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @author maxr@google.com (Max Ross)
 */
public class DatabaseUtils {

  public static Connection createConnection(int index) throws SQLException {
    DatabasePlatform dbPlatform = DatabasePlatformFactory.FACTORY.getDatabasePlatform();
    return
        DriverManager.getConnection(
            dbPlatform.getUrl(index),
            dbPlatform.getUser(),
            dbPlatform.getPassword());
  }

  public static void destroyDatabase(int index, IdGenType idGenType) throws SQLException {
    DatabasePlatform dbPlatform = DatabasePlatformFactory.FACTORY.getDatabasePlatform();
    Connection conn = createConnection(index);
    try {
      for(String statement : dbPlatform.getDropTableStatements(idGenType)) {
        try {
          JdbcUtil.executeUpdate(conn, statement, false);
        } catch (SQLException sqle) {
          // not interested, keep moving
        }
      }
    } finally {
      conn.close();
    }
  }

  public static void createDatabase(int index, IdGenType idGenType) throws SQLException {
    DatabasePlatform dbPlatform = DatabasePlatformFactory.FACTORY.getDatabasePlatform();
    Connection conn = createConnection(index);
    try {
      for(String statement : dbPlatform.getCreateTableStatements(idGenType)) {
        JdbcUtil.executeUpdate(conn, statement, false);
      }
    } finally {
      conn.close();
    }
  }


}
