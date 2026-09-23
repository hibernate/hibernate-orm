/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.selectcase;

import java.util.Date;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@JiraKey(value = "HHH-14702")
@Jpa(annotatedClasses = { SelectCaseEnumMultiselectTest.Subscription.class })
public class SelectCaseEnumMultiselectTest {

	@BeforeEach
	public void setupData(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Subscription future = new Subscription();
			future.setStartDate( new Date( System.currentTimeMillis() + 86400000L ) );
			future.setSubscriptionStatus( SubscriptionStatus.BOOKED );
			entityManager.persist( future );

			Subscription past = new Subscription();
			past.setStartDate( new Date( System.currentTimeMillis() - 86400000L ) );
			future.setSubscriptionStatus( SubscriptionStatus.ACTIVE );
			entityManager.persist( past );
		} );
	}

	@AfterEach
	public void cleanupData(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-13237")
	public void testSearchedCaseWithEnumResultInMultiselect(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Tuple> query = cb.createTupleQuery();
			Root<Subscription> root = query.from( Subscription.class );

			Expression<SubscriptionStatus> status = cb.selectCase(SubscriptionStatus.class)
					.when(
							cb.greaterThan( root.get( "startDate" ), cb.currentTimestamp() ),
							SubscriptionStatus.BOOKED
					)
					.otherwise( SubscriptionStatus.ACTIVE );
			Expression<Boolean> active = cb.selectCase( Boolean.class)
					.when(
							cb.equal( root.get( "subscriptionStatus" ), SubscriptionStatus.ACTIVE ),
							true
					)
					.otherwise( false );

			query.multiselect( root.get( "id" ), status, active );
			query.orderBy( cb.asc( root.get( "id" ) ) );

			List<Tuple> results = entityManager.createQuery( query ).getResultList();
			assertEquals( 2, results.size() );
			for ( Tuple tuple : results ) {
				assertNotNull( tuple.get( 0 ) );
				assertNotNull( tuple.get( 1 ) );
				assertNotNull( tuple.get( 2 ) );
			}
		} );
	}

	@Test
	public void testSearchedCaseWithEnumResultCastInMultiselect(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Tuple> query = cb.createTupleQuery();
			Root<Subscription> root = query.from( Subscription.class );

			Expression<SubscriptionStatus> status1 = cb.selectCase(SubscriptionStatus.class)
					.when(
							cb.greaterThan( root.get( "startDate" ), cb.currentTimestamp() ),
							SubscriptionStatus.BOOKED
					)
					.otherwise( SubscriptionStatus.ACTIVE )
					.as( SubscriptionStatus.class );

			query.multiselect( root.get( "id" ), status1 );
			query.orderBy( cb.asc( root.get( "id" ) ) );

			List<Tuple> results = entityManager.createQuery( query ).getResultList();
			assertEquals( 2, results.size() );
			for ( Tuple tuple : results ) {
				assertNotNull( tuple.get( 0 ) );
				assertNotNull( tuple.get( 1 ) );
			}
		} );
	}

	@Test
	public void testSimpleCaseWithEnumResultInMultiselect(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Tuple> query = cb.createTupleQuery();
			Root<Subscription> root = query.from( Subscription.class );

			Expression<SubscriptionStatus> status = cb.selectCase( root.<Integer>get( "id" ), SubscriptionStatus.class )
					.when( 1, SubscriptionStatus.BOOKED )
					.otherwise( SubscriptionStatus.ACTIVE );

			query.multiselect( root.get( "id" ), status );
			query.orderBy( cb.asc( root.get( "id" ) ) );

			List<Tuple> results = entityManager.createQuery( query ).getResultList();
			assertEquals( 2, results.size() );
			for ( Tuple tuple : results ) {
				assertNotNull( tuple.get( 0 ) );
				assertNotNull( tuple.get( 1 ) );
			}
		} );
	}

	public enum SubscriptionStatus {
		BOOKED,
		ACTIVE
	}

	@Entity(name = "Subscription")
	@Table(name = "subscription")
	public static class Subscription {

		@Id
		@GeneratedValue
		private Integer id;

		@Temporal(TemporalType.TIMESTAMP)
		@Column(name = "start_date", nullable = false)
		private Date startDate;

		private SubscriptionStatus subscriptionStatus;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Date getStartDate() {
			return startDate;
		}

		public void setStartDate(Date startDate) {
			this.startDate = startDate;
		}

		public SubscriptionStatus getSubscriptionStatus() {
			return subscriptionStatus;
		}

		public void setSubscriptionStatus(SubscriptionStatus subscriptionStatus) {
			this.subscriptionStatus = subscriptionStatus;
		}
	}
}
