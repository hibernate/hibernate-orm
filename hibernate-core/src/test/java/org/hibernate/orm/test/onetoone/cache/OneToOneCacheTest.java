/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetoone.cache;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(
		xmlMappings = {
				"org/hibernate/orm/test/onetoone/cache/Details.hbm.xml",
				"org/hibernate/orm/test/onetoone/cache/Person.hbm.xml",
		}
)
@SessionFactory(
		generateStatistics = true
)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
		}
)
public class OneToOneCacheTest {

	private <TPerson extends Person, TDetails extends Details> void OneToOneTest(
			Class<TPerson> personClass,
			Class<TDetails> detailsClass,
			SessionFactoryScope scope) throws Exception {

		// Initialize the database with data.
		List<Object> ids = createPersonsAndDetails( personClass, detailsClass, scope );

		// Clear the second level cache and the statistics.
		SessionFactoryImplementor sfi = scope.getSessionFactory();
		CacheImplementor cache = sfi.getCache();
		StatisticsImplementor statistics = sfi.getStatistics();

		cache.evictEntityData( personClass );
		cache.evictEntityData( detailsClass );
		cache.evictQueryRegions();

		statistics.clear();

		// Fill the empty caches with data.
		this.getPersons( personClass, ids, scope );

		// Verify that no data was retrieved from the cache.
		assertEquals( 0, statistics.getSecondLevelCacheHitCount(), "Second level cache hit count" );

		statistics.clear();

		this.getPersons( personClass, ids, scope );

		// Verify that all data was retrieved from the cache.
		assertEquals( 0, statistics.getSecondLevelCacheMissCount(), "Second level cache miss count" );
	}

	private <TPerson extends Person, TDetails extends Details> List<Object> createPersonsAndDetails(
			Class<TPerson> personClass,
			Class<TDetails> detailsClass,
			SessionFactoryScope scope) throws Exception {
		Constructor<TPerson> ctorPerson = personClass.getConstructor();
		Constructor<TDetails> ctorDetails = detailsClass.getConstructor();
		List<Object> ids = new ArrayList<>();
		return scope.fromTransaction(
				session -> {
					for ( int i = 0; i < 6; i++ ) {
						Person person;
						try {
							person = ctorPerson.newInstance();

							if ( i % 2 == 0 ) {
								Details details = ctorDetails.newInstance();

								details.setData( String.format( "%s%d", detailsClass.getName(), i ) );
								person.setDetails( details );
							}

							person.setName( String.format( "%s%d", personClass.getName(), i ) );

							session.persist( person );
							ids.add( person.getId() );
						}
						catch (Exception e) {
							throw new RuntimeException( e );
						}
					}
					return ids;
				}
		);


	}

	private <TPerson extends Person> List<TPerson> getPersons(
			Class<TPerson> personClass,
			List<Object> ids, SessionFactoryScope scope) {
		return scope.fromTransaction(
				session -> {
					List<TPerson> people = new ArrayList<>();

					for ( Object id : ids ) {
						people.add( session.get( personClass, id ) );
					}

					return people;
				}
		);
	}

	@Test
	@FailureExpected( jiraKey = "HHH-14216", reason = "The changes introduces by HHH-14216 have been reverted see https://github.com/hibernate/hibernate-orm/pull/5061 discussion")
	public void OneToOneCacheByForeignKey(SessionFactoryScope scope) throws Exception {
		OneToOneTest( PersonByFK.class, DetailsByFK.class, scope );
	}

	@Test
	public void OneToOneCacheByRef(SessionFactoryScope scope) throws Exception {
		OneToOneTest( PersonByRef.class, DetailsByRef.class, scope );
	}
}
