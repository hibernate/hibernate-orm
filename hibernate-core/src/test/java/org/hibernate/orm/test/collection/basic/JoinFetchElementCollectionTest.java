/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.collection.basic;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.junit.FailureExpected;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@FailureExpected("SQM changes required - PluralAttributeReference#addNavigableReference is not impl so when resolution the fetch, trying to build expression yields NPE due to not being able to lookup column references.")
public class JoinFetchElementCollectionTest extends SessionFactoryBasedFunctionalTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Contact.class, EmailAddress.class, User.class };
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8206")
	@FailureExpected(jiraKey = "HHH-8206", value = "This is not explicitly supported, however should arguably throw an exception")
	public void testJoinFetchesByPath() {
		Set<EmailAddress> emailAddresses = new HashSet<EmailAddress>();
		emailAddresses.add( new EmailAddress( "test1@test.com" ) );
		emailAddresses.add( new EmailAddress( "test2@test.com" ) );
		emailAddresses.add( new EmailAddress( "test3@test.com" ) );

		// Session 1: Insert a user with email addresses but no emailAddresses2
		inTransaction(
				session -> {
					User user = new User();
					user.setName( "john" );
					Contact contact = new Contact();
					contact.setName( "John Doe" );
					contact.setEmailAddresses( emailAddresses );
					contact = (Contact) session.merge( contact );
					user.setContact( contact );
					session.merge( user );
				}
		);

		// Session 2: Retrieve the user object and check if the sets have the expected values
		User user = inTransaction(
				session -> {
					final String qry = "SELECT user "
							+ "FROM User user "
							+ "LEFT OUTER JOIN FETCH user.contact "
							+ "LEFT OUTER JOIN FETCH user.contact.emailAddresses2 "
							+ "LEFT OUTER JOIN FETCH user.contact.emailAddresses";
					return (User) session.createQuery( qry ).uniqueResult();

				}
		);
		assertEquals( emailAddresses, user.getContact().getEmailAddresses() );
		assertTrue( user.getContact().getEmailAddresses2().isEmpty() );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-5465")
	public void testJoinFetchElementCollection() {
		Set<EmailAddress> emailAddresses = new HashSet<EmailAddress>();
		emailAddresses.add( new EmailAddress( "test1@test.com" ) );
		emailAddresses.add( new EmailAddress( "test2@test.com" ) );
		emailAddresses.add( new EmailAddress( "test3@test.com" ) );

		// Session 1: Insert a user with email addresses but no emailAddresses2
		inTransaction(
				session -> {
					User user = new User();
					user.setName( "john" );
					Contact contact = new Contact();
					contact.setName( "John Doe" );
					contact.setEmailAddresses( emailAddresses );
					contact = (Contact) session.merge( contact );
					user.setContact( contact );
					session.merge( user );
				}
		);

		// Session 2: Retrieve the user object and check if the sets have the expected values
		User user = inTransaction(
				session -> {
					final String qry = "SELECT user "
							+ "FROM User user "
							+ "LEFT OUTER JOIN FETCH user.contact c "
							+ "LEFT OUTER JOIN FETCH c.emailAddresses2 "
							+ "LEFT OUTER JOIN FETCH c.emailAddresses";
					return (User) session.createQuery( qry ).uniqueResult();
				}
		);

		assertEquals( emailAddresses, user.getContact().getEmailAddresses() );
		assertTrue( user.getContact().getEmailAddresses2().isEmpty() );
	}

}
