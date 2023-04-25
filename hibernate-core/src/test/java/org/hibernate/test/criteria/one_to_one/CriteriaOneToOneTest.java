/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.criteria.one_to_one;

import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Janario Oliveira
 */
public class CriteriaOneToOneTest extends BaseNonConfigCoreFunctionalTestCase {

	private Object[] persist() {
		Session sessionDelete = openSession();
		Transaction txDelete = sessionDelete.beginTransaction();

		String hql2 = "delete from Customer";
		Query query2 = sessionDelete.createQuery( hql2 );
		query2.executeUpdate();

		String hql1 = "delete from User";
		Query query1 = sessionDelete.createQuery( hql1 );
		query1.executeUpdate();

		txDelete.commit();
		sessionDelete.close();

		Session session = openSession();
		Transaction tx = session.beginTransaction();
		User user1 = new User( "user1" );
		User user2 = new User( "user2" );

		Customer customer1 = new Customer( "customer1" );
		Customer customer2 = new Customer( "customer2" );

		user2.setCustomer( customer2 );
		customer2.setUser( user2 );

		session.persist( customer1 );
		session.persist( customer2 );
		session.persist( user1 );
		session.persist( user2 );

		tx.commit();
		session.close();
		return new Object[] {user1, customer1, user2, customer2};
	}

	@Test
	@TestForIssue(jiraKey = "HHH-16512")
	public void testCustomerWithoutUser() {
		Object[] objects = persist();
		Customer customer1 = (Customer) objects[1];

		Session session = openSession();

		final CriteriaBuilder cb = session.getCriteriaBuilder();

		//isNull predicate with direction Customer -> User
		final CriteriaQuery<Customer> customerUserIsNullQuery = cb.createQuery( Customer.class );
		final Root<Customer> customerUserIsNullRoot = customerUserIsNullQuery.from( Customer.class );
		customerUserIsNullQuery.where( cb.isNull( customerUserIsNullRoot.get( Customer_.user ) ) );
		List<Customer> customerUserIsNullResults = session.createQuery( customerUserIsNullQuery ).getResultList();
		assertEquals( 1, customerUserIsNullResults.size() );
		assertTrue( customerUserIsNullResults.contains( customer1 ) );

		session.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-16512")
	public void testUserWithoutCustomer() {
		Object[] objects = persist();
		User user1 = (User) objects[0];

		Session session = openSession();

		final CriteriaBuilder cb = session.getCriteriaBuilder();

		//isNull predicate on ID of Customer via Join with direction User -> Customer
		final CriteriaQuery<User> userJoinCustomerQuery = cb.createQuery( User.class );
		Root<User> userJoinCustomerQueryRoot = userJoinCustomerQuery.from( User.class );
		final Join<User, Customer> customerJoin = userJoinCustomerQueryRoot.join( User_.customer, JoinType.LEFT );
		final Predicate isCustomerIDNullPredicate = cb.isNull( customerJoin.get( Customer_.id ) );
		userJoinCustomerQuery.where( isCustomerIDNullPredicate );
		List<User> userJoinCustomerResults = session.createQuery( userJoinCustomerQuery ).getResultList();
		assertEquals( 1, userJoinCustomerResults.size() );
		assertTrue( userJoinCustomerResults.contains( user1 ) );

		//isNull predicate on Customer via Join with direction User -> Customer (should do the same as the one above
		final CriteriaQuery<User> userCustomerIsNullQuery = cb.createQuery( User.class );
		final Root<User> userCustomerIsNullRoot = userCustomerIsNullQuery.from( User.class );
		userCustomerIsNullQuery.where( cb.isNull( userCustomerIsNullRoot.get( User_.customer ) ) );
		List<User> userCustomerIsNullResults = session.createQuery( userCustomerIsNullQuery ).getResultList();
		assertEquals( 1, userCustomerIsNullResults.size() );
		assertTrue( userCustomerIsNullResults.contains( user1 ) );

		session.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { User.class, Customer.class};
	}
}
