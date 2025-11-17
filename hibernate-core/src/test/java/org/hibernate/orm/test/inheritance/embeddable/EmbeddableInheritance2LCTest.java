/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance.embeddable;

import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		EmbeddableInheritance2LCTest.TestEntity.class,
		ParentEmbeddable.class,
		ChildOneEmbeddable.class,
		SubChildOneEmbeddable.class,
		ChildTwoEmbeddable.class,
} )
@ServiceRegistry( settings = @Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true" ) )
@SessionFactory( generateStatistics = true )
public class EmbeddableInheritance2LCTest {
	@Test
	public void testFind(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		final CacheImplementor cache = scope.getSessionFactory().getCache();
		cache.evictEntityData();
		scope.inTransaction( session -> {
			// load the entity in cache
			session.find( TestEntity.class, 1L );
			assertThat( statistics.getSecondLevelCachePutCount() ).isEqualTo( 1 );
		} );
		scope.inTransaction( session -> {
			final TestEntity result = session.find( TestEntity.class, 1L );
			assertThat( statistics.getSecondLevelCacheHitCount() ).isEqualTo( 1 );
			assertThat( result.getEmbeddable().getParentProp() ).isEqualTo( "embeddable_1" );
			assertThat( result.getEmbeddable() ).isExactlyInstanceOf( SubChildOneEmbeddable.class );
			assertThat( ( (SubChildOneEmbeddable) result.getEmbeddable() ).getChildOneProp() ).isEqualTo( 1 );
			assertThat( ( (SubChildOneEmbeddable) result.getEmbeddable() ).getSubChildOneProp() ).isEqualTo( 1.0 );
		} );
	}

	@Test
	public void testQuery(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		final CacheImplementor cache = scope.getSessionFactory().getCache();
		cache.evictEntityData();
		scope.inTransaction( session -> {
			session.createQuery( "from TestEntity where id = 2", TestEntity.class ).getSingleResult();
			assertThat( statistics.getSecondLevelCachePutCount() ).isEqualTo( 1 );
		} );
		scope.inTransaction( session -> {
			final TestEntity result = session.find( TestEntity.class, 2L );
			assertThat( statistics.getSecondLevelCacheHitCount() ).isEqualTo( 1 );
			assertThat( result.getEmbeddable().getParentProp() ).isEqualTo( "embeddable_2" );
			assertThat( result.getEmbeddable() ).isExactlyInstanceOf( ChildTwoEmbeddable.class );
			assertThat( ( (ChildTwoEmbeddable) result.getEmbeddable() ).getChildTwoProp() ).isEqualTo( 2L );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new TestEntity( 1L, new SubChildOneEmbeddable( "embeddable_1", 1, 1.0 ) ) );
			session.persist( new TestEntity( 2L, new ChildTwoEmbeddable( "embeddable_2", 2L ) ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from TestEntity" ).executeUpdate() );
	}

	@Entity( name = "TestEntity" )
	@Cacheable
	static class TestEntity {
		@Id
		private Long id;

		@Embedded
		private ParentEmbeddable embeddable;

		public TestEntity() {
		}

		public TestEntity(Long id, ParentEmbeddable embeddable) {
			this.id = id;
			this.embeddable = embeddable;
		}

		public ParentEmbeddable getEmbeddable() {
			return embeddable;
		}

		public void setEmbeddable(ParentEmbeddable embeddable) {
			this.embeddable = embeddable;
		}
	}
}
