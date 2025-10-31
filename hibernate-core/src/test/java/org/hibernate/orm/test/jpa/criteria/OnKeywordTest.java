/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria;

import jakarta.persistence.criteria.CollectionJoin;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.orm.test.jpa.metamodel.Address;
import org.hibernate.orm.test.jpa.metamodel.Alias;
import org.hibernate.orm.test.jpa.metamodel.Country;
import org.hibernate.orm.test.jpa.metamodel.CreditCard;
import org.hibernate.orm.test.jpa.metamodel.Customer;
import org.hibernate.orm.test.jpa.metamodel.Entity1;
import org.hibernate.orm.test.jpa.metamodel.Entity2;
import org.hibernate.orm.test.jpa.metamodel.Entity3;
import org.hibernate.orm.test.jpa.metamodel.Info;
import org.hibernate.orm.test.jpa.metamodel.LineItem;
import org.hibernate.orm.test.jpa.metamodel.LineItem_;
import org.hibernate.orm.test.jpa.metamodel.Order;
import org.hibernate.orm.test.jpa.metamodel.Order_;

import org.hibernate.orm.test.jpa.metamodel.Phone;
import org.hibernate.orm.test.jpa.metamodel.Product;
import org.hibernate.orm.test.jpa.metamodel.ShelfLife;
import org.hibernate.orm.test.jpa.metamodel.Spouse;
import org.hibernate.orm.test.jpa.metamodel.Thing;
import org.hibernate.orm.test.jpa.metamodel.ThingWithQuantity;
import org.hibernate.orm.test.jpa.metamodel.VersionedEntity;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

/**
 * Similar to {@link org.hibernate.orm.test.query.hql.OnKeywordTest}, but here testing from JPA criteria queries.
 *
 * @author Steve Ebersole
 */
@Jpa(annotatedClasses = {
		Address.class,
		Alias.class,
		Country.class,
		CreditCard.class,
		Customer.class,
		Entity1.class,
		Entity2.class,
		Entity3.class,
		Info.class,
		LineItem.class,
		Order.class,
		Phone.class,
		Product.class,
		ShelfLife.class,
		Spouse.class,
		Thing.class,
		ThingWithQuantity.class,
		VersionedEntity.class
})
public class OnKeywordTest {
	@Test
	public void basicTest(EntityManagerFactoryScope scope) {

		scope.inEntityManager(
				entityManager -> {
					CriteriaQuery<Order> criteria = entityManager.getCriteriaBuilder().createQuery( Order.class );
					Root<Order> root = criteria.from( Order.class );
					criteria.select( root );
					CollectionJoin<Order, LineItem> lineItemsJoin = root.join( Order_.lineItems );
					lineItemsJoin.on(
							entityManager.getCriteriaBuilder().gt(
									lineItemsJoin.get( LineItem_.quantity ),
									entityManager.getCriteriaBuilder().literal( 20 )
							)
					);
					entityManager.createQuery( criteria ).getResultList();
				}
		);
	}
}
