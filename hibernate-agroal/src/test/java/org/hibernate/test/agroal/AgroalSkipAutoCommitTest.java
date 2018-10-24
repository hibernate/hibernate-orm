/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.agroal;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.test.agroal.util.PreparedStatementSpyConnectionProvider;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialectFeature(DialectChecks.SupportsJdbcDriverProxying.class)
public class AgroalSkipAutoCommitTest extends BaseCoreFunctionalTestCase {

	private PreparedStatementSpyConnectionProvider connectionProvider = new PreparedStatementSpyConnectionProvider();

	@Override
	protected void configure(Configuration configuration) {
		configuration.getProperties().put( AvailableSettings.CONNECTION_PROVIDER, connectionProvider );
		configuration.getProperties().put( AvailableSettings.CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT, Boolean.TRUE );
		configuration.getProperties().put( AvailableSettings.AUTOCOMMIT, Boolean.FALSE.toString() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ City.class };
	}

	@Test
	public void test() {
		connectionProvider.clear();
		doInHibernate( this::sessionFactory, session -> {
			City city = new City();
			city.setId( 1L );
			city.setName( "Cluj-Napoca" );
			session.persist( city );

			assertTrue( connectionProvider.getAcquiredConnections().isEmpty() );
			assertTrue( connectionProvider.getReleasedConnections().isEmpty() );
		} );
		verifyConnections();

		connectionProvider.clear();
		doInHibernate( this::sessionFactory, session -> {
			City city = session.find( City.class, 1L );
			assertEquals( "Cluj-Napoca", city.getName() );
		} );
		verifyConnections();
	}

	private void verifyConnections() {
		assertTrue( connectionProvider.getAcquiredConnections().isEmpty() );

		List<Connection> connections = connectionProvider.getReleasedConnections();
		assertEquals( 1, connections.size() );
		Connection connection = connections.get( 0 );
		try {
			verify(connection, never()).setAutoCommit( false );
		}
		catch (SQLException e) {
			fail(e.getMessage());
		}
	}

	@Entity(name = "City" )
	public static class City {

		@Id
		private Long id;

		private String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
