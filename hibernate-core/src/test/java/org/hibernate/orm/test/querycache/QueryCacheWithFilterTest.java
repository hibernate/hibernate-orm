/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.querycache;

import java.util.List;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
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

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel( annotatedClasses = QueryCacheWithFilterTest.Person.class )
@SessionFactory( generateStatistics = true )
@ServiceRegistry( settings = {
		@Setting( name = AvailableSettings.USE_QUERY_CACHE, value = "true" ),
		@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true" )
} )
public class QueryCacheWithFilterTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Person( "John" ) ) );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from Person" ).executeUpdate() );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-16385" )
	public void testQueryCacheKeyIsImmutable(SessionFactoryScope scope) {
		scope.getSessionFactory().getCache().evictQueryRegions();
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inTransaction( session -> {
			session.enableFilter( "personName" ).setParameter( "name", "John" );
			executeQuery( session, 1 );
			// cache query key should not be affected by changing enabled filters
			session.enableFilter( "personName2" ).setParameter( "name", "John2" );
		} );

		assertThat( statistics.getQueryCacheHitCount() ).isEqualTo( 0 );
		assertThat( statistics.getQueryCacheMissCount() ).isEqualTo( 1 );
		assertThat( statistics.getQueryCachePutCount() ).isEqualTo( 1 );

		scope.inTransaction( session -> {
			session.enableFilter( "personName" ).setParameter( "name", "John" );
			executeQuery( session, 1 );
		} );

		assertThat( statistics.getQueryCacheHitCount() ).isEqualTo( 1 );
		assertThat( statistics.getQueryCacheMissCount() ).isEqualTo( 1 );
		assertThat( statistics.getQueryCachePutCount() ).isEqualTo( 1 );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-16617" )
	public void testQueryCacheDifferentFilterParams(SessionFactoryScope scope) {
		scope.getSessionFactory().getCache().evictQueryRegions();
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inTransaction( session -> {
			session.enableFilter( "personName" ).setParameter( "name", "John" );
			executeQuery( session, 1 );
		} );

		assertThat( statistics.getQueryCacheHitCount() ).isEqualTo( 0 );
		assertThat( statistics.getQueryCacheMissCount() ).isEqualTo( 1 );
		assertThat( statistics.getQueryCachePutCount() ).isEqualTo( 1 );

		scope.inTransaction( session -> {
			session.enableFilter( "personName" ).setParameter( "name", "Jack" );
			executeQuery( session, 0 );
		} );

		assertThat( statistics.getQueryCacheHitCount() ).isEqualTo( 0 );
		assertThat( statistics.getQueryCacheMissCount() ).isEqualTo( 2 );
		assertThat( statistics.getQueryCachePutCount() ).isEqualTo( 2 );
	}

	private void executeQuery(SessionImplementor session, int expectedResults) {
		final List<Person> resultList = session.createQuery(
				"from Person p",
				Person.class
		).setCacheable( true ).getResultList();
		assertThat( resultList ).hasSize( expectedResults );
	}

	@Entity( name = "Person" )
	@FilterDef( name = "personName", parameters = @ParamDef( name = "name", type = String.class ) )
	@Filter( name = "personName", condition = "name = :name" )
	@FilterDef( name = "personName2", parameters = @ParamDef( name = "name", type = String.class ) )
	@Filter( name = "personName2", condition = "name = :name" )
	public static class Person {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public Person() {
		}

		public Person(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}
}
