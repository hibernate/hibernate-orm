/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat, Inc. and/or it's affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc. and/or it's affiliates.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.cache.infinispan.functional.cluster;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.Stoppable;
import org.hibernate.testing.env.ConnectionProviderBuilder;

/**
 * A {@link ConnectionProvider} implementation adding JTA-style transactionality around the returned
 * connections using the {@link DualNodeJtaTransactionManagerImpl}.
 * 
 * @author Brian Stansberry
 */
public class DualNodeConnectionProviderImpl implements ConnectionProvider, Configurable {
   private static ConnectionProvider actualConnectionProvider = ConnectionProviderBuilder.buildConnectionProvider();
   private String nodeId;
   private boolean isTransactional;

	@Override
	public boolean isUnwrappableAs(Class unwrapType) {
		return DualNodeConnectionProviderImpl.class.isAssignableFrom( unwrapType ) ||
				ConnectionProvider.class.isAssignableFrom( unwrapType );
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public <T> T unwrap(Class<T> unwrapType) {
		if ( DualNodeConnectionProviderImpl.class.isAssignableFrom( unwrapType ) ) {
			return (T) this;
		}
		else if ( ConnectionProvider.class.isAssignableFrom( unwrapType ) ) {
			return (T) actualConnectionProvider;
		}
		else {
			throw new UnknownUnwrapTypeException( unwrapType );
		}
	}

   public static ConnectionProvider getActualConnectionProvider() {
      return actualConnectionProvider;
   }

   public void setNodeId(String nodeId) throws HibernateException {
      if (nodeId == null) {
         throw new HibernateException( "nodeId not configured" );
	  }
	  this.nodeId = nodeId;
   }

   public Connection getConnection() throws SQLException {
      DualNodeJtaTransactionImpl currentTransaction = DualNodeJtaTransactionManagerImpl
               .getInstance(nodeId).getCurrentTransaction();
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
	   if ( actualConnectionProvider instanceof Stoppable ) {
		   ( ( Stoppable ) actualConnectionProvider ).stop();
	   }
   }

   public boolean supportsAggressiveRelease() {
      return true;
   }

	@Override
	public void configure(Map configurationValues) {
		nodeId = (String) configurationValues.get( "nodeId" );
	}
}
