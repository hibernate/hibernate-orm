/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.query;

import java.time.Instant;
import java.util.Date;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.jpa.test.Wallet;
import org.hibernate.jpa.test.Wallet_;

import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

@TestForIssue(jiraKey = "HHH-15113")
public class ReuseCriteriaWithMixedParametersTest extends BaseEntityManagerFunctionalTestCase {

	@Rule
	public ExpectedException illegalString = ExpectedException.none();

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Wallet.class
		};
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
			//This throws due to ParameterExpressionImpl#name change
			//causing marketEntrance literal to get the same name.
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
			//This throws due to ParameterExpressionImpl#name change
			//causing like-escape literal to get the same name.
			query.setParameter( stringValueParameter, "A%" );

			query.getResultList();

		} );
	}

	@Test
	public void predicateReuse() {
		//TODO: Unwanted behaviour.
		illegalString.expect( IllegalArgumentException.class );
		illegalString.expectMessage( "did not match expected type [java.lang.String (n/a)]" );
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
			Assert.assertTrue( "No error", true );

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
			//This throws due to ParameterExpressionImpl#name change
			//causing marketEntrance parameter to get the same name.
			query.setParameter( dateValueParameter, Date.from( Instant.EPOCH ) );

			query.getResultList();
		} );
	}

}
