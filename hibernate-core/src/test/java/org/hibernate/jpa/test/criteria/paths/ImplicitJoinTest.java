/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria.paths;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@Jpa(annotatedClasses = {
		Order.class,
		LineItem.class
})
public class ImplicitJoinTest {

	@Test
	public void testImplicitJoinFromExplicitCollectionJoin(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
					CriteriaQuery<Order> criteria = criteriaBuilder.createQuery( Order.class );
					Root<Order> orderRoot = criteria.from( Order.class );
					Join<Order, LineItem> lineItemsJoin = orderRoot.join( Order_.lineItems );
					criteria.where( criteriaBuilder.lt( lineItemsJoin.get( LineItem_.quantity ), 2 ) );
					criteria.select( orderRoot ).distinct( true );
					TypedQuery<Order> query = entityManager.createQuery( criteria );
					query.getResultList();
				}
		);
	}
}
