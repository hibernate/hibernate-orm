/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.collection.basic;

import org.hibernate.Hibernate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
		List<EmailAddress> emailAddresses3 = new ArrayList<>();
		emailAddresses3.add( new EmailAddress( "test4@test.com" ) );
		emailAddresses3.add( new EmailAddress( "test5@test.com" ) );
		User user = new User();
		user.setName( "john" );
		Contact contact = new Contact();
		contact.setName( "John Doe" );
		contact.setEmailAddresses( emailAddresses );
		emailAddresses.forEach( address -> contact.getContactsByEmail().put(address, contact) );
		contact.setEmailAddresses3( emailAddresses3 );

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
					assertEquals( Hibernate.size( contactsByEmail.entrySet() ), 3 );
					Hibernate.remove( contactsByEmail, new EmailAddress( "test1@test.com" ) );
					assertFalse( Hibernate.isInitialized(contactsByEmail) );
//					assertEquals( cont, Hibernate.get(contactsByEmail, new EmailAddress( "test1@test.com" ) ) );
					assertEquals( Hibernate.size( contactsByEmail.entrySet() ), 2 );

					assertEquals( Hibernate.size( addresses ), 3 );
					assertFalse( Hibernate.isInitialized(addresses) );
					Hibernate.remove( addresses, new EmailAddress( "test1@test.com" ) );
					assertFalse( Hibernate.isInitialized(addresses) );
					assertEquals( Hibernate.size( addresses ), 2 );

					List<EmailAddress> addresses3 = cont.getEmailAddresses3();
					assertEquals( Hibernate.size(addresses3), 2 );
					Hibernate.remove( addresses3, new EmailAddress( "test4@test.com" ) );
					assertFalse( Hibernate.isInitialized( addresses3 ) );
					assertEquals( Hibernate.size(addresses3), 1 );
				}
		);
	}

}
