/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.paths;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import org.hibernate.orm.test.jpa.metamodel.AbstractMetamodelSpecificTest;
import org.hibernate.orm.test.jpa.metamodel.Address;
import org.hibernate.orm.test.jpa.metamodel.Order;
import org.hibernate.orm.test.jpa.metamodel.Thing;
import org.hibernate.orm.test.jpa.metamodel.ThingWithQuantity;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ExpectedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Michael Rudolf
 * @author James Gilbertson
 */
public class AbstractPathImplTest extends AbstractMetamodelSpecificTest {
	@BeforeEach
	public void prepareTestData() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		Thing thing = new Thing();
		thing.setId( "thing1" );
		thing.setName( "A Thing" );
		em.persist( thing );

		thing = new Thing();
		thing.setId( "thing2" );
		thing.setName( "Another Thing" );
		em.persist( thing );

		ThingWithQuantity thingWithQuantity = new ThingWithQuantity();
		thingWithQuantity.setId( "thingWithQuantity3" );
		thingWithQuantity.setName( "3 Things" );
		thingWithQuantity.setQuantity( 3 );
		em.persist( thingWithQuantity );

		em.getTransaction().commit();
		em.close();
	}

	@AfterEach
	public void cleanupTestData() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.remove( em.find( Thing.class, "thing1" ) );
		em.remove( em.find( Thing.class, "thing2" ) );
		em.remove( em.find( ThingWithQuantity.class, "thingWithQuantity3" ) );
		em.getTransaction().commit();
		em.close();
	}

	@ExpectedException(value = IllegalArgumentException.class)
	@Test
	public void testGetNonExistingAttributeViaName() {
		EntityManager em = getOrCreateEntityManager();
		try {
			CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
			CriteriaQuery<Order> criteria = criteriaBuilder.createQuery( Order.class );
			Root<Order> orderRoot = criteria.from( Order.class );
			orderRoot.get( "nonExistingAttribute" );
		}
		finally {
			em.close();
		}
	}

	@Test
	public void testIllegalDereference() {
		EntityManager em = getOrCreateEntityManager();
		try {
			CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
			CriteriaQuery<Order> criteria = criteriaBuilder.createQuery( Order.class );
			Root<Order> orderRoot = criteria.from( Order.class );
			Path simplePath = orderRoot.get( "totalPrice" );
			// this should cause an ISE...
			try {
				simplePath.get( "yabbadabbado" );
				fail( "Attempt to dereference basic path should throw IllegalStateException" );
			}
			catch (IllegalStateException expected) {
			}
		}
		finally {
			em.close();
		}
	}

	@Test
	public void testTypeExpression() {
		EntityManager em = getOrCreateEntityManager();
		try {
			CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
			CriteriaQuery<Thing> criteria = criteriaBuilder.createQuery( Thing.class );
			Root<Thing> thingRoot = criteria.from( Thing.class );

			criteria.select( thingRoot );
			assertEquals( 3, em.createQuery( criteria ).getResultList().size() );

			criteria.where( criteriaBuilder.equal( thingRoot.type(), criteriaBuilder.literal( Thing.class ) ) );
			assertEquals( 2, em.createQuery( criteria ).getResultList().size() );
		}
		finally {
			em.close();
		}
	}

	@Test
	@JiraKey( value = "HHH-15433")
	public void testTypeExpressionWithoutInheritance() {
		EntityManager em = getOrCreateEntityManager();
		try {
			CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
			CriteriaQuery<Address> criteria = criteriaBuilder.createQuery( Address.class );
			Root<Address> addressRoot = criteria.from( Address.class );

			criteria.select( addressRoot );
			criteria.where( criteriaBuilder.equal( addressRoot.type(), criteriaBuilder.literal( Address.class ) ) );
			em.createQuery( criteria ).getResultList();
		}
		finally {
			em.close();
		}
	}
}
