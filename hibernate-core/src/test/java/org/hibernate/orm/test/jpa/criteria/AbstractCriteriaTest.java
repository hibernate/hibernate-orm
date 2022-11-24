/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.criteria;

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
import org.hibernate.orm.test.jpa.metamodel.Order;
import org.hibernate.orm.test.jpa.metamodel.Phone;
import org.hibernate.orm.test.jpa.metamodel.Product;
import org.hibernate.orm.test.jpa.metamodel.ShelfLife;
import org.hibernate.orm.test.jpa.metamodel.Spouse;
import org.hibernate.orm.test.jpa.metamodel.Thing;
import org.hibernate.orm.test.jpa.metamodel.ThingWithQuantity;
import org.hibernate.orm.test.jpa.metamodel.VersionedEntity;

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
}
)
public abstract class AbstractCriteriaTest {
}
