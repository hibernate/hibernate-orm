/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.propertyref.inheritence.joined;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;

import org.hibernate.Hibernate;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 */
public class JoinedSubclassPropertyRefTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] { "propertyref/inheritence/joined/Person.hbm.xml" };
	}

	@Test
	public void testPropertyRefToJoinedSubclass() {
		Person person = new Person();
		inTransaction(
				session -> {
					person.setName( "Gavin King" );
					BankAccount acc = new BankAccount();
					acc.setBsb( "0634" );
					acc.setType( 'B' );
					acc.setAccountNumber( "xxx-123-abc" );
					person.setBankAccount( acc );
					session.persist( person );
				}
		);

		inTransaction(
				session -> {
					Person p = session.get( Person.class, person.getId() );
					assertNotNull( p.getBankAccount() );
					assertTrue( Hibernate.isInitialized( p.getBankAccount() ) );
				}
		);

		inTransaction(
				session -> {
					CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
					CriteriaQuery<Person> criteria = criteriaBuilder.createQuery( Person.class );
					criteria.from( Person.class ).fetch( "bankAccount", JoinType.LEFT );

					Person p = session.createQuery( criteria ).uniqueResult();
//					Person p = (Person) session.createCriteria(Person.class)
//							.setFetchMode("bankAccount", FetchMode.JOIN)
//							.uniqueResult();
					assertNotNull( p.getBankAccount() );
					assertTrue( Hibernate.isInitialized( p.getBankAccount() ) );
				}
		);

		inTransaction(
				session -> session.delete( person )
		);
	}

}
