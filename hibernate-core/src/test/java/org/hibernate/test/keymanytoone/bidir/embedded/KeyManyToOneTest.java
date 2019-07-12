/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.keymanytoone.bidir.embedded;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class KeyManyToOneTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "keymanytoone/bidir/embedded/Mapping.hbm.xml" };
	}

	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
	}

	@Test
	public void testCriteriaRestrictionOnKeyManyToOne() {
		inTransaction( s -> {
			s.createQuery( "from Order o where o.customer.name = 'Acme'" ).list();
//		Criteria criteria = s.createCriteria( Order.class );
//		criteria.createCriteria( "customer" ).add( Restrictions.eq( "name", "Acme" ) );
//		criteria.list();
			CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
			CriteriaQuery<Order> criteria = criteriaBuilder.createQuery( Order.class );
			Root<Order> root = criteria.from( Order.class );
			Join<Object, Object> customer = root.join( "customer", JoinType.INNER );
			criteria.where( criteriaBuilder.equal( customer.get( "name" ), "Acme" ) );
			s.createQuery( criteria ).list();
		} );
	}

	@Test
	public void testSaveCascadedToKeyManyToOne() {
		// test cascading a save to an association with a key-many-to-one which refers to a
		// just saved entity
		inTransaction( s -> {
			Customer cust = new Customer( "Acme, Inc." );
			Order order = new Order( cust, 1 );
			cust.getOrders().add( order );
			sessionFactory().getStatistics().clear();
			s.save( cust );
			s.flush();
			assertEquals( 2, sessionFactory().getStatistics().getEntityInsertCount() );
			s.delete( cust );
		} );
	}

	@Test
	public void testQueryingOnMany2One() {
		Customer cust = new Customer( "Acme, Inc." );
		Order order = new Order( cust, 1 );
		inTransaction( s -> {
			cust.getOrders().add( order );
			s.save( cust );
		} );

		inTransaction( s -> {
			List results = s.createQuery( "from Order o where o.customer.name = :name" )
					.setParameter( "name", cust.getName() )
					.list();
			assertEquals( 1, results.size() );
		} );

		inTransaction( s -> {
			s.delete( cust );
		} );
	}

	@Test
	public void testLoadingStrategies() {
		Session s = openSession();
		s.beginTransaction();
		Customer cust = new Customer( "Acme, Inc." );
		Order order = new Order( cust, 1 );
		cust.getOrders().add( order );
		s.save( cust );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();

		cust = ( Customer ) s.get( Customer.class, cust.getId() );
		assertEquals( 1, cust.getOrders().size() );
		s.clear();

		cust = ( Customer ) s.createQuery( "from Customer" ).uniqueResult();
		assertEquals( 1, cust.getOrders().size() );
		s.clear();

		cust = ( Customer ) s.createQuery( "from Customer c join fetch c.orders" ).uniqueResult();
		assertEquals( 1, cust.getOrders().size() );
		s.clear();

		s.delete( cust );
		s.getTransaction().commit();
		s.close();
	}
}
