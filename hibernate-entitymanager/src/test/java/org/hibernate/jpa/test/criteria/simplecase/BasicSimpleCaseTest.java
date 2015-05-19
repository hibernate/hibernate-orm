/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria.simplecase;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static javax.persistence.criteria.CriteriaBuilder.SimpleCase;

/**
 * Mote that these are simply performing syntax checking (can the criteria query
 * be properly compiled and executed)
 *
 * @author Steve Ebersole
 */
public class BasicSimpleCaseTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Customer.class};
	}

	@Test
	public void testCaseInOrderBy() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		CriteriaBuilder builder = em.getCriteriaBuilder();

		CriteriaQuery<Customer> query = builder.createQuery( Customer.class );
		Root<Customer> root = query.from( Customer.class );
		query.select( root );

		Path<String> emailPath = root.get( "email" );
		SimpleCase<String, Integer> orderCase = builder.selectCase( emailPath );
		orderCase = orderCase.when( "test@test.com", 1 );
		orderCase = orderCase.when( "test2@test.com", 2 );

		query.orderBy( builder.asc( orderCase.otherwise( 0 ) ) );

		em.createQuery( query );

	}

	@Test
	public void testCaseInOrderBy2() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		CriteriaBuilder builder = em.getCriteriaBuilder();

		CriteriaQuery<Customer> query = builder.createQuery( Customer.class );
		Root<Customer> root = query.from( Customer.class );
		query.select( root );

		Path<String> emailPath = root.get( "email" );
		SimpleCase<String, String> orderCase = builder.selectCase( emailPath );
		orderCase = orderCase.when( "test@test.com", "a" );
		orderCase = orderCase.when( "test2@test.com", "b" );

		query.orderBy( builder.asc( orderCase.otherwise( "c" ) ) );

		em.createQuery( query );

	}

	@Entity(name = "Customer")
	@Table(name = "customer")
	public static class Customer {
		private Integer id;
		private String email;

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}
	}
}
