/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.querycache;

import java.util.function.Consumer;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = QueryCacheExistingEntityInstanceTest.TestEntity.class )
@SessionFactory( generateStatistics = true )
@ServiceRegistry( settings = {
		@Setting( name = AvailableSettings.USE_QUERY_CACHE, value = "true" ),
		@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true" )
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-17188" )
public class QueryCacheExistingEntityInstanceTest {
	private static final String TEXT = "text";
	private static final String QUERY = "select e from TestEntity e where text = :text";

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new TestEntity( 1L, TEXT ) ) );
	}

	@Test
	public void testNormalBehavior(SessionFactoryScope scope) {
		testQueryCache( scope, session -> {
		} );
	}

	@Test
	public void testAfterFind(SessionFactoryScope scope) {
		testQueryCache( scope, session -> session.find( TestEntity.class, 1L ) );
	}

	@Test
	public void testAfterGetReference(SessionFactoryScope scope) {
		testQueryCache( scope, session -> session.getReference( TestEntity.class, 1L ) );
	}

	private void testQueryCache(SessionFactoryScope scope, Consumer<SessionImplementor> beforeQuery) {
		scope.getSessionFactory().getCache().evictQueryRegions();
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inTransaction( session -> {
			beforeQuery.accept( session );
			executeQuery( session );
		} );

		assertQueryCacheStatistics( statistics, 0, 1, 1 );

		scope.inTransaction( QueryCacheExistingEntityInstanceTest::executeQuery );

		assertQueryCacheStatistics( statistics, 1, 1, 1 );
	}

	private static void executeQuery(SessionImplementor session) {
		final TestEntity entity = session.createQuery( QUERY, TestEntity.class )
				.setParameter( "text", TEXT )
				.setCacheable( true )
				.getSingleResult();
		assertEquals( TEXT, entity.getText() );
	}

	@SuppressWarnings( "SameParameterValue" )
	private static void assertQueryCacheStatistics(
			StatisticsImplementor statistics,
			int hitCount,
			int missCount,
			int putCount) {
		assertThat( statistics.getQueryCacheHitCount() ).isEqualTo( hitCount );
		assertThat( statistics.getQueryCacheMissCount() ).isEqualTo( missCount );
		assertThat( statistics.getQueryCachePutCount() ).isEqualTo( putCount );
	}

	@Entity( name = "TestEntity" )
	@Cacheable
	public static class TestEntity {
		@Id
		private Long id;

		private String text;

		public TestEntity() {
		}

		public TestEntity(Long id, String text) {
			this.id = id;
			this.text = text;
		}

		public Long getId() {
			return id;
		}

		public String getText() {
			return text;
		}
	}
}
