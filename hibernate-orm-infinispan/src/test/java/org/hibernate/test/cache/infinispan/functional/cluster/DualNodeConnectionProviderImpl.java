/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
