/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.criteria.many_to_many;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Janario Oliveira
 */
public class CriteriaManyToManyTest extends BaseNonConfigCoreFunctionalTestCase {

	private Seller[] persist(String prefix) {
		Session session = openSession();

		Transaction tx = session.beginTransaction();
		Seller seller1 = new Seller( prefix + "-seller1" );
		Seller seller2 = new Seller( prefix + "-seller2" );

		Customer customer1 = new Customer( prefix + "-customer1" );
		Customer customer2 = new Customer( prefix + "-customer2" );
		Customer customer3 = new Customer( prefix + "-customer3" );

		seller1.addCustomer( customer1 );
		seller1.addCustomer( customer2 );
		seller2.addCustomer( customer2 );
		seller2.addCustomer( customer3 );

		session.persist( customer1 );
		session.persist( customer2 );
		session.persist( customer3 );
		session.persist( seller1 );
		session.persist( seller2 );

		tx.commit();
		session.close();
		return new Seller[] {seller1, seller2};
	}

	@Test
	public void testJoinTable() {
		Seller[] sellers = persist( "join-table" );
		Seller seller1 = sellers[0];
		Seller seller2 = sellers[1];

		Session session = openSession();

		Criteria criteria = session.createCriteria( Seller.class, "s" );
		criteria.createCriteria(
				"s.soldTo",
				"c",
				JoinType.INNER_JOIN,
				Restrictions.eq( "name", "join-table-customer1" )
		);
		criteria.setResultTransformer( Criteria.DISTINCT_ROOT_ENTITY );
		@SuppressWarnings("unchecked")
		List<Seller> results = criteria.list();
		assertTrue( results.size() == 1 );
		assertTrue( results.contains( seller1 ) );
		assertFalse( results.contains( seller2 ) );


		criteria = session.createCriteria( Seller.class, "s" );
		criteria.createCriteria(
				"s.soldTo",
				"c",
				JoinType.INNER_JOIN,
				Restrictions.eq( "name", "join-table-customer2" )
		);
		criteria.setResultTransformer( Criteria.DISTINCT_ROOT_ENTITY );

		@SuppressWarnings("unchecked")
		List<Seller> results2 = criteria.list();
		assertTrue( results2.size() == 2 );
		assertTrue( results2.contains( seller1 ) );
		assertTrue( results2.contains( seller2 ) );

		session.close();
	}

	@Test
	public void testMappedBy() {
		Set<Customer> customersAll = new LinkedHashSet<Customer>();
		Seller[] sellers = persist( "mappedby" );
		customersAll.addAll( sellers[0].getSoldTo() );
		customersAll.addAll( sellers[1].getSoldTo() );

		Customer[] customers = customersAll.toArray( new Customer[customersAll.size()] );
		Customer customer1 = customers[0];
		Customer customer2 = customers[1];
		Customer customer3 = customers[2];

		Session session = openSession();

		Criteria criteria = session.createCriteria( Customer.class, "c" );
		criteria.createCriteria(
				"c.boughtFrom",
				"s",
				JoinType.INNER_JOIN,
				Restrictions.eq( "name", "mappedby-seller1" )
		);
		criteria.setResultTransformer( Criteria.DISTINCT_ROOT_ENTITY );
		@SuppressWarnings("unchecked")
		List<Customer> results = criteria.list();
		assertTrue( results.size() == 2 );
		assertTrue( results.contains( customer1 ) );
		assertTrue( results.contains( customer2 ) );
		assertFalse( results.contains( customer3 ) );


		criteria = session.createCriteria( Customer.class, "c" );
		criteria.createCriteria(
				"c.boughtFrom",
				"s",
				JoinType.INNER_JOIN,
				Restrictions.eq( "name", "mappedby-seller2" )
		);
		criteria.setResultTransformer( Criteria.DISTINCT_ROOT_ENTITY );
		@SuppressWarnings("unchecked")
		List<Customer> results2 = criteria.list();
		assertTrue( results2.size() == 2 );
		assertFalse( results2.contains( customer1 ) );
		assertTrue( results2.contains( customer2 ) );
		assertTrue( results2.contains( customer3 ) );

		session.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {Seller.class, Customer.class};
	}
}
