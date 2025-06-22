/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.basic;

import org.hibernate.Hibernate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(
		annotatedClasses = {
				Contact.class, EmailAddress.class, User.class
		}
)
@SessionFactory
public class CollectionSizeTest {

	@Test
	public void prepareData(SessionFactoryScope scope) {
		Set<EmailAddress> emailAddresses = new HashSet<>();
		emailAddresses.add( new EmailAddress( "test1@test.com" ) );
		emailAddresses.add( new EmailAddress( "test2@test.com" ) );
		emailAddresses.add( new EmailAddress( "test3@test.com" ) );
		User user = new User();
		user.setName( "john" );
		Contact contact = new Contact();
		contact.setName( "John Doe" );
		contact.setEmailAddresses( emailAddresses );
		emailAddresses.forEach( address -> contact.getContactsByEmail().put(address, contact) );

		scope.inTransaction(
				session -> {
					user.setContact( contact );
					session.persist( user );
					session.persist( contact );
					session.flush();
				}
		);
		scope.inTransaction(
				session -> {
					Contact cont = session.get(Contact.class, contact.getId());

					Set<EmailAddress> addresses = cont.getEmailAddresses();
					assertEquals( Hibernate.size(addresses), 3 );
					assertTrue( Hibernate.contains( addresses, new EmailAddress( "test1@test.com" ) ) );
					assertFalse( Hibernate.contains( addresses, new EmailAddress( "test9@test.com" ) ) );
					assertFalse( Hibernate.isInitialized(addresses) );

					Map<EmailAddress, Contact> contactsByEmail = cont.getContactsByEmail();
					assertEquals( cont, Hibernate.get(contactsByEmail, new EmailAddress( "test1@test.com" ) ) );
					assertNull( Hibernate.get(contactsByEmail, new EmailAddress( "test9@test.com" ) ) );
					assertFalse( Hibernate.isInitialized(contactsByEmail) );
				}
		);
	}

}
