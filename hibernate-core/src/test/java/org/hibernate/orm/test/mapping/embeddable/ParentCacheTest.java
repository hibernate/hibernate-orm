/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable;

import org.hibernate.annotations.Parent;
import org.hibernate.cfg.AvailableSettings;
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
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = { ParentCacheTest.ChildEmbeddable.class, ParentCacheTest.ParentEntity.class } )
@SessionFactory( generateStatistics = true )
@ServiceRegistry( settings = @Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true" ) )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16879" )
public class ParentCacheTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new ParentEntity( new ChildEmbeddable( "test" ) ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from ParentEntity" ).executeUpdate() );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.getSessionFactory().getCache().evictEntityData();
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inTransaction( session -> {
			final ParentEntity parent = session.find( ParentEntity.class, 1L );
			final ChildEmbeddable embeddable = parent.getChild();
			assertThat( embeddable.getParent() ).isNotNull();
		} );

		assertThat( statistics.getSecondLevelCacheHitCount() ).isEqualTo( 0 );
		assertThat( statistics.getSecondLevelCacheMissCount() ).isEqualTo( 1L );
		assertThat( statistics.getSecondLevelCachePutCount() ).isEqualTo( 1L );

		scope.inTransaction( session -> {
			final ParentEntity parent = session.find( ParentEntity.class, 1L );
			final ChildEmbeddable embeddable = parent.getChild();
			assertThat( embeddable.getParent() ).isNotNull();
		} );

		assertThat( statistics.getSecondLevelCacheHitCount() ).isEqualTo( 1L );
	}

	@Embeddable
	public static class ChildEmbeddable {
		@Parent
		public ParentEntity parent;

		public String field;

		public ChildEmbeddable() {
		}

		public ChildEmbeddable(String field) {
			this.field = field;
		}

		public ParentEntity getParent() {
			return parent;
		}

		public void setParent(ParentEntity parent) {
			this.parent = parent;
		}
	}

	@Entity( name = "ParentEntity" )
	@Cacheable
	public static class ParentEntity {
		@Id
		@GeneratedValue
		public Long id;

		@Embedded
		public ChildEmbeddable child;

		public ParentEntity() {
		}

		public ParentEntity(ChildEmbeddable child) {
			this.child = child;
		}

		public ChildEmbeddable getChild() {
			return child;
		}
	}
}
