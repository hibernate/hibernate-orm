/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.criteria.hhh12685;

import javax.persistence.Parameter;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.hibernate.test.criteria.Bid;
import org.hibernate.test.criteria.Item;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertFalse;

public class CriteriaQueryIsBoundManualParameterTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Bid.class, // Reuse to not create new
				Item.class
		};
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12685")
	public void testCriteriaQueryParameterIsBoundCheckNotFails() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			CriteriaQuery<Item> query = builder.createQuery( Item.class );
			Root<Item> root = query.from( Item.class );
			ParameterExpression<String> parameter = builder.parameter( String.class, "name" );
			Predicate predicate = builder.equal( root.get( "name" ), parameter );
			query.where( predicate );
			TypedQuery<Item> criteriaQuery = entityManager.createQuery( query );
			Parameter<?> dynamicParameter = criteriaQuery.getParameter( "name" );
			boolean bound = criteriaQuery.isBound( dynamicParameter );
			assertFalse( bound );
		} );
	}

}
