/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.paths;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
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
import org.hibernate.orm.test.jpa.metamodel.Order;
import org.hibernate.orm.test.jpa.metamodel.Phone;
import org.hibernate.orm.test.jpa.metamodel.Product;
import org.hibernate.orm.test.jpa.metamodel.ShelfLife;
import org.hibernate.orm.test.jpa.metamodel.Spouse;
import org.hibernate.orm.test.jpa.metamodel.Thing;
import org.hibernate.orm.test.jpa.metamodel.ThingWithQuantity;
import org.hibernate.orm.test.jpa.metamodel.VersionedEntity;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.ExpectedException;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Michael Rudolf
 * @author James Gilbertson
 */
@Jpa(annotatedClasses = {
		Address.class, Alias.class, Country.class, CreditCard.class, Customer.class,
		Entity1.class, Entity2.class, Entity3.class,
		Info.class, LineItem.class, Order.class, Phone.class, Product.class,
		ShelfLife.class, Spouse.class, Thing.class, ThingWithQuantity.class,
		VersionedEntity.class
})
public class AbstractPathImplTest {
	@BeforeEach
	public void prepareTestData(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {

			Thing thing = new Thing();
			thing.setId( "thing1" );
			thing.setName( "A Thing" );
			entityManager.persist( thing );

			thing = new Thing();
			thing.setId( "thing2" );
			thing.setName( "Another Thing" );
			entityManager.persist( thing );

			ThingWithQuantity thingWithQuantity = new ThingWithQuantity();
			thingWithQuantity.setId( "thingWithQuantity3" );
			thingWithQuantity.setName( "3 Things" );
			thingWithQuantity.setQuantity( 3 );
			entityManager.persist( thingWithQuantity );

		} );
	}

	@AfterEach
	public void cleanupTestData(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@ExpectedException(value = IllegalArgumentException.class)
	@Test
	public void testGetNonExistingAttributeViaName(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
			CriteriaQuery<Order> criteria = criteriaBuilder.createQuery( Order.class );
			Root<Order> orderRoot = criteria.from( Order.class );
			orderRoot.get( "nonExistingAttribute" );
		} );
	}

	@Test
	public void testIllegalDereference(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
			CriteriaQuery<Order> criteria = criteriaBuilder.createQuery( Order.class );
			Root<Order> orderRoot = criteria.from( Order.class );
			Path<?> simplePath = orderRoot.get( "totalPrice" );
			// this should cause an ISE...
			assertThrows(
					IllegalStateException.class,
					() -> simplePath.get( "yabbadabbadoo" ),
					"Attempting to dereference a basic path should throw IllegalStateException"
			);
		} );
	}

	@Test
	public void testTypeExpression(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
			CriteriaQuery<Thing> criteria = criteriaBuilder.createQuery( Thing.class );
			Root<Thing> thingRoot = criteria.from( Thing.class );

			criteria.select( thingRoot );
			assertEquals( 3, entityManager.createQuery( criteria ).getResultList().size() );

			criteria.where( criteriaBuilder.equal( thingRoot.type(), criteriaBuilder.literal( Thing.class ) ) );
			assertEquals( 2, entityManager.createQuery( criteria ).getResultList().size() );
		} );
	}

	@Test
	@JiraKey( value = "HHH-15433")
	public void testTypeExpressionWithoutInheritance(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
			CriteriaQuery<Address> criteria = criteriaBuilder.createQuery( Address.class );
			Root<Address> addressRoot = criteria.from( Address.class );

			criteria.select( addressRoot );
			criteria.where( criteriaBuilder.equal( addressRoot.type(), criteriaBuilder.literal( Address.class ) ) );
			entityManager.createQuery( criteria ).getResultList();
		} );
	}
}
