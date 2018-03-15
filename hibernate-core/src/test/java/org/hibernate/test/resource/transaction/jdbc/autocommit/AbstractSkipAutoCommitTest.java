/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.resource.transaction.jdbc.autocommit;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.EntityTransaction;
import javax.persistence.Id;
import javax.sql.DataSource;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.transaction.spi.TransactionImplementor;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.transaction.TransactionUtil2;
import org.hibernate.test.util.jdbc.PreparedStatementSpyConnectionProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author Vlad Mihalcea
 */
public abstract class AbstractSkipAutoCommitTest extends BaseUnitTestCase {

	private PreparedStatementSpyConnectionProvider connectionProvider;
	private DataSource dataSource;
	private SessionFactoryImplementor emf;

	@Before
	public void createEntityManagerFactory() {
		Map<String,Object> config = new HashMap<>();

		connectionProvider = new PreparedStatementSpyConnectionProvider() {
			@Override
			protected Connection actualConnection() throws SQLException {
				Connection connection = super.actualConnection();
				connection.setAutoCommit( false );
				return connection;
			}
		};

		dataSource = dataSource();

		config.put( AvailableSettings.CONNECTION_PROVIDER, connectionProvider );
		config.put( AvailableSettings.DATASOURCE, dataSource );
		config.put( AvailableSettings.CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT, Boolean.TRUE );
		config.put( AvailableSettings.HBM2DDL_AUTO, "create-drop" );

		config.put( AvailableSettings.JPA_TRANSACTION_COMPLIANCE, "false" );

		emf =  Bootstrap.getEntityManagerFactoryBuilder(
				new BaseEntityManagerFunctionalTestCase.TestingPersistenceUnitDescriptorImpl( getClass().getSimpleName() ) {
					@Override
					public List<String> getManagedClassNames() {
						return Collections.singletonList( City.class.getName() );
					}
				},
				config
		).build().unwrap( SessionFactoryImplementor.class );
		if ( emf == null ) {
			throw new RuntimeException( "Could not build EMF" );
		}
	}

	protected abstract DataSource dataSource();

	@After
	public void releaseResources() {
		if ( connectionProvider != null ) {
			connectionProvider.stop();
		}

		// todo : somewhay to stop/close DataSource if not Closeable?
		if ( dataSource instanceof Closeable ) {
			try {
				( (Closeable) dataSource ).close();
			}
			catch (IOException e) {
				log.debugf( "Unable to release DataSource : %s", dataSource );
			}

			if ( emf != null ) {
				emf.close();
			}
		}
	}

	@Test
	public void testRollbackOnNonJtaDataSourceWithAutoCommitConnection() {
		TransactionUtil2.inEntityManager(
				emf,
				entityManager -> {
					final EntityTransaction txn = entityManager.getTransaction();
//					txn.begin();

					final TransactionImplementor hibernateTxn = (TransactionImplementor) txn;
					hibernateTxn.markRollbackOnly();

					txn.rollback();
				}
		);
	}

	@Test
	public void test() {
		connectionProvider.clear();
		doInJPA(
				() -> emf,
				entityManager -> {
					City city = new City();
					city.setId( 1L );
					city.setName( "Cluj-Napoca" );
					entityManager.persist( city );

					assertTrue( connectionProvider.getAcquiredConnections().isEmpty() );
					assertTrue( connectionProvider.getReleasedConnections().isEmpty() );
				}
		);
		verifyConnections();

		connectionProvider.clear();
		doInJPA(
				() -> emf,
				entityManager -> {
					City city = entityManager.find( City.class, 1L );
					assertEquals( "Cluj-Napoca", city.getName() );
				}
		);
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
