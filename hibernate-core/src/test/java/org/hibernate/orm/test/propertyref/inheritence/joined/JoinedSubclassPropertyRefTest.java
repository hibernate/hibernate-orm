/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.propertyref.inheritence.joined;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gavin King
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/propertyref/inheritence/joined/Person.hbm.xml"
)
@SessionFactory
public class JoinedSubclassPropertyRefTest {

	@Test
	public void testPropertyRefToJoinedSubclass(SessionFactoryScope scope) {
		Person person = new Person();
		scope.inTransaction(
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

		scope.inTransaction(
				session -> {
					Person p = session.get( Person.class, person.getId() );
					assertNotNull( p.getBankAccount() );
					assertTrue( Hibernate.isInitialized( p.getBankAccount() ) );
				}
		);

		scope.inTransaction(
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

		scope.inTransaction(
				session -> session.remove( person )
		);
	}

}
