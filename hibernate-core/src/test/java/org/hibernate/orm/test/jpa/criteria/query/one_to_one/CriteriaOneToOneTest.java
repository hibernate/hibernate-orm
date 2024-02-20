/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.criteria.query.one_to_one;

import java.util.List;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import static org.junit.Assert.assertEquals;


public class CriteriaOneToOneTest extends BaseEntityManagerFunctionalTestCase {

	@Before
	public void persist() {
		TransactionUtil.doInJPA( this::entityManagerFactory, (EntityManager session) -> {

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
				});
	}

	@Test
	@JiraKey("HHH-16512")
	public void testCustomerWithoutUser() {


		TransactionUtil.doInJPA( this::entityManagerFactory, (EntityManager session) -> {

			final CriteriaBuilder cb = session.getCriteriaBuilder();

			//isNull predicate with direction Customer -> User
			final CriteriaQuery<Customer> customerUserIsNullQuery = cb.createQuery( Customer.class );
			final Root<Customer> customerUserIsNullRoot = customerUserIsNullQuery.from( Customer.class );
			customerUserIsNullQuery.where( cb.isNull( customerUserIsNullRoot.get( "user" ) ) );
			List<Customer> customerUserIsNullResults = session.createQuery( customerUserIsNullQuery )
					.getResultList();
			assertEquals( 1, customerUserIsNullResults.size() );

			//isNull predicate on ID of Customer via Join with direction User -> Customer
			final CriteriaQuery<User> userJoinCustomerQuery = cb.createQuery( User.class );
			Root<User> userJoinCustomerQueryRoot = userJoinCustomerQuery.from( User.class );
			final Join<User, Customer> customerJoin = userJoinCustomerQueryRoot.join( "customer", JoinType.LEFT );
			final Predicate isCustomerIDNullPredicate = cb.isNull( customerJoin.get( "id" ) );
			userJoinCustomerQuery.where( isCustomerIDNullPredicate );
			List<User> userJoinCustomerResults = session.createQuery( userJoinCustomerQuery ).getResultList();
			assertEquals( 1, userJoinCustomerResults.size() );

			//isNull predicate on Customer via Join with direction User -> Customer (should do the same as the one above
			final CriteriaQuery<User> userCustomerIsNullQuery = cb.createQuery( User.class );
			final Root<User> userCustomerIsNullRoot = userCustomerIsNullQuery.from( User.class );
			userCustomerIsNullQuery.where( cb.isNull( userCustomerIsNullRoot.get( "customer" ) ) );
			List<User> userCustomerIsNullResults = session.createQuery( userCustomerIsNullQuery )
					.getResultList();
			assertEquals( 1, userCustomerIsNullResults.size() );
		});
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{ User.class, Customer.class };
	}

	/**
	 * @author Janario Oliveira
	 */
	@Entity
	public static class Customer {
		@Id
		@GeneratedValue
		private Integer id;
		private String name;

		@OneToOne
		@JoinColumn(name = "user_id")
		private User user;

		protected Customer() {
		}

		public Customer(String name) {
			this.name = name;
		}

		public User getUser() {
			return user;
		}

		public void setUser(User user) {
			this.user = user;
		}

		@Override
		public boolean equals(Object o) {
			if ( !( o instanceof Customer ) ) {
				return false;
			}

			Customer seller = (Customer) o;
			return name.equals( seller.name );
		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}
	}

	/**
	 * @author Janario Oliveira
	 */
	@Entity
	public static class User {
		@Id
		@GeneratedValue
		private Integer id;
		private String name;

		@OneToOne(mappedBy = "user")
		private Customer customer;

		protected User() {
		}

		public User(String name) {
			this.name = name;
		}

		public Customer getCustomer() {
			return customer;
		}

		public void setCustomer(Customer customer) {
			this.customer = customer;
		}

		@Override
		public boolean equals(Object o) {
			if ( !( o instanceof User ) ) {
				return false;
			}

			User customer = (User) o;
			return name.equals( customer.name );

		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}
	}
}
