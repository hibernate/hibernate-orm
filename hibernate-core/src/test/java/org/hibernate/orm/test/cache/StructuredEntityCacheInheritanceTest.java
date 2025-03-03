/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.EntityStatistics;
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

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		StructuredEntityCacheInheritanceTest.MainEntity.class,
		StructuredEntityCacheInheritanceTest.SubEntity.class,
} )
@ServiceRegistry( settings = {
		@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true" ),
		@Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
		@Setting( name = AvailableSettings.USE_STRUCTURED_CACHE, value = "true" ),
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16714" )
public class StructuredEntityCacheInheritanceTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new MainEntity( 1L, "main_1" ) );
			session.persist( new SubEntity( 2L, "main_2", "sub" ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from MainEntity" ).executeUpdate() );
	}

	@Test
	public void testMainEntity(SessionFactoryScope scope) {
		scope.getSessionFactory().getCache().evictAllRegions();
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		final EntityStatistics entityStatistics = statistics.getEntityStatistics( MainEntity.class.getName() );

		scope.inTransaction( session -> {
			final MainEntity mainEntity = session.get( MainEntity.class, 1L );
			assertThat( mainEntity.getMainProp() ).isEqualTo( "main_1" );
		} );

		// 0 hits, 1 miss, 1 put
		assertThat( entityStatistics.getCacheHitCount() ).isEqualTo( 0 );
		assertThat( entityStatistics.getCacheMissCount() ).isEqualTo( 1 );
		assertThat( entityStatistics.getCachePutCount() ).isEqualTo( 1 );

		scope.inTransaction( session -> {
			final MainEntity subEntity = session.get( MainEntity.class, 1L );
			assertThat( subEntity.getMainProp() ).isEqualTo( "main_1" );
		} );

		// 1 hit, 1 miss (unchanged), 1 put (unchanged)
		assertThat( entityStatistics.getCacheHitCount() ).isEqualTo( 1 );
		assertThat( entityStatistics.getCacheMissCount() ).isEqualTo( 1 );
		assertThat( entityStatistics.getCachePutCount() ).isEqualTo( 1 );
	}

	@Test
	public void testSubEntity(SessionFactoryScope scope) {
		scope.getSessionFactory().getCache().evictAllRegions();
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		final EntityStatistics entityStatistics = statistics.getEntityStatistics( MainEntity.class.getName() );

		scope.inTransaction( session -> {
			final SubEntity subEntity = session.get( SubEntity.class, 2L );
			assertThat( subEntity.getMainProp() ).isEqualTo( "main_2" );
			assertThat( subEntity.getSubProp() ).isEqualTo( "sub" );
		} );

		// 0 hits, 1 miss, 1 put
		assertThat( entityStatistics.getCacheHitCount() ).isEqualTo( 0 );
		assertThat( entityStatistics.getCacheMissCount() ).isEqualTo( 1 );
		assertThat( entityStatistics.getCachePutCount() ).isEqualTo( 1 );

		scope.inTransaction( session -> {
			final SubEntity subEntity = session.get( SubEntity.class, 2L );
			assertThat( subEntity.getMainProp() ).isEqualTo( "main_2" );
			assertThat( subEntity.getSubProp() ).isEqualTo( "sub" );
		} );

		// 1 hit, 1 miss (unchanged), 1 put (unchanged)
		assertThat( entityStatistics.getCacheHitCount() ).isEqualTo( 1 );
		assertThat( entityStatistics.getCacheMissCount() ).isEqualTo( 1 );
		assertThat( entityStatistics.getCachePutCount() ).isEqualTo( 1 );
	}

	@Test
	public void testSubEntityInsertedDuringTransaction(SessionFactoryScope scope) {
		scope.getSessionFactory().getCache().evictAllRegions();
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		final EntityStatistics entityStatistics = statistics.getEntityStatistics( MainEntity.class.getName() );

		scope.inTransaction( session -> {
			session.persist( new SubEntity( 3L, "main_3", "another_sub" ) );
			session.flush();
			session.clear();
			final SubEntity subEntity = session.get( SubEntity.class, 3L );
			assertThat( subEntity.getMainProp() ).isEqualTo( "main_3" );
			assertThat( subEntity.getSubProp() ).isEqualTo( "another_sub" );
		} );

		// 1 hit, 0 misses, 1 put
		assertThat( entityStatistics.getCacheHitCount() ).isEqualTo( 1 );
		assertThat( entityStatistics.getCacheMissCount() ).isEqualTo( 0 );
		assertThat( entityStatistics.getCachePutCount() ).isEqualTo( 1 );

		scope.inTransaction( session -> {
			final SubEntity subEntity = session.get( SubEntity.class, 3L );
			assertThat( subEntity.getMainProp() ).isEqualTo( "main_3" );
			assertThat( subEntity.getSubProp() ).isEqualTo( "another_sub" );
		} );

		// 2 hits, 0 misses (unchanged), 1 put (unchanged)
		assertThat( entityStatistics.getCacheHitCount() ).isEqualTo( 2 );
		assertThat( entityStatistics.getCacheMissCount() ).isEqualTo( 0 );
		assertThat( entityStatistics.getCachePutCount() ).isEqualTo( 1 );
	}

	@Entity( name = "MainEntity" )
	@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
	@DiscriminatorColumn( name = "disc_col", discriminatorType = DiscriminatorType.STRING )
	@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
	public static class MainEntity {
		@Id
		private Long id;

		private String mainProp;

		public MainEntity() {
		}

		public MainEntity(Long id, String mainProp) {
			this.id = id;
			this.mainProp = mainProp;
		}

		public String getMainProp() {
			return mainProp;
		}
	}

	@Entity( name = "SubEntity" )
	@DiscriminatorValue( "sub" )
	public static class SubEntity extends MainEntity {
		private String subProp;

		public SubEntity() {
		}

		public SubEntity(Long id, String mainProp, String subProp) {
			super( id, mainProp );
			this.subProp = subProp;
		}

		public String getSubProp() {
			return subProp;
		}
	}
}
