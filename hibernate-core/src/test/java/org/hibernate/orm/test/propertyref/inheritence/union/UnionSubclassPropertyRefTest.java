/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.propertyref.inheritence.union;


import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gavin King
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/propertyref/inheritence/union/Person.hbm.xml"
)
@SessionFactory
public class UnionSubclassPropertyRefTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Customer c = new Customer();
					c.setName( "Emmanuel" );
					c.setCustomerId( "C123-456" );
					c.setPersonId( "P123-456" );
					Account a = new Account();
					a.setCustomer( c );
					a.setPerson( c );
					a.setType( 'X' );
					session.persist( c );
					session.persist( a );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from Account" ).executeUpdate();
					session.createQuery( "delete from Person" ).executeUpdate();
					session.createQuery( "delete from Customer" ).executeUpdate();
				}
		);
	}

	@Test
	public void testOneToOnePropertyRef(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Account a = (Account) session.createQuery(
									"from Account acc join fetch acc.customer join fetch acc.person" )
							.uniqueResult();
					assertNotNull( a.getCustomer() );
					assertTrue( Hibernate.isInitialized( a.getCustomer() ) );
					assertNotNull( a.getPerson() );
					assertTrue( Hibernate.isInitialized( a.getPerson() ) );
					Customer c = (Customer) session.createQuery( "from Customer" ).uniqueResult();
					assertSame( a.getCustomer(), c );
					assertSame( a.getPerson(), c );
					session.remove( a );
					session.remove( a.getCustomer() );
					session.remove( a.getPerson() );
				}
		);
	}

}
