/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.basic;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(
		annotatedClasses = {
				Contact.class, EmailAddress.class, User.class
		}
)
@SessionFactory
public class JoinFetchElementCollectionTest {

	private Set<EmailAddress> emailAddresses;

	@BeforeAll
	public void prepareData(SessionFactoryScope scope) {
		Set<EmailAddress> emailAddresses = new HashSet<>();
		emailAddresses.add( new EmailAddress( "test1@test.com" ) );
		emailAddresses.add( new EmailAddress( "test2@test.com" ) );
		emailAddresses.add( new EmailAddress( "test3@test.com" ) );

		// Session 1: Insert a user with email addresses but no emailAddresses2
		scope.inTransaction(
				session -> {
					User user = new User();
					user.setName( "john" );
					Contact contact = new Contact();
					contact.setName( "John Doe" );
					contact.setEmailAddresses( emailAddresses );
					contact = (Contact) session.merge( contact );
					user.setContact( contact );
					user = (User) session.merge( user );
				}
		);
		this.emailAddresses = emailAddresses;
	}

	@Test
	@JiraKey(value = "HHH-8206")
	public void testJoinFetchesByPath(SessionFactoryScope scope) {
		// Session 2: Retrieve the user object and check if the sets have the expected values
		scope.inTransaction(
				session -> {
					final String qry = "SELECT user "
							+ "FROM User user "
							+ "LEFT OUTER JOIN FETCH user.contact "
							+ "LEFT OUTER JOIN FETCH user.contact.emailAddresses2 "
							+ "LEFT OUTER JOIN FETCH user.contact.emailAddresses";
					User user = (User) session.createQuery( qry ).uniqueResult();
					assertEquals( emailAddresses, user.getContact().getEmailAddresses() );
					assertTrue( user.getContact().getEmailAddresses2().isEmpty() );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-5465")
	public void testJoinFetchElementCollection(SessionFactoryScope scope) {
		// Session 2: Retrieve the user object and check if the sets have the expected values
		scope.inTransaction(
				session -> {
					final String qry = "SELECT user "
							+ "FROM User user "
							+ "LEFT OUTER JOIN FETCH user.contact c "
							+ "LEFT OUTER JOIN FETCH c.emailAddresses2 "
							+ "LEFT OUTER JOIN FETCH c.emailAddresses";
					User user = (User) session.createQuery( qry ).uniqueResult();
					assertEquals( emailAddresses, user.getContact().getEmailAddresses() );
					assertTrue( user.getContact().getEmailAddresses2().isEmpty() );
				}
		);
	}

}
