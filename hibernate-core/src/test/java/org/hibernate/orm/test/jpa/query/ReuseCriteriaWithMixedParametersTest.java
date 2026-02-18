/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.hibernate.orm.test.jpa.Wallet;
import org.hibernate.orm.test.jpa.Wallet_;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertEquals;


@Jpa(
		annotatedClasses = { Wallet.class, ReuseCriteriaWithMixedParametersTest.Person.class }
)
@JiraKey(value = "HHH-15142")
public class ReuseCriteriaWithMixedParametersTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void cqReuse(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
			final CriteriaQuery<Wallet> criteriaQuery = criteriaBuilder.createQuery( Wallet.class );
			final Root<Wallet> root = criteriaQuery.from( Wallet.class );

			final ParameterExpression<String> stringValueParameter = criteriaBuilder.parameter( String.class );

			criteriaQuery.where(
					criteriaBuilder.like(
							root.get( Wallet_.model ),
							stringValueParameter
					),
					criteriaBuilder.lessThan(
							root.get( Wallet_.marketEntrance ),
							criteriaBuilder.literal( Date.from( Instant.EPOCH ) )
					)
			);

			Query query = entityManager.createQuery( criteriaQuery );
			query.setParameter( stringValueParameter, "Z%" );

			query.getResultList();

			query = entityManager.createQuery( criteriaQuery );
			query.setParameter( stringValueParameter, "A%" );

			query.getResultList();

		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsArbitraryEscapeCharInLike.class)
	public void likeCqReuse(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {

			final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
			final CriteriaQuery<Wallet> criteriaQuery = criteriaBuilder.createQuery( Wallet.class );
			final Root<Wallet> root = criteriaQuery.from( Wallet.class );

			final ParameterExpression<String> stringValueParameter = criteriaBuilder.parameter( String.class );

			criteriaQuery.where(
					criteriaBuilder.like(
							root.get( Wallet_.model ),
							stringValueParameter,
							'/'
					)
			);

			Query query = entityManager.createQuery( criteriaQuery );
			query.setParameter( stringValueParameter, "Z%" );

			query.getResultList();

			query = entityManager.createQuery( criteriaQuery );
			query.setParameter( stringValueParameter, "A%" );

			query.getResultList();

		} );
	}

	@Test
	public void predicateReuse(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
			final CriteriaQuery<Wallet> criteriaQuery = criteriaBuilder.createQuery( Wallet.class );
			final Root<Wallet> root = criteriaQuery.from( Wallet.class );

			final ParameterExpression<String> stringValueParameter = criteriaBuilder.parameter( String.class );
			final ParameterExpression<Date> dateValueParameter = criteriaBuilder.parameter( Date.class );

			criteriaQuery.where(
					criteriaBuilder.like(
							root.get( Wallet_.model ),
							stringValueParameter
					)
			);

			Query query = entityManager.createQuery( criteriaQuery );
			query.setParameter( stringValueParameter, "Z%" );

			query.getResultList();

			criteriaQuery.where(
					criteriaBuilder.like(
							root.get( Wallet_.model ),
							stringValueParameter
					),
					criteriaBuilder.lessThan(
							root.get( Wallet_.marketEntrance ),
							dateValueParameter
					)
			);

			query = entityManager.createQuery( criteriaQuery );
			query.setParameter( stringValueParameter, "A%" );
			query.setParameter( dateValueParameter, Date.from( Instant.EPOCH ) );

			query.getResultList();
		} );
	}

	@Test
	public void testLikePredicate(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.persist( new Person( "Person 1" ) );
					entityManager.persist( new Person( "Person 2" ) );
				}
		);

		scope.inTransaction(
				entityManager -> {
					final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					final CriteriaQuery<Person> personQuery = cb.createQuery( Person.class );
					final Root<Person> root = personQuery.from( Person.class );
					final ParameterExpression<String> pattern = cb.parameter( String.class );
					CriteriaQuery<Person> criteriaQuery = personQuery
							.where( cb.like(
									root.get( "name" ),
									pattern,
									cb.literal( '\\' )
							) );
					for ( int i = 0; i < 2; i++ ) {
						final TypedQuery<Person> query = entityManager.createQuery( criteriaQuery );
						query.setParameter( pattern, "%_1" );
						final List<Person> result = query.getResultList();

						assertEquals( 1, result.size() );
					}
				}
		);

	}

	@Entity(name = "Person")
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

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
