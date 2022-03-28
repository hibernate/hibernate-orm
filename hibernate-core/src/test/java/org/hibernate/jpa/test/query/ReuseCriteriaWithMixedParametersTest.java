/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.query;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.jpa.test.Wallet;
import org.hibernate.jpa.test.Wallet_;

import org.hibernate.testing.TestForIssue;
import org.junit.After;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

@TestForIssue(jiraKey = "HHH-15142")
public class ReuseCriteriaWithMixedParametersTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Wallet.class,
				Person.class
		};
	}

	@After
	public void tearDown() {
		doInJPA( this::entityManagerFactory, entityManager -> {
					 entityManager.createQuery( "delete from Person" ).executeUpdate();
				 }
		);
	}

	@Test
	public void cqReuse() {
		doInJPA( this::entityManagerFactory, entityManager -> {
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
	public void likeCqReuse() {
		doInJPA( this::entityManagerFactory, entityManager -> {
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
	public void predicateReuse() {
		doInJPA( this::entityManagerFactory, entityManager -> {
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
	public void testLikePredicate() {
		doInJPA( this::entityManagerFactory, entityManager -> {

					 entityManager.persist( new Person( "Person 1" ) );
					 entityManager.persist( new Person( "Person 2" ) );
				 }
		);

		doInJPA( this::entityManagerFactory, entityManager -> {
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
