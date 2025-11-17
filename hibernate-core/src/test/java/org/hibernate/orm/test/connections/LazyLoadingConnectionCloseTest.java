/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.connections;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.util.connections.ConnectionCheckingConnectionProvider;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;

import org.hibernate.testing.orm.junit.EntityManagerFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Selaron
 */
@JiraKey("HHH-4808")
public class LazyLoadingConnectionCloseTest extends EntityManagerFactoryBasedFunctionalTest {

	private ConnectionCheckingConnectionProvider connectionProvider;

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
		options.put( AvailableSettings.CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT, "false" );

		connectionProvider = new ConnectionCheckingConnectionProvider();
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
		public SimpleEntity getParent() {
			return parent;
		}

		public void setParent(final SimpleEntity parent) {
			this.parent = parent;
		}
	}

}
