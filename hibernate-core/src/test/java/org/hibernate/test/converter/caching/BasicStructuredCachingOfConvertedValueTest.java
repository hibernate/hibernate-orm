/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.converter.caching;

import java.util.Map;

import org.hibernate.Session;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.cache.CachingRegionFactory;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
public class BasicStructuredCachingOfConvertedValueTest extends BaseNonConfigCoreFunctionalTestCase {

	@Test
	@TestForIssue( jiraKey = "HHH-9615" )
	@SuppressWarnings("unchecked")
	public void basicCacheStructureTest() {
		EntityPersister persister =  sessionFactory().getMetamodel().entityPersisters().get( Address.class.getName() );
		DomainDataRegion region = persister.getCacheAccessStrategy().getRegion();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// test during store...
		PostalAreaConverter.clearCounts();

		Session session = openSession();
		session.getTransaction().begin();
		session.save( new Address( 1, "123 Main St.", null, PostalArea._78729 ) );
		session.getTransaction().commit();
		session.close();

		{
			inSession(
					s -> {
						final EntityDataAccess entityDataAccess = region.getEntityDataAccess( persister.getNavigableRole() );
						final Object cacheKey = entityDataAccess.generateCacheKey(
								1,
								persister,
								sessionFactory(),
								null
						);
						final Object cachedItem = entityDataAccess.get( s, cacheKey );
						final Map<String, ?> state = (Map) cachedItem;
						// this is the point of the Jira.. that this "should be" the converted value
						assertThat( state.get( "postalArea" ), instanceOf( PostalArea.class ) );
					}
			);
		}

		assertThat( PostalAreaConverter.toDatabaseCallCount, is(1) );
		assertThat( PostalAreaConverter.toDomainCallCount, is(0) );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// test during load...
		PostalAreaConverter.clearCounts();
		sessionFactory().getCache().evictAll();

		session = openSession();
		session.getTransaction().begin();
		Address address = session.get( Address.class, 1 );
		session.getTransaction().commit();
		session.close();

		{
			inSession(
					s -> {
						final EntityDataAccess entityDataAccess = region.getEntityDataAccess( persister.getNavigableRole() );
						final Object cacheKey = entityDataAccess.generateCacheKey(
								1,
								persister,
								sessionFactory(),
								null
						);
						final Object cachedItem = entityDataAccess.get( s, cacheKey );
						final Map<String, ?> state = (Map) cachedItem;
						// this is the point of the Jira.. that this "should be" the converted value
						assertThat( state.get( "postalArea" ), instanceOf( PostalArea.class ) );
					}
			);
		}

		assertThat( PostalAreaConverter.toDatabaseCallCount, is(0) );
		assertThat( PostalAreaConverter.toDomainCallCount, is(1) );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// cleanup
		session = openSession();
		session.getTransaction().begin();
		session.delete( address );
		session.getTransaction().commit();
		session.close();
	}


	@Override
	protected void addSettings(Map settings) {
		super.addSettings( settings );

		settings.put( AvailableSettings.USE_SECOND_LEVEL_CACHE, "true" );
		settings.put( AvailableSettings.CACHE_REGION_FACTORY, CachingRegionFactory.class );
		settings.put( AvailableSettings.USE_STRUCTURED_CACHE, "true" );

	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Address.class };
	}

}
