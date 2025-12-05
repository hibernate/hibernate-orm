/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid.immutableentity;

import org.hibernate.NaturalIdLoadAccess;
import org.hibernate.annotations.Immutable;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test case for NaturalId annotation on an {@link Immutable} entity
 *
 * @author Eric Dalquist
 */
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.USE_QUERY_CACHE, value = "true" ),
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true" ),
				@Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" )
		}
)
@DomainModel( annotatedClasses = Building.class )
@SessionFactory
@SuppressWarnings("unchecked")
@JiraKey( value = "HHH-7085" )
public class ImmutableEntityNaturalIdTest {
	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		final StatisticsImplementor stats = sessionFactory.getStatistics();

		sessionFactory.getCache().evictAllRegions();
		stats.clear();

		scope.inTransaction(
				(session) -> {
					Building b1 = new Building();
					b1.setName( "Computer Science" );
					b1.setAddress( "1210 W. Dayton St." );
					b1.setCity( "Madison" );
					b1.setState( "WI" );

					session.persist( b1 );
				}
		);

		assertEquals( 0, stats.getNaturalIdCacheHitCount(), "Cache hits should be empty" );
		assertEquals( 0, stats.getNaturalIdCacheMissCount(), "Cache misses should be empty" );
		assertEquals( 1, stats.getNaturalIdCachePutCount(), "Cache put should be one after insert" );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testNaturalIdMapping(SessionFactoryScope scope) {
		final EntityMappingType buildingMapping = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getEntityMappingType( Building.class );

		final NaturalIdMapping naturalIdMapping = buildingMapping.getNaturalIdMapping();
		assertThat( naturalIdMapping, notNullValue() );
		assertThat( naturalIdMapping.getNaturalIdAttributes().size(), is( 3 ) );
		// nullability is not specified, so they should be nullable by annotations-specific default
		for ( SingularAttributeMapping attribute : naturalIdMapping.getNaturalIdAttributes() ) {
			assertThat( attribute.getAttributeMetadata().isNullable(), is( true ) );
		}

		final EntityPersister entityPersister = buildingMapping.getEntityPersister();
		assertThat(
				"Class should have a natural key",
				entityPersister.hasNaturalIdentifier(),
				is( true )
		);
		assertThat(
				"Wrong number of attributes",
				entityPersister.getNaturalIdentifierProperties().length,
				is( 3 )
		);

		// nullability is not specified, so they should be nullable by annotations-specific default
		assertTrue( entityPersister.getPropertyNullability()[ entityPersister.getPropertyIndex( "address" )] );
		assertTrue( entityPersister.getPropertyNullability()[ entityPersister.getPropertyIndex( "city" )] );
		assertTrue( entityPersister.getPropertyNullability()[ entityPersister.getPropertyIndex( "state" )] );

	}

	@Test
	public void testImmutableNaturalIdLifecycle(SessionFactoryScope scope) {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		final StatisticsImplementor stats = sessionFactory.getStatistics();

		// Clear caches and reset cache stats
		sessionFactory.getCache().evictNaturalIdData();
		stats.clear();

		// load #1 - should result in:
		//		- cache miss
		//		- query
		//		- cache put

		scope.inTransaction(
				(session) -> {
					final NaturalIdLoadAccess<Building> naturalIdLoader = session.byNaturalId( Building.class );
					final Building building = naturalIdLoader
							.using( "address", "1210 W. Dayton St." )
							.using( "city", "Madison" )
							.using( "state", "WI" )
							.load();
					assertThat( building, notNullValue() );
					assertEquals( 0, stats.getNaturalIdCacheHitCount(),  "Cache hits should be empty" );
					assertEquals( 1, stats.getNaturalIdCacheMissCount(), "Cache misses should be one" );
					assertEquals( 1, stats.getNaturalIdCachePutCount(), "Cache put should be one after load" );
					assertThat( stats.getPrepareStatementCount(), is( 1L ) );
				}
		);

		// load #2 - should result in
		//		- cache hit

		scope.inTransaction(
				(session) -> {
					final NaturalIdLoadAccess<Building> naturalIdLoader = session.byNaturalId( Building.class );
					final Building building = naturalIdLoader
							.using( "address", "1210 W. Dayton St." )
							.using( "city", "Madison" )
							.using( "state", "WI" )
							.load();
					assertThat( building, notNullValue() );
					assertEquals( 1, stats.getNaturalIdCacheHitCount(), "Cache hits should be one after second query" );
					assertEquals( 1, stats.getNaturalIdCacheMissCount(), "Cache misses should be one after second query" );
					assertEquals( 1, stats.getNaturalIdCachePutCount(), "Cache put should be one after second query" );

					// Try Deleting
					session.remove( building );

					// third query
					naturalIdLoader.load();
					assertEquals( 1, stats.getNaturalIdCacheHitCount(), "Cache hits should be one after second query" );
					assertEquals( 1, stats.getNaturalIdCacheMissCount(), "Cache misses should be two after second query" );
					assertEquals( 1, stats.getNaturalIdCachePutCount(), "Cache put should be one after second query" );
				}
		);

		//Try three, should be db lookup and miss

		scope.inTransaction(
				(session) -> {
					final Building building = session.byNaturalId( Building.class )
							.using( "address", "1210 W. Dayton St." )
							.using( "city", "Madison" )
							.using( "state", "WI" )
							.load();

					// second query
					assertNull( building );
					assertEquals( 1, stats.getNaturalIdCacheHitCount(), "Cache hits should be one after third query" );
					assertEquals( 2, stats.getNaturalIdCacheMissCount(), "Cache misses should be one after third query" );
					assertEquals( 1, stats.getNaturalIdCachePutCount(), "Cache put should be one after third query" );

					// here, we should know that that natural-id does not exist as part of the Session...
					session.byNaturalId( Building.class )
							.using( "address", "1210 W. Dayton St." )
							.using( "city", "Madison" )
							.using( "state", "WI" )
							.load();

					assertEquals( 1, stats.getNaturalIdCacheHitCount(), "Cache hits should still be one" );
					assertEquals( 3, stats.getNaturalIdCacheMissCount(), "Cache misses should now be four" );
					assertEquals( 1, stats.getNaturalIdCachePutCount(), "Cache put should still be one" );
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-7371" )
	public void testImmutableNaturalIdLifecycle2(SessionFactoryScope scope) {
		scope.inTransaction(
				(s) -> {
					final NaturalIdLoadAccess<Building> naturalIdLoader = s.byNaturalId( Building.class );
					naturalIdLoader
							.using( "address", "1210 W. Dayton St." )
							.using( "city", "Madison" )
							.using( "state", "WI" );

					Building building = naturalIdLoader.getReference();
					assertNotNull( building );

					s.remove( building );
					building = naturalIdLoader.load();
//org.hibernate.ObjectNotFoundException: No row with the given identifier exists: [org.hibernate.test.naturalid.immutableentity.Building#1]
//		at org.hibernate.internal.SessionFactoryImpl$1$1.handleEntityNotFound(SessionFactoryImpl.java:247)
//		at org.hibernate.event.internal.DefaultLoadEventListener.returnNarrowedProxy(DefaultLoadEventListener.java:282)
//		at org.hibernate.event.internal.DefaultLoadEventListener.proxyOrLoad(DefaultLoadEventListener.java:248)
//		at org.hibernate.event.internal.DefaultLoadEventListener.onLoad(DefaultLoadEventListener.java:148)
//		at org.hibernate.internal.SessionImpl.fireLoad(SessionImpl.java:1079)
//		at org.hibernate.internal.SessionImpl.access$13(SessionImpl.java:1075)
//		at org.hibernate.internal.SessionImpl$IdentifierLoadAccessImpl.load(SessionImpl.java:2425)
//		at org.hibernate.internal.SessionImpl$NaturalIdLoadAccessImpl.load(SessionImpl.java:2586)
//		at org.hibernate.test.naturalid.immutableentity.ImmutableEntityNaturalIdTest.testImmutableNaturalIdLifecycle2(ImmutableEntityNaturalIdTest.java:188)

					assertNull( building );
				}
		);
	}
}
