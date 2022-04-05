/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.userguide.tooling.modelgen;

import java.math.BigDecimal;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.userguide.model.tooling.Customer;
import org.hibernate.userguide.model.tooling.Item;
import org.hibernate.userguide.model.tooling.Order;
import org.hibernate.userguide.model.tooling.Order_;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = { Order.class, Item.class, Customer.class } )
@SessionFactory
public class ModelGenTests {
	@Test
	public void testIt(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			//tag::tooling-modelgen-usage[]
			final CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			final CriteriaQuery<Customer> criteria = criteriaBuilder.createQuery( Customer.class );

			final Root<Order> root = criteria.from( Order.class );

			criteria.select( root.get( Order_.customer ) );
			criteria.where( criteriaBuilder.greaterThan( root.get( Order_.totalCost ), new BigDecimal( 100 ) ) );
			//end::tooling-modelgen-usage[]
		} );
	}
}
