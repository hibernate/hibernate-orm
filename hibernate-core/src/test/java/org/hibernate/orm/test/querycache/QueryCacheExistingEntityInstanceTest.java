/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.querycache;

import java.util.function.Consumer;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		QueryCacheExistingEntityInstanceTest.TestEntity.class,
		QueryCacheExistingEntityInstanceTest.ChildEntity.class,
		QueryCacheExistingEntityInstanceTest.ParentEntity.class,
} )
@SessionFactory( generateStatistics = true )
@ServiceRegistry( settings = {
		@Setting( name = AvailableSettings.USE_QUERY_CACHE, value = "true" ),
		@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true" )
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-17188" )
@Jira( "https://hibernate.atlassian.net/browse/HHH-17329" )
@Jira( "https://hibernate.atlassian.net/browse/HHH-17359" )
public class QueryCacheExistingEntityInstanceTest {
	private static final String TEXT = "text";
	private static final String QUERY = "select e from TestEntity e where text = :text";

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ChildEntity child = new ChildEntity( "child_1" );
			session.persist( child );
			final TestEntity testEntity = new TestEntity( 1L, TEXT, child );
			session.persist( testEntity );
			session.persist( new ParentEntity( 1L, testEntity ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from ParentEntity" ).executeUpdate();
			session.createMutationQuery( "delete from TestEntity" ).executeUpdate();
			session.createMutationQuery( "delete from ChildEntity" ).executeUpdate();
		} );
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

	@Test
	public void testAfterParentFind(SessionFactoryScope scope) {
		testQueryCache( scope, session -> {
			session.find( ParentEntity.class, 1L );
			// make TestEntity instance available in PC
			session.find( TestEntity.class, 1L );
		} );
	}

	@Test
	public void testAfterParentQuery(SessionFactoryScope scope) {
		testQueryCache( scope, session -> {
			session.createQuery(
					"from ParentEntity",
					ParentEntity.class
			).getResultList();
			// make TestEntity instance available in PC
			session.find( TestEntity.class, 1L );
		} );
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
		assertThat( entity.getText() ).isEqualTo( TEXT );
		assertThat( entity.getChild().getName() ).isEqualTo( "child_1" );
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

		@ManyToOne
		@JoinColumn
		private ChildEntity child;

		public TestEntity() {
		}

		public TestEntity(Long id, String text, ChildEntity child) {
			this.id = id;
			this.text = text;
			this.child = child;
		}

		public Long getId() {
			return id;
		}

		public String getText() {
			return text;
		}

		public ChildEntity getChild() {
			return child;
		}
	}

	@Entity( name = "ChildEntity" )
	public static class ChildEntity {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public ChildEntity() {
		}

		public ChildEntity(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	@Entity( name = "ParentEntity" )
	public static class ParentEntity {
		@Id
		private Long id;

		@ManyToOne( fetch = FetchType.LAZY )
		@Fetch( FetchMode.SELECT )
		@JoinColumn( name = "test_entity_id" )
		private TestEntity testEntity;

		public ParentEntity() {
		}

		public ParentEntity(Long id, TestEntity testEntity) {
			this.id = id;
			this.testEntity = testEntity;
		}

		public TestEntity getTestEntity() {
			return testEntity;
		}
	}
}
