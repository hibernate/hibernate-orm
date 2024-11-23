/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache.hhh13179;

import org.hibernate.stat.CacheRegionStatistics;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Check that second level caching works for hbm mapped joined subclass inheritance structures
 */
@JiraKey("HHH-13179")
@DomainModel(
		xmlMappings = {
				"org/hibernate/orm/test/cache/hhh13179/JoinedSubclassPerson.hbm.xml",
				"org/hibernate/orm/test/cache/hhh13179/UnionSubclassPerson.hbm.xml",
				"org/hibernate/orm/test/cache/hhh13179/DiscriminatorSubclassPerson.hbm.xml"
		}
)
@SessionFactory(generateStatistics = true)
public class HHH13179Test {


	@Test
	public void testJoinedSubclassCaching(SessionFactoryScope scope) {
		// BaseCoreFunctionalTestCase automatically creates the SessionFactory and provides the Session.
		String regionName = JoinedSubclassPerson.class.getName();
		scope.inTransaction(
				session -> {
					CacheRegionStatistics cacheRegionStatistics = session.getSessionFactory()
							.getStatistics()
							.getCacheRegionStatistics(
									regionName );
					assertThat( cacheRegionStatistics.getPutCount() ).as( "Cache put should be 0" ).isEqualTo( 0 );

					JoinedSubclassPerson person1 = new JoinedSubclassUIPerson();
					person1.setOid( 1L );
					session.persist( person1 );
				}
		);

		scope.inTransaction(
				session -> {
					JoinedSubclassPerson person2 = session.get( JoinedSubclassPerson.class, 1L );

					CacheRegionStatistics cacheRegionStatistics = session.getSessionFactory()
							.getStatistics()
							.getCacheRegionStatistics( regionName );
					assertThat( cacheRegionStatistics.getHitCount() ).as( "Cache hit should be 1" ).isEqualTo( 1 );
					assertThat( cacheRegionStatistics.getPutCount() ).as( "Cache put should be 1" ).isEqualTo( 1 );
				}
		);

	}

	@Test
	public void testUnionSubclassCaching(SessionFactoryScope scope) {
		// BaseCoreFunctionalTestCase automatically creates the SessionFactory and provides the Session.
		String regionName = UnionSubclassPerson.class.getName();
		scope.inTransaction(
				session -> {
					CacheRegionStatistics cacheRegionStatistics = session.getSessionFactory()
							.getStatistics()
							.getCacheRegionStatistics(
									regionName );
					assertThat( cacheRegionStatistics.getPutCount() ).as( "Cache put should be 0" ).isEqualTo( 0 );

					UnionSubclassPerson person1 = new UnionSubclassUIPerson();
					person1.setOid( 1L );
					session.persist( person1 );
				}
		);

		scope.inTransaction(
				session -> {
					UnionSubclassPerson person2 = session.get( UnionSubclassPerson.class, 1L );

					CacheRegionStatistics cacheRegionStatistics = session.getSessionFactory()
							.getStatistics()
							.getCacheRegionStatistics( regionName );
					assertThat( cacheRegionStatistics.getHitCount() ).as( "Cache hit should be 1" ).isEqualTo( 1 );
					assertThat( cacheRegionStatistics.getPutCount() ).as( "Cache put should be 1" ).isEqualTo( 1 );
				}
		);
	}

	@Test
	public void testDiscriminatorSubclassCaching(SessionFactoryScope scope) {
		// BaseCoreFunctionalTestCase automatically creates the SessionFactory and provides the Session.
		String regionName = DiscriminatorSubclassPerson.class.getName();

		scope.inTransaction(
				session -> {
					CacheRegionStatistics cacheRegionStatistics = session.getSessionFactory()
							.getStatistics()
							.getCacheRegionStatistics(
									regionName );
					assertThat( cacheRegionStatistics.getPutCount() ).as( "Cache put should be 0" ).isEqualTo( 0 );

					DiscriminatorSubclassPerson person1 = new DiscriminatorSubclassUIPerson();
					person1.setOid( 1L );
					session.persist( person1 );

				}
		);


		scope.inTransaction(
				session -> {
					session.get( DiscriminatorSubclassPerson.class, 1L );

					CacheRegionStatistics cacheRegionStatistics = session.getSessionFactory()
							.getStatistics()
							.getCacheRegionStatistics( regionName );
					assertThat( cacheRegionStatistics.getHitCount() ).as( "Cache hit should be 1" ).isEqualTo( 1 );
					assertThat( cacheRegionStatistics.getPutCount() ).as( "Cache put should be 1" ).isEqualTo( 1 );
				}
		);
	}
}
