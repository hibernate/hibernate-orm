/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.criteria;

import org.hibernate.jpa.test.metamodel.Address;
import org.hibernate.jpa.test.metamodel.Alias;
import org.hibernate.jpa.test.metamodel.Country;
import org.hibernate.jpa.test.metamodel.CreditCard;
import org.hibernate.jpa.test.metamodel.Customer;
import org.hibernate.jpa.test.metamodel.Entity1;
import org.hibernate.jpa.test.metamodel.Entity2;
import org.hibernate.jpa.test.metamodel.Entity3;
import org.hibernate.jpa.test.metamodel.Info;
import org.hibernate.jpa.test.metamodel.LineItem;
import org.hibernate.jpa.test.metamodel.Order;
import org.hibernate.jpa.test.metamodel.Phone;
import org.hibernate.jpa.test.metamodel.Product;
import org.hibernate.jpa.test.metamodel.ShelfLife;
import org.hibernate.jpa.test.metamodel.Spouse;
import org.hibernate.jpa.test.metamodel.Thing;
import org.hibernate.jpa.test.metamodel.ThingWithQuantity;
import org.hibernate.jpa.test.metamodel.VersionedEntity;

import org.hibernate.testing.orm.junit.Jpa;

/**
 * @author Jan Schatteman
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
public abstract class AbstractCriteriaTest {
}
