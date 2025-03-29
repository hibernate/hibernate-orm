/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter.caching;

import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.entry.StandardCacheEntryImpl;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true" ),
				@Setting( name = AvailableSettings.CACHE_REGION_FACTORY, value = "org.hibernate.testing.cache.CachingRegionFactory" ),
				@Setting( name = AvailableSettings.USE_STRUCTURED_CACHE, value = "false" ),
		}
)
@DomainModel( annotatedClasses = Address.class )
@SessionFactory
public class BasicUnstructuredCachingOfConvertedValueTest {

	public static final int postalAreaAttributeIndex = 0;

	@Test
	@JiraKey( value = "HHH-9615" )
	public void basicCacheStructureTest(SessionFactoryScope scope) {
		EntityPersister persister = scope.getSessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor( Address.class.getName() );
		final DomainDataRegion region = persister.getCacheAccessStrategy().getRegion();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// test during store...
		PostalAreaConverter.clearCounts();

		scope.inTransaction(
				(session) -> {
					session.persist( new Address( 1, "123 Main St.", null, PostalArea._78729 ) );
				}
		);

		scope.inTransaction(
				(session) -> {
					final EntityDataAccess entityDataAccess = region.getEntityDataAccess( persister.getNavigableRole() );
					final Object cacheKey = entityDataAccess.generateCacheKey(
							1,
							persister,
							scope.getSessionFactory(),
							null
					);
					final Object cachedItem = entityDataAccess.get( session, cacheKey );
					final StandardCacheEntryImpl state = (StandardCacheEntryImpl) cachedItem;
					// this is the point of the Jira.. that this "should be" the converted value
					assertThat( state.getDisassembledState()[ postalAreaAttributeIndex ], instanceOf( PostalArea.class ) );
				}
		);

		assertThat( PostalAreaConverter.toDatabaseCallCount, is( 1 ) );
		assertThat( PostalAreaConverter.toDomainCallCount, is( 0 ) );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// test during load...
		PostalAreaConverter.clearCounts();
		scope.getSessionFactory().getCache().evictAll();

		scope.inTransaction(
				(session) -> session.get( Address.class, 1 )
		);

		scope.inTransaction(
				(session) -> {
					final EntityDataAccess entityDataAccess = region.getEntityDataAccess( persister.getNavigableRole() );
					final Object cacheKey = entityDataAccess.generateCacheKey(
							1,
							persister,
							scope.getSessionFactory(),
							null
					);
					final Object cachedItem = entityDataAccess.get( session, cacheKey );
					final StandardCacheEntryImpl state = (StandardCacheEntryImpl) cachedItem;
					// this is the point of the Jira.. that this "should be" the converted value
					assertThat( state.getDisassembledState()[ postalAreaAttributeIndex ], instanceOf( PostalArea.class ) );
				}
		);

		assertThat( PostalAreaConverter.toDatabaseCallCount, is( 0 ) );
		assertThat( PostalAreaConverter.toDomainCallCount, is( 1 ) );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> session.createQuery( "delete Address" ).executeUpdate()
		);
	}
}
