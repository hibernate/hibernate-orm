/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.querycache;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		BasicEntity.class
} )
@SessionFactory( generateStatistics = true )
@ServiceRegistry( settings = {
		@Setting( name = AvailableSettings.USE_QUERY_CACHE, value = "true" ),
		@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true" )
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-18439" )
public class QueryCacheNullValueTest {
	@Test
	public void testNullProperty(SessionFactoryScope scope) {
		executeQuery( scope, "select data from BasicEntity" );
	}

	@Test
	public void testNullLiteral(SessionFactoryScope scope) {
		executeQuery( scope, "select null from BasicEntity" );
	}

	private static void executeQuery(SessionFactoryScope scope, String hql) {
		scope.getSessionFactory().getCache().evictQueryRegions();
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		for ( int i = 0; i < 2; i++ ) {
			final int hitCount = i;
			scope.inTransaction( session -> {
				assertThat( session.createQuery( hql, String.class )
									.setCacheable( true )
									.getSingleResult() ).isNull();
				// 0 hits, 1 miss, 1 put
				assertThat( statistics.getQueryCacheHitCount() ).isEqualTo( hitCount );
				assertThat( statistics.getQueryCacheMissCount() ).isEqualTo( 1 );
				assertThat( statistics.getQueryCachePutCount() ).isEqualTo( 1 );
				session.clear();
			} );
		}
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new BasicEntity( 1, null ) ) );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}
}
