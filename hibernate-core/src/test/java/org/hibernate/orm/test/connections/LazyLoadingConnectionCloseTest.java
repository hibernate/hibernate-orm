/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.connections;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Query;
import javax.sql.DataSource;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl;
import org.hibernate.orm.test.jpa.connection.BaseDataSource;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.EntityManagerFactoryBasedFunctionalTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;

/**
 * @author Selaron
 */
@TestForIssue(jiraKey = "HHH-4808")
public class LazyLoadingConnectionCloseTest extends EntityManagerFactoryBasedFunctionalTest {

	private ConnectionProviderDecorator connectionProvider;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { SimpleEntity.class, ChildEntity.class };
	}

	@Override
	protected void addConfigOptions(Map options) {
		options.put( AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, "true" );

		options.put(
				AvailableSettings.CONNECTION_HANDLING,
				PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT
		);

		options.put( AvailableSettings.AUTOCOMMIT, "false" );

		connectionProvider = new ConnectionProviderDecorator( getDataSource() );
		options.put( AvailableSettings.CONNECTION_PROVIDER, connectionProvider );

	}


	@BeforeAll
	public void setUp() {
		inTransaction( entityManager -> {
			final SimpleEntity entity = new SimpleEntity();
			entity.setId( 1L );
			entity.setName( "TheParent" );

			final ChildEntity c1 = new ChildEntity();
			c1.setId( 1L );
			c1.setParent( entity );
			c1.setName( "child1" );

			final ChildEntity c2 = new ChildEntity();
			c2.setId( 2L );
			c2.setParent( entity );
			c2.setName( "child2" );

			entityManager.persist( entity );
			entityManager.persist( c1 );
			entityManager.persist( c2 );
		} );
	}

	@AfterAll
	public void tearDown() {
		inTransaction( entityManager -> {
			entityManager.createQuery( "delete from ChildEntity" ).executeUpdate();
			entityManager.createQuery( "delete from SimpleEntity" ).executeUpdate();
		} );
	}

	/**
	 * Tests connections get closed after transaction commit.
	 */
	@Test
	public void testConnectionCloseAfterTx() {
		connectionProvider.clear();
		inEntityManager(
				entityManager -> {
					entityManager.getTransaction().begin();
					try {

						final Query qry = entityManager.createQuery( "FROM SimpleEntity" );
						final List<SimpleEntity> entities = qry.getResultList();
						final SimpleEntity entity = entities.get( 0 );
						assertEquals( 1, connectionProvider.getCurrentOpenConnections() );
					}
					catch (Exception e) {
						if ( entityManager.getTransaction().isActive() ) {
							entityManager.getTransaction().rollback();
						}
						throw e;
					}
					finally {
						if ( entityManager.getTransaction().isActive() ) {
							entityManager.getTransaction().commit();
						}
					}
					assertTrue( connectionProvider.areAllConnectionClosed() );
				}
		);

	}

	/**
	 * Tests connections get closed after lazy collection initialization.
	 */
	@Test
	public void testConnectionCloseAfterLazyCollectionInit() {
		connectionProvider.clear();
		inEntityManager(
				entityManager -> {
					final Query qry = entityManager.createQuery( "FROM SimpleEntity" );
					final List<SimpleEntity> entities = qry.getResultList();
					final SimpleEntity entity = entities.get( 0 );

					// assert no connection is open
					assertTrue( connectionProvider.areAllConnectionClosed() );

					final int oldOpenedConnections = connectionProvider.getTotalOpenedConnectionCount();
					final Set<ChildEntity> lazyChildren = entity.getChildren();

					// this will initialize the collection and such trigger a query
					lazyChildren.stream().findAny();

					// assert a connection had been opened
					assertTrue( oldOpenedConnections < connectionProvider.getTotalOpenedConnectionCount() );

					// assert there's no remaining connection left.
					assertTrue( connectionProvider.areAllConnectionClosed() );

				}
		);
	}

	/**
	 * Tests connections get closed after transaction commit.
	 */
	@Test
	public void testConnectionCloseAfterLazyPojoPropertyInit() {
		connectionProvider.clear();
		inEntityManager(
				entityManager -> {
					final Query qry = entityManager.createQuery( "FROM ChildEntity" );
					final List<ChildEntity> entities = qry.getResultList();
					final ChildEntity entity = entities.get( 0 );

					// assert no connection is open
					assertTrue( connectionProvider.areAllConnectionClosed() );

					final int oldOpenedConnections = connectionProvider.getTotalOpenedConnectionCount();

					final SimpleEntity parent = entity.getParent();
					// this will initialize the collection and such trigger a query
					parent.getName();
					// assert a connection had been opened
					assertTrue( oldOpenedConnections < connectionProvider.getTotalOpenedConnectionCount() );


					// assert there's no remaining connection left.
					assertTrue( connectionProvider.areAllConnectionClosed() );
				}
		);
	}

	/**
	 * Tests connections get closed after transaction commit.
	 */
	@Test
	public void testConnectionCloseAfterQueryWithoutTx() {
		connectionProvider.clear();
		inEntityManager(
				entityManager -> {
					final int oldOpenedConnections = connectionProvider.getTotalOpenedConnectionCount();
					final List<ChildEntity> childrenByQuery = entityManager.createQuery( "FROM ChildEntity" )
							.getResultList();
					assertTrue( childrenByQuery.size() > 0 );

					// assert a connection had been opened
					assertTrue( oldOpenedConnections < connectionProvider.getTotalOpenedConnectionCount() );
					// assert there's no remaining connection left.
					assertTrue( connectionProvider.areAllConnectionClosed() );
				}
		);
	}

	@Entity(name = "SimpleEntity")
	public static class SimpleEntity {
		private Long id;

		private String name;

		Set<ChildEntity> children = new HashSet<>();

		@Id
		public Long getId() {
			return id;
		}

		public void setId(final Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(final String name) {
			this.name = name;
		}

		@OneToMany(targetEntity = ChildEntity.class, mappedBy = "parent")
		@LazyCollection(LazyCollectionOption.EXTRA)
		@Fetch(FetchMode.SELECT)
		public Set<ChildEntity> getChildren() {
			return children;
		}

		public void setChildren(final Set<ChildEntity> children) {
			this.children = children;
		}
	}

	@Entity(name = "ChildEntity")
	public static class ChildEntity {
		private Long id;

		private String name;

		private SimpleEntity parent;

		@Id
		public Long getId() {
			return id;
		}

		public void setId(final Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(final String name) {
			this.name = name;
		}

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn
		@LazyToOne(LazyToOneOption.PROXY)
		public SimpleEntity getParent() {
			return parent;
		}

		public void setParent(final SimpleEntity parent) {
			this.parent = parent;
		}
	}

	private BaseDataSource getDataSource() {
		final Properties connectionProps = new Properties();
		connectionProps.put( "user", Environment.getProperties().getProperty( Environment.USER ) );
		connectionProps.put( "password", Environment.getProperties().getProperty( Environment.PASS ) );

		final String url = Environment.getProperties().getProperty( Environment.URL );
		return new BaseDataSource() {
			@Override
			public Connection getConnection() throws SQLException {
				return DriverManager.getConnection( url, connectionProps );
			}

			@Override
			public Connection getConnection(String username, String password) throws SQLException {
				return DriverManager.getConnection( url, connectionProps );
			}
		};
	}

	public static class ConnectionProviderDecorator extends UserSuppliedConnectionProviderImpl {

		private final DataSource dataSource;

		private int connectionCount;
		private int openConnections;

		private Connection connection;

		public ConnectionProviderDecorator(DataSource dataSource) {
			this.dataSource = dataSource;
		}

		@Override
		public Connection getConnection() throws SQLException {
			connectionCount++;
			openConnections++;
			connection = spy( dataSource.getConnection() );
			return connection;
		}

		@Override
		public void closeConnection(Connection connection) throws SQLException {
			connection.close();
			openConnections--;
		}

		public int getTotalOpenedConnectionCount() {
			return this.connectionCount;
		}

		public int getCurrentOpenConnections() {
			return openConnections;
		}

		public boolean areAllConnectionClosed() {
			return openConnections == 0;
		}

		public void clear() {
			connectionCount = 0;
			openConnections = 0;
		}
	}

}
