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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Helper methods for Jdbc
 *
 * @author maxr@google.com (Max Ross)
 */
public class JdbcUtil {

  private static final Log LOG = LogFactory.getLog(JdbcUtil.class);

  public static int executeUpdate(
      Connection conn,
      String query,
      boolean closeConnection) throws SQLException {
    Statement statement = null;
    try {
      statement = conn.createStatement();
      return statement.executeUpdate(query);
    } finally {
      if (closeConnection) {
        closeAllResources(null, statement, conn);
      } else {
        closeAllResources(null, statement, null);
      }
    }
  }

  public static void executeJdbcQuery(
      Connection conn,
      String query,
      JdbcStrategy jdbcStrategy,
      boolean closeConnection) throws SQLException {
    Statement statement = null;
    ResultSet rs = null;
    try {
      statement = conn.createStatement();
      rs = statement.executeQuery(query);
      while (rs.next()) {
        jdbcStrategy.extractData(rs);
      }
    } finally {
      if (closeConnection) {
        closeAllResources(rs, statement, conn);
      } else {
        closeAllResources(rs, statement, null);
      }
    }
  }


  public static void executeJdbcQuery(Connection conn, String query,
      JdbcStrategy jdbcStrategy)
      throws SQLException {
    executeJdbcQuery(conn, query, jdbcStrategy, true);
  }

  public static void executePreparedStatementQuery(Connection conn,
      PreparedStatement stmt, JdbcStrategy strategy, boolean closeConnection)
      throws SQLException {
    ResultSet rs = null;
    try {
      rs = stmt.executeQuery();
      while (rs.next()) {
        strategy.extractData(rs);
      }
    } finally {
      if (closeConnection) {
        closeAllResources(rs, stmt, conn);
      } else {
        closeAllResources(rs, stmt, null);
      }
    }
  }

  public static void executePreparedStatementQuery(Connection conn,
      PreparedStatement stmt, JdbcStrategy strategy)
      throws SQLException {
    executePreparedStatementQuery(conn, stmt, strategy, true);
  }

  public static void closeAllResources(ResultSet rs, Statement statement,
      Connection conn) {
    if (rs != null) {
      try {
        rs.close();
      } catch (Throwable t) {
        LOG.error("Error closing result set.", t);
      }
    }
    if (statement != null) {
      try {
        statement.close();
      } catch (Throwable t) {
        LOG.error("Error closing statement.", t);
      }
    }
    if (conn != null) {
      try {
        conn.close();
      } catch (Throwable t) {
        LOG.error("Error closing connection.", t);
      }
    }
  }
}
