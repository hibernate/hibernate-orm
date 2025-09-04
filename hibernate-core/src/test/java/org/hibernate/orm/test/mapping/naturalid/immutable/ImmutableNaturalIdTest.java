/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid.immutable;

import jakarta.persistence.PersistenceException;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true" ),
				@Setting( name = AvailableSettings.USE_QUERY_CACHE, value = "true" ),
				@Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" )
		}
)
@DomainModel( xmlMappings = "org/hibernate/orm/test/mapping/naturalid/immutable/User.hbm.xml" )
@SessionFactory
public class ImmutableNaturalIdTest {
	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey( value = "HHH-10360")
	public void verifyMetamodel(SessionFactoryScope scope) {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		final EntityMappingType entityMappingType = sessionFactory.getRuntimeMetamodels().getEntityMappingType( User.class );

		final AttributeMapping userNameMapping = entityMappingType.findAttributeMapping( "userName" );
		assertFalse( userNameMapping.getAttributeMetadata().isNullable() );

		final EntityPersister persister = entityMappingType.getEntityPersister();
		final int propertyIndex = persister.getPropertyIndex( "userName" );
		// nullability is not specified, so it should be non-nullable by hbm-specific default
		assertFalse( persister.getPropertyNullability()[propertyIndex] );
	}

	@Test
	public void testNaturalIdCheck(SessionFactoryScope scope) {
		final User detachedUser = scope.fromTransaction(
				(session) -> {
					final User user = new User( "steve", "superSecret" );
					session.persist( user );
					return user;
				}
		);

		// try to change the user-name (natural-id) and re-attach... should error
		detachedUser.setUserName( "Steve" );
		try {
			scope.inTransaction(
					(session) -> {
						session.merge( detachedUser );
					}
			);
			fail();
		}
		catch (PersistenceException expected) {
			// expected outcome
		}
	}

	@Test
	public void testNaturalIdCache(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();

		scope.inTransaction(
				(session) -> {
					final User u = new User( "steve", "superSecret" );
					session.persist( u );
				}
		);

		statistics.clear();

		scope.inTransaction(
				(session) -> {
					final User steve = session.bySimpleNaturalId( User.class ).load( "steve" );
					assertNotNull( steve );
				}
		);

		assertEquals( 0, statistics.getNaturalIdCacheHitCount() );
		assertEquals( 0, statistics.getNaturalIdCachePutCount() );

		scope.inTransaction(
				(session) -> {
					session.persist( new User( "gavin", "supsup" ) );
				}
		);

		statistics.clear();

		scope.inTransaction(
				(session) -> {
					final User steve = session.bySimpleNaturalId( User.class ).load( "steve" );
					assertNotNull( steve );
					assertEquals( 0, statistics.getNaturalIdCacheHitCount() );

					final User steve2 = session.bySimpleNaturalId( User.class ).load( "steve" );
					assertNotNull( steve2 );
					assertEquals( 0, statistics.getNaturalIdCacheHitCount() );

				}
		);
	}

	@Test
	public void testNaturalIdDeleteUsingCache(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();

		scope.inTransaction(
				(session) -> {
					final User u = new User( "steve", "superSecret" );
					session.persist( u );
				}
		);

		statistics.clear();

		scope.inTransaction(
				(session) -> {
					final User steve = session.bySimpleNaturalId( User.class ).load( "steve" );
					assertNotNull( steve );
				}
		);

		assertEquals( 1, statistics.getNaturalIdQueryExecutionCount() );
		assertEquals( 0, statistics.getNaturalIdCacheHitCount() );
		assertEquals( 0, statistics.getNaturalIdCachePutCount() );

		statistics.clear();

		scope.inTransaction(
				(session) -> {
					final User steve = session.bySimpleNaturalId( User.class ).load( "steve" );
					session.remove( steve );
				}
		);

		scope.inTransaction(
				(session) -> {
					final User steve = session.bySimpleNaturalId( User.class ).load( "steve" );
					assertNull( steve );
				}
		);
	}

}
