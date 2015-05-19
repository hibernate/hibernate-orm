/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria.paths;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.jpa.test.metamodel.AbstractMetamodelSpecificTest;
import org.hibernate.jpa.test.metamodel.Order;
import org.hibernate.jpa.test.metamodel.Order_;
import org.hibernate.jpa.test.metamodel.Thing;
import org.hibernate.jpa.test.metamodel.ThingWithQuantity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Michael Rudolf
 * @author James Gilbertson
 */
public class AbstractPathImplTest extends AbstractMetamodelSpecificTest {
    @Before
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

    @After
    public void cleanupTestData() {
        EntityManager em = getOrCreateEntityManager();
        em.getTransaction().begin();
        em.remove( em.find( Thing.class, "thing1" ) );
        em.remove( em.find( Thing.class, "thing2" ) );
        em.remove( em.find( ThingWithQuantity.class, "thingWithQuantity3" ) );
        em.getTransaction().commit();
        em.close();
    }

	@Test(expected = IllegalArgumentException.class)
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
			assertEquals( em.createQuery( criteria ).getResultList().size(), 3);

			criteria.where( criteriaBuilder.equal( thingRoot.type(), criteriaBuilder.literal( Thing.class ) ) );
			assertEquals( em.createQuery( criteria ).getResultList().size(), 2 );
		}
		finally {
			em.close();
		}
	}
}
