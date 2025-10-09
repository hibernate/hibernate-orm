/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.unionsubclass;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import org.hibernate.Hibernate;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.testing.jdbc.SQLServerSnapshotIsolationConnectionProvider;
import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistryFunctionalTesting;
import org.hibernate.testing.orm.junit.ServiceRegistryProducer;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.List;

import static org.hibernate.cfg.JdbcSettings.CONNECTION_PROVIDER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gavin King
 */
@SuppressWarnings({"unchecked", "JUnitMalformedDeclaration"})
@ServiceRegistryFunctionalTesting
@DomainModel(xmlMappings = "org/hibernate/orm/test/unionsubclass/Beings.hbm.xml")
@SessionFactory
public class UnionSubclassTest implements ServiceRegistryProducer {

	@Override
	public StandardServiceRegistry produceServiceRegistry(StandardServiceRegistryBuilder builder) {
		if ( DialectContext.getDialect() instanceof SQLServerDialect ) {
			var connectionProvider = new SQLServerSnapshotIsolationConnectionProvider();
			connectionProvider.setConnectionProvider( (ConnectionProvider) builder.getSettings().get( CONNECTION_PROVIDER ) );
			builder.applySetting( CONNECTION_PROVIDER, connectionProvider );
		}
		return builder.build();
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testUnionSubclassCollection(SessionFactoryScope factoryScope) throws Exception {
		factoryScope.inTransaction(s -> {
			Location mel = new Location( "Earth" );
			s.persist( mel );

			Human gavin = new Human();
			gavin.setIdentity( "gavin" );
			gavin.setSex( 'M' );
			gavin.setLocation( mel );
			mel.addBeing( gavin );

			gavin.getInfo().put( "foo", "bar" );
			gavin.getInfo().put( "x", "y" );
		} );

		factoryScope.inTransaction(s -> {
			CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
			CriteriaQuery<Human> criteria = criteriaBuilder.createQuery( Human.class );
			criteria.from( Human.class );
			Human gavin = s.createQuery( criteria ).uniqueResult();
			assertEquals( 2, gavin.getInfo().size() );
			s.remove( gavin );
			s.remove( gavin.getLocation() );
		} );
	}

	@Test
	public void testUnionSubclassFetchMode(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction(s -> {
			Location mel = new Location( "Earth" );
			s.persist( mel );

			Human gavin = new Human();
			gavin.setIdentity( "gavin" );
			gavin.setSex( 'M' );
			gavin.setLocation( mel );
			mel.addBeing( gavin );
			Human max = new Human();
			max.setIdentity( "max" );
			max.setSex( 'M' );
			max.setLocation( mel );
			mel.addBeing( gavin );
		} );

		factoryScope.inTransaction( (s) -> {
			CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
			CriteriaQuery<Human> criteria = criteriaBuilder.createQuery( Human.class );
			criteria.from( Human.class ).fetch( "location", JoinType.LEFT ).fetch( "beings", JoinType.LEFT );

			List<Human> list = s.createQuery( criteria ).list();

			for ( Human h : list ) {
				assertTrue( Hibernate.isInitialized( h.getLocation() ) );
				assertTrue( Hibernate.isInitialized( h.getLocation().getBeings() ) );
			}
		} );
	}

	@Test
	public void testUnionSubclassOneToMany(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction(s -> {
			Location mel = new Location( "Melbourne, Australia" );
			Location mars = new Location( "Mars" );
			s.persist( mel );
			s.persist( mars );

			Human gavin = new Human();
			gavin.setIdentity( "gavin" );
			gavin.setSex( 'M' );
			gavin.setLocation( mel );
			mel.addBeing( gavin );

			Alien x23y4 = new Alien();
			x23y4.setIdentity( "x23y4$$hu%3" );
			x23y4.setLocation( mars );
			x23y4.setSpecies( "martian" );
			mars.addBeing( x23y4 );

			Alien yy3dk = new Alien();
			yy3dk.setIdentity( "yy3dk&*!!!" );
			yy3dk.setLocation( mars );
			yy3dk.setSpecies( "martian" );
			mars.addBeing( yy3dk );

			Hive hive = new Hive();
			hive.setLocation( mars );
			hive.getMembers().add( x23y4 );
			x23y4.setHive( hive );
			hive.getMembers().add( yy3dk );
			yy3dk.setHive( hive );
			s.persist( hive );

			yy3dk.getHivemates().add( x23y4 );
			x23y4.getHivemates().add( yy3dk );
		} );

		factoryScope.inTransaction( (s) -> {
			var hive = (Hive) s.createQuery( "from Hive h" ).uniqueResult();
			assertFalse( Hibernate.isInitialized( hive.getMembers() ) );
			assertEquals( 2, hive.getMembers().size() );

			s.clear();

			hive = (Hive) s.createQuery( "from Hive h left join fetch h.location left join fetch h.members" )
					.uniqueResult();
			assertTrue( Hibernate.isInitialized( hive.getMembers() ) );
			assertEquals( 2, hive.getMembers().size() );

			s.clear();

			var x23y4 = (Alien) s.createQuery( "from Alien a left join fetch a.hivemates where a.identity like 'x%'" )
					.uniqueResult();
			assertTrue( Hibernate.isInitialized( x23y4.getHivemates() ) );
			assertEquals( 1, x23y4.getHivemates().size() );

			s.clear();

			x23y4 = (Alien) s.createQuery( "from Alien a where a.identity like 'x%'" ).uniqueResult();
			assertFalse( Hibernate.isInitialized( x23y4.getHivemates() ) );
			assertEquals( 1, x23y4.getHivemates().size() );
		} );
	}

	@Test
	public void testUnionSubclassManyToOne(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction(s -> {
			Location mel = new Location( "Melbourne, Australia" );
			Location mars = new Location( "Mars" );
			s.persist( mel );
			s.persist( mars );

			Human gavin = new Human();
			gavin.setIdentity( "gavin" );
			gavin.setSex( 'M' );
			gavin.setLocation( mel );
			mel.addBeing( gavin );

			Alien x23y4 = new Alien();
			x23y4.setIdentity( "x23y4$$hu%3" );
			x23y4.setLocation( mars );
			x23y4.setSpecies( "martian" );
			mars.addBeing( x23y4 );

			Hive hive = new Hive();
			hive.setLocation( mars );
			hive.getMembers().add( x23y4 );
			x23y4.setHive( hive );
			s.persist( hive );

			Thing thing = new Thing();
			thing.setDescription( "some thing" );
			thing.setOwner( gavin );
			gavin.getThings().add( thing );
			s.persist( thing );
			s.flush();

			s.clear();

			thing = (Thing) s.createQuery( "from Thing t left join fetch t.owner" ).uniqueResult();
			assertTrue( Hibernate.isInitialized( thing.getOwner() ) );
			assertEquals( "gavin", thing.getOwner().getIdentity() );
			s.clear();

			thing = (Thing) s.createQuery(
							"select t from Thing t left join t.owner where t.owner.identity='gavin'" )
					.uniqueResult();
			assertFalse( Hibernate.isInitialized( thing.getOwner() ) );
			assertEquals( "gavin", thing.getOwner().getIdentity() );
			s.clear();

			gavin = (Human) s.createQuery( "from Human h left join fetch h.things" ).uniqueResult();
			assertTrue( Hibernate.isInitialized( gavin.getThings() ) );
			assertEquals( "some thing", ( (Thing) gavin.getThings().get( 0 ) ).getDescription() );
			s.clear();

			assertEquals( 2, s.createQuery( "from Being b left join fetch b.things" ).list().size() );
			s.clear();

			gavin = (Human) s.createQuery( "from Being b join fetch b.things" ).uniqueResult();
			assertTrue( Hibernate.isInitialized( gavin.getThings() ) );
			assertEquals( "some thing", ( (Thing) gavin.getThings().get( 0 ) ).getDescription() );
			s.clear();

			gavin = (Human) s.createQuery(
							"select h from Human h join h.things t where t.description='some thing'" )
					.uniqueResult();
			assertFalse( Hibernate.isInitialized( gavin.getThings() ) );
			assertEquals( "some thing", ( (Thing) gavin.getThings().get( 0 ) ).getDescription() );
			s.clear();

			gavin = (Human) s.createQuery(
							"select b from Being b join b.things t where t.description='some thing'" )
					.uniqueResult();
			assertFalse( Hibernate.isInitialized( gavin.getThings() ) );
			assertEquals( "some thing", ( (Thing) gavin.getThings().get( 0 ) ).getDescription() );
			s.clear();

			thing = s.find( Thing.class, thing.getId() );
			assertFalse( Hibernate.isInitialized( thing.getOwner() ) );
			assertEquals( "gavin", thing.getOwner().getIdentity() );

			thing.getOwner().getThings().remove( thing );
			thing.setOwner( x23y4 );
			x23y4.getThings().add( thing );

			s.flush();

			s.clear();

			thing = s.find( Thing.class, thing.getId() );
			assertFalse( Hibernate.isInitialized( thing.getOwner() ) );
			assertEquals( "x23y4$$hu%3", thing.getOwner().getIdentity()
			);

			s.remove( thing );
			CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
			CriteriaQuery<Alien> criteria = criteriaBuilder.createQuery( Alien.class );
			criteria.from( Alien.class );
			x23y4 = s.createQuery( criteria ).uniqueResult();
			s.remove( x23y4.getHive() );
			s.remove( s.find( Location.class, mel.getId() ) );
			s.remove( s.find( Location.class, mars.getId() ) );
			assertTrue( s.createQuery( "from Being" ).list().isEmpty() );

		} );
	}

	@Test
	public void testUnionSubclass(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction(s -> {
			Location mel = new Location( "Melbourne, Australia" );
			Location atl = new Location( "Atlanta, GA" );
			Location mars = new Location( "Mars" );
			s.persist( mel );
			s.persist( atl );
			s.persist( mars );

			Human gavin = new Human();
			gavin.setIdentity( "gavin" );
			gavin.setSex( 'M' );
			gavin.setLocation( mel );
			mel.addBeing( gavin );

			Alien x23y4 = new Alien();
			x23y4.setIdentity( "x23y4$$hu%3" );
			x23y4.setLocation( mars );
			x23y4.setSpecies( "martian" );
			mars.addBeing( x23y4 );

			Hive hive = new Hive();
			hive.setLocation( mars );
			hive.getMembers().add( x23y4 );
			x23y4.setHive( hive );
			s.persist( hive );

			assertEquals( 2, s.createQuery( "from Being" ).list().size() );
			assertEquals( 1, s.createQuery( "from Being b where b.class = Alien" ).list().size() );
			assertEquals( 1, s.createQuery( "from Being b where type(b) = :what" ).setParameter(
					"what",
					Alien.class
			).list().size() );
			assertEquals( 2, s.createQuery( "from Being b where type(b) in :what" ).setParameterList(
					"what",
					new Class[] {
							Alien.class,
							Human.class
					}
			).list().size() );
			assertEquals( 1, s.createQuery( "from Alien" ).list().size() );
			s.clear();

			List<?> beings = s.createQuery( "from Being b left join fetch b.location" ).list();
			for ( Object being : beings ) {
				Being b = (Being) being;
				assertTrue( Hibernate.isInitialized( b.getLocation() ) );
				assertNotNull( b.getLocation().getName() );
				assertNotNull( b.getIdentity() );
				assertNotNull( b.getSpecies() );
			}
			assertEquals( 2, beings.size() );
			s.clear();

			beings = s.createQuery( "from Being" ).list();
			for ( Object being : beings ) {
				Being b = (Being) being;
				assertFalse( Hibernate.isInitialized( b.getLocation() ) );
				assertNotNull( b.getLocation().getName() );
				assertNotNull( b.getIdentity() );
				assertNotNull( b.getSpecies() );
			}
			assertEquals( 2, beings.size() );
			s.clear();

			List<?> locations = s.createQuery( "from Location" ).list();
			int count = 0;
			for ( Object location : locations ) {
				Location l = (Location) location;
				assertNotNull( l.getName() );
				for ( Object o : l.getBeings() ) {
					count++;
					assertSame( l, ( (Being) o ).getLocation() );
				}
			}
			assertEquals( 2, count );
			assertEquals( 3, locations.size() );
			s.clear();

			locations = s.createQuery( "from Location loc left join fetch loc.beings" ).list();
			count = 0;
			for ( Object location : locations ) {
				Location l = (Location) location;
				assertNotNull( l.getName() );
				for ( Object o : l.getBeings() ) {
					count++;
					assertSame( ( (Being) o ).getLocation(), l );
				}
			}
			assertEquals( 2, count );
			assertEquals( 3, locations.size() );
			s.clear();

			gavin = s.find( Human.class, gavin.getId() );
			atl = s.find( Location.class, atl.getId() );

			atl.addBeing( gavin );
			assertEquals( 1, s.createQuery( "from Human h where h.location.name like '%GA'" ).list().size() );
			s.remove( gavin );

			CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
			CriteriaQuery<Alien> criteria = criteriaBuilder.createQuery( Alien.class );
			criteria.from( Alien.class );

			x23y4 = s.createQuery( criteria ).uniqueResult();
			s.remove( x23y4.getHive() );
			assertTrue( s.createQuery( "from Being" ).list().isEmpty() );

			s.createQuery( "delete from Location" ).executeUpdate();

		} );
	}

	@Test
	public void testNestedUnionedSubclasses(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction(s -> {
			Location mel = new Location( "Earth" );
			Human marcf = new Human();
			marcf.setIdentity( "marc" );
			marcf.setSex( 'M' );
			mel.addBeing( marcf );
			Employee steve = new Employee();
			steve.setIdentity( "steve" );
			steve.setSex( 'M' );
			steve.setSalary( (double) 0 );
			mel.addBeing( steve );
			s.persist( mel );
		} );
	}

	@Test
	@JiraKey(value = "HHH-11740")
	public void testBulkOperationsWithDifferentConnections(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction(s -> {
			Location mars = new Location( "Mars" );
			s.persist( mars );

			Location earth = new Location( "Earth" );
			s.persist( earth );

			Hive hive = new Hive();
			hive.setLocation( mars );
			s.persist( hive );

			Alien alien = new Alien();
			alien.setIdentity( "Uncle Martin" );
			alien.setSpecies( "Martian" );
			alien.setHive( hive );
			hive.getMembers().add( alien );
			mars.addBeing( alien );

			s.persist( alien );

			Human human = new Human();
			human.setIdentity( "Jane Doe" );
			human.setSex( 'M' );
			earth.addBeing( human );

			s.persist( human );
		} );

		// The following tests that bulk operations can be executed using 2 different
		// connections.

		factoryScope.inTransaction( s1 -> {
			// Force connection acquisition
			s1.getJdbcCoordinator().getLogicalConnection().getPhysicalConnection();

			// Transaction used by s1 is already started.
			// Assert that the Connection is already physically connected.
			assertTrue( s1.getJdbcCoordinator().getLogicalConnection().isPhysicallyConnected() );

			// Assert that the same Connection will be used for s1's entire transaction
			assertEquals(
					PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION,
					s1.getJdbcCoordinator().getLogicalConnection().getConnectionHandlingMode()
			);

			// Get the Connection s1 will use.
			final Connection connection1 = s1.getJdbcCoordinator().getLogicalConnection().getPhysicalConnection();

			// Avoid a pessimistic lock exception by not doing anything with s1 until
			// after a second Session (with a different connection) is used
			// for a bulk operation.

			factoryScope.inTransaction( s2 -> {
				// Force connection acquisition
				s2.getJdbcCoordinator().getLogicalConnection().getPhysicalConnection();
							// Check same assertions for s2 as was done for s1.
				assertTrue( s2.getJdbcCoordinator().getLogicalConnection().isPhysicallyConnected() );
							assertEquals(
									PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION,
									s2.getJdbcCoordinator().getLogicalConnection().getConnectionHandlingMode()
							);

							// Get the Connection s2 will use.
							Connection connection2 = s2.getJdbcCoordinator().getLogicalConnection().getPhysicalConnection();

							// Assert that connection2 is not the same as connection1
							assertNotSame( connection1, connection2 );

							// Execute a bulk operation on s2 (using connection2)
							assertEquals(
									1,
									s2.createQuery( "delete from Being where species = 'Martian'" ).executeUpdate()
							);

							// Assert the Connection has not changed
							assertSame( connection2, s2.getJdbcCoordinator().getLogicalConnection().getPhysicalConnection() );
						}
			);

			// Assert that the Connection used by s1 has hot changed.
			assertSame( connection1, s1.getJdbcCoordinator().getLogicalConnection().getPhysicalConnection() );

			// Execute a bulk operation on s1 (using connection1)
			assertEquals(
					1,
					s1.createQuery( "update Being set identity = 'John Doe' where identity = 'Jane Doe'" )
							.executeUpdate()
			);

			// Assert that the Connection used by s1 has hot changed.
			assertSame( connection1, s1.getJdbcCoordinator().getLogicalConnection().getPhysicalConnection() );

		} );
	}
}
