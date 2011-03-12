/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat, Inc. and/or it's affiliates, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.hibernate.test.cache.infinispan.tm;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.connection.ConnectionProviderFactory;

/**
 * XaConnectionProvider.
 * 
 * @author Galder Zamarreño
 * @since 3.5
 */
public class XaConnectionProvider implements ConnectionProvider {
   private static ConnectionProvider actualConnectionProvider = ConnectionProviderFactory.newConnectionProvider();
   private boolean isTransactional;

   public static ConnectionProvider getActualConnectionProvider() {
      return actualConnectionProvider;
   }

   public void configure(Properties props) throws HibernateException {
   }

   public Connection getConnection() throws SQLException {
      XaTransactionImpl currentTransaction = XaTransactionManagerImpl.getInstance().getCurrentTransaction();
      if (currentTransaction == null) {
         isTransactional = false;
         return actualConnectionProvider.getConnection();
      } else {
         isTransactional = true;
         Connection connection = currentTransaction.getEnlistedConnection();
         if (connection == null) {
            connection = actualConnectionProvider.getConnection();
            currentTransaction.enlistConnection(connection);
         }
         return connection;
      }
   }

   public void closeConnection(Connection conn) throws SQLException {
      if (!isTransactional) {
         conn.close();
      }
   }

   public void close() throws HibernateException {
      actualConnectionProvider.close();
   }

   public boolean supportsAggressiveRelease() {
      return true;
   }
}
