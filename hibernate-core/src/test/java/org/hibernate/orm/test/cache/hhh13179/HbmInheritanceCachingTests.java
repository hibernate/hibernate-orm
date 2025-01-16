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
 * Check that second level caching statistics work for inheritance hierarchies mapped using HBM
 */
@JiraKey("HHH-13179")
@DomainModel( xmlMappings = {
		"org/hibernate/orm/test/cache/hhh13179/JoinedSubclassPerson.hbm.xml",
		"org/hibernate/orm/test/cache/hhh13179/UnionSubclassPerson.hbm.xml",
		"org/hibernate/orm/test/cache/hhh13179/DiscriminatorSubclassPerson.hbm.xml"
} )
@SessionFactory(generateStatistics = true)
@SuppressWarnings("JUnitMalformedDeclaration")
public class HbmInheritanceCachingTests {

	@Test
	public void testJoinedSubclassCaching(SessionFactoryScope scope) {
		final String regionName = JoinedSubclassPerson.class.getName();
		final CacheRegionStatistics cacheRegionStatistics = scope.getSessionFactory()
				.getStatistics()
				.getCacheRegionStatistics( regionName );
		assert cacheRegionStatistics != null;
		assertThat( cacheRegionStatistics.getHitCount() ).isEqualTo( 0 );
		assertThat( cacheRegionStatistics.getPutCount() ).isEqualTo( 0 );

		scope.inTransaction( (session) -> {
			final JoinedSubclassPerson person1 = new JoinedSubclassUIPerson();
			person1.setOid( 1L );
			session.persist( person1 );
		} );

		assertThat( cacheRegionStatistics.getHitCount() ).isEqualTo( 0 );
		assertThat( cacheRegionStatistics.getPutCount() ).isEqualTo( 1 );

		scope.inTransaction((session) -> {
			final JoinedSubclassPerson person2 = session.find( JoinedSubclassPerson.class, 1L );
			assertThat( person2 ).isNotNull();
		} );

		assertThat( cacheRegionStatistics.getHitCount() ).isEqualTo( 1 );
		assertThat( cacheRegionStatistics.getPutCount() ).isEqualTo( 1 );
	}

	@Test
	public void testUnionSubclassCaching(SessionFactoryScope scope) {
		final String regionName = UnionSubclassPerson.class.getName();
		final CacheRegionStatistics cacheRegionStatistics = scope.getSessionFactory()
				.getStatistics()
				.getCacheRegionStatistics( regionName );
		assert cacheRegionStatistics != null;
		assertThat( cacheRegionStatistics.getHitCount() ).isEqualTo( 0 );
		assertThat( cacheRegionStatistics.getPutCount() ).isEqualTo( 0 );

		scope.inTransaction( (session) -> {
			final UnionSubclassPerson person1 = new UnionSubclassUIPerson();
			person1.setOid( 1L );
			session.persist( person1 );
		} );

		assertThat( cacheRegionStatistics.getHitCount() ).isEqualTo( 0 );
		assertThat( cacheRegionStatistics.getPutCount() ).isEqualTo( 1 );

		scope.inTransaction( (session) -> {
			UnionSubclassPerson person2 = session.find( UnionSubclassPerson.class, 1L );
			assertThat( person2 ).isNotNull();
		} );

		assertThat( cacheRegionStatistics.getHitCount() ).isEqualTo( 1 );
		assertThat( cacheRegionStatistics.getPutCount() ).isEqualTo( 1 );
	}

	@Test
	public void testDiscriminatorSubclassCaching(SessionFactoryScope scope) {
		final String regionName = DiscriminatorSubclassPerson.class.getName();
		final CacheRegionStatistics cacheRegionStatistics = scope.getSessionFactory()
				.getStatistics()
				.getCacheRegionStatistics( regionName );
		assert cacheRegionStatistics != null;
		assertThat( cacheRegionStatistics.getHitCount() ).isEqualTo( 0 );
		assertThat( cacheRegionStatistics.getPutCount() ).isEqualTo( 0 );

		scope.inTransaction( (session) -> {
			DiscriminatorSubclassPerson person1 = new DiscriminatorSubclassUIPerson();
			person1.setOid( 1L );
			session.persist( person1 );
		} );

		assertThat( cacheRegionStatistics.getHitCount() ).isEqualTo( 0 );
		assertThat( cacheRegionStatistics.getPutCount() ).isEqualTo( 1 );

		scope.inTransaction( (session) -> {
			final DiscriminatorSubclassPerson found = session.find( DiscriminatorSubclassPerson.class, 1L );
			assertThat( found ).isNotNull();
		} );

		assertThat( cacheRegionStatistics.getHitCount() ).isEqualTo( 1 );
		assertThat( cacheRegionStatistics.getPutCount() ).isEqualTo( 1 );
	}
}
