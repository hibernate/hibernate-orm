/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.querycache;

import java.util.List;

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

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = QueryCacheParametersTest.Customer.class )
@SessionFactory( generateStatistics = true )
@ServiceRegistry( settings = {
		@Setting( name = AvailableSettings.USE_QUERY_CACHE, value = "true" ),
		@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true" )
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16594" )
public class QueryCacheParametersTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Customer( 1L, "John", "Doe", "M", 123456L ) ) );
	}

	@Test
	public void testQueryCacheWithSingleCondition(SessionFactoryScope scope) {
		testQueryCacheHits( scope, true );
	}

	@Test
	public void testQueryCacheWithMultipleCondition(SessionFactoryScope scope) {
		testQueryCacheHits( scope, false );
	}

	private void testQueryCacheHits(SessionFactoryScope scope, boolean singleCondition) {
		scope.getSessionFactory().getCache().evictQueryRegions();
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		// Query once (miss and populate cache)
		scope.inTransaction( session -> executeQuery( session, singleCondition ) );

		// 0 hits, 1 miss, 1 put
		assertThat( statistics.getQueryCacheHitCount() ).isEqualTo( 0 );
		assertThat( statistics.getQueryCacheMissCount() ).isEqualTo( 1 );
		assertThat( statistics.getQueryCachePutCount() ).isEqualTo( 1 );

		// Query 10 more times with the same parameters and values
		for ( int i = 0; i < 10; i++ ) {
			scope.inTransaction( session -> executeQuery( session, singleCondition ) );
		}

		// 10 hits, 1 miss (unchanged), 1 put (unchanged)
		assertThat( statistics.getQueryCacheHitCount() ).isEqualTo( 10 );
		assertThat( statistics.getQueryCacheMissCount() ).isEqualTo( 1 );
		assertThat( statistics.getQueryCachePutCount() ).isEqualTo( 1 );
	}

	private void executeQuery(SessionImplementor session, boolean singleCondition) {
		final CriteriaBuilder cb = session.getCriteriaBuilder();
		final CriteriaQuery<Customer> cq = cb.createQuery( Customer.class );
		final Root<Customer> customer = cq.from( Customer.class );
		final Predicate predicate;
		if ( singleCondition ) {
			predicate = createSingleConditionPredicate( cb, customer );
		}
		else {
			predicate = createMultipleConditionsPredicate( cb, customer );
		}
		cq.where( predicate );
		final List<Customer> customers = session.createQuery( cq ).setCacheable( true ).getResultList();
		assertThat( customers ).hasSize( 1 );
	}

	private static Predicate createSingleConditionPredicate(CriteriaBuilder cb, Root<Customer> customer) {
		return cb.and( cb.equal( customer.get( "firstName" ), "John" ) );
	}

	private static Predicate createMultipleConditionsPredicate(CriteriaBuilder cb, Root<Customer> customer) {
		return cb.and(
				cb.equal( customer.get( "firstName" ), "John" ),
				cb.equal( customer.get( "lastName" ), "Doe" ),
				cb.equal( customer.get( "gender" ), "M" ),
				cb.equal( customer.get( "ssn" ), 123456L )
		);
	}

	@Entity( name = "Customer" )
	public static class Customer {
		@Id
		private Long id;
		private String firstName;
		private String lastName;
		private String gender;
		private Long ssn;

		public Customer() {
		}

		public Customer(Long id, String firstName, String lastName, String gender, Long ssn) {
			this.id = id;
			this.firstName = firstName;
			this.lastName = lastName;
			this.gender = gender;
			this.ssn = ssn;
		}
	}
}
