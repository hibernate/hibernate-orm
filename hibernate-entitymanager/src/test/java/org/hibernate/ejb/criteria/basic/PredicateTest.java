/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.ejb.criteria.basic;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.hibernate.ejb.metamodel.AbstractMetamodelSpecificTest;
import org.hibernate.ejb.metamodel.Order;

/**
 * Test the various predicates.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public class PredicateTest extends AbstractMetamodelSpecificTest {

	private EntityManager em;
	private CriteriaBuilder builder;

	public void setUp() throws Exception {
		super.setUp();
		builder = factory.getCriteriaBuilder();
		em = getOrCreateEntityManager();
		createTestOrders();
		em.getTransaction().begin();
	}

	public void tearDown() throws Exception {
		em.getTransaction().commit();
		em.close();
		super.tearDown();
	}

	public void testEmptyConjunction() {
		// yes this is a retarded case, but explicitly allowed in the JPA spec
		CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
		Root<Order> orderRoot = orderCriteria.from( Order.class );
		orderCriteria.select( orderRoot );
		orderCriteria.where( builder.isTrue( builder.conjunction() ) );
		em.createQuery( orderCriteria ).getResultList();

		List<Order> orders = em.createQuery( orderCriteria ).getResultList();
		assertTrue( orders.size() == 3 );
	}

	public void testEmptyDisjunction() {
		// yes this is a retarded case, but explicitly allowed in the JPA spec
		CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
		Root<Order> orderRoot = orderCriteria.from( Order.class );
		orderCriteria.select( orderRoot );
		orderCriteria.where( builder.isFalse( builder.disjunction() ) );
		em.createQuery( orderCriteria ).getResultList();

		List<Order> orders = em.createQuery( orderCriteria ).getResultList();
		assertTrue( orders.size() == 3 );
	}

	/**
	 * Check simple not.
	 */
	public void testSimpleNot() {
		CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
		Root<Order> orderRoot = orderCriteria.from( Order.class );

		orderCriteria.select( orderRoot );
		orderCriteria.where( builder.not( builder.equal( orderRoot.get( "id" ), "order-1" ) ) );

		List<Order> orders = em.createQuery( orderCriteria ).getResultList();
		assertTrue( orders.size() == 2 );
	}

	/**
	 * Check complicated not.
	 */
	public void testComplicatedNotOr() {
		CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
		Root<Order> orderRoot = orderCriteria.from( Order.class );

		orderCriteria.select( orderRoot );
		Predicate p1 = builder.equal( orderRoot.get( "id" ), "order-1" );
		Predicate p2 = builder.equal( orderRoot.get( "id" ), "order-2" );
		orderCriteria.where( builder.not( builder.or( p1, p2 ) ) );

		List<Order> orders = em.createQuery( orderCriteria ).getResultList();
		assertTrue( orders.size() == 1 );
		Order order = orders.get( 0 );
		assertEquals( "order-3", order.getId() );
	}

	/**
	 * Check complicated not.
	 */
	public void testNotMultipleOr() {
		CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
		Root<Order> orderRoot = orderCriteria.from( Order.class );

		orderCriteria.select( orderRoot );
		Predicate p1 = builder.equal( orderRoot.get( "id" ), "order-1" );
		Predicate p2 = builder.equal( orderRoot.get( "id" ), "order-2" );
		Predicate p3 = builder.equal( orderRoot.get( "id" ), "order-3" );
		orderCriteria.where( builder.not( builder.or( p1, p2, p3 ) ) );

		List<Order> orders = em.createQuery( orderCriteria ).getResultList();
		assertTrue( orders.size() == 0 );
	}

	/**
	 * Check complicated not.
	 */
	public void testComplicatedNotAnd() {
		CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
		Root<Order> orderRoot = orderCriteria.from( Order.class );

		orderCriteria.select( orderRoot );
		Predicate p1 = builder.equal( orderRoot.get( "id" ), "order-1" );
		Predicate p2 = builder.equal( orderRoot.get( "id" ), "order-2" );
		orderCriteria.where( builder.not( builder.and( p1, p2 ) ) );

		List<Order> orders = em.createQuery( orderCriteria ).getResultList();
		assertTrue( orders.size() == 3 );
	}

	private void createTestOrders() {
		em.getTransaction().begin();
		em.persist( new Order( "order-1", 1.0d ) );
		em.persist( new Order( "order-2", 10.0d ) );
		em.persist( new Order( "order-3", new char[]{'r','u'} ) );
		em.getTransaction().commit();
	}

	/**
	 * Check predicate for field which has simple char array type (char[]).
	 */
	public void testCharArray() {
		CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
		Root<Order> orderRoot = orderCriteria.from( Order.class );
		
		orderCriteria.select( orderRoot );
		Predicate p = builder.equal( orderRoot.get( "domen" ), new char[]{'r','u'} );
		orderCriteria.where( p );

		List<Order> orders = em.createQuery( orderCriteria ).getResultList();
		assertTrue( orders.size() == 1 );
	}

	/**
	 * Check predicate for field which has simple char array type (byte[]).
	 */
	public void testByteArray() {
		CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
		Root<Order> orderRoot = orderCriteria.from( Order.class );
		
		orderCriteria.select( orderRoot );
		Predicate p = builder.equal( orderRoot.get( "number" ), new byte[]{'1','2'} );
		orderCriteria.where( p );

		List<Order> orders = em.createQuery( orderCriteria ).getResultList();
		assertTrue( orders.size() == 0 );
	}


}
