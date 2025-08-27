/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import java.time.LocalDate;
import java.util.List;

import org.hibernate.query.Query;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.contacts.Address;
import org.hibernate.testing.orm.domain.contacts.Contact;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Tuple;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Christian Beikov
 */
@DomainModel(standardModels = StandardDomainModel.CONTACTS)
@SessionFactory
public class CteCycleTests {

	@Test
	@JiraKey( "HHH-16465" )
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsRecursiveCtes.class)
	public void testRecursiveCycleClause(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Query<Tuple> query = session.createQuery(
							"with alternativeContacts as (" +
									"select c.alternativeContact alt from Contact c where c.id = :param " +
									"union all " +
									"select c.alt.alternativeContact alt from alternativeContacts c" +
									")" +
									"cycle alt set isCycle to true default false " +
									"select ac, c.isCycle from alternativeContacts c join c.alt ac order by ac.id, c.isCycle",
							Tuple.class
					);
					List<Tuple> list = query.setParameter( "param", 1 ).getResultList();
					assertEquals( 4, list.size() );
					assertEquals( "John", list.get( 0 ).get( 0, Contact.class ).getName().getFirst() );
					assertEquals( "Jane", list.get( 1 ).get( 0, Contact.class ).getName().getFirst() );
					assertFalse( list.get( 1 ).get( 1, Boolean.class ) );
					assertEquals( "Jane", list.get( 2 ).get( 0, Contact.class ).getName().getFirst() );
					assertTrue( list.get( 2 ).get( 1, Boolean.class ) );
					assertEquals( "Granny", list.get( 3 ).get( 0, Contact.class ).getName().getFirst() );
				}
		);
	}

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Contact contact = new Contact(
					1,
					new Contact.Name( "John", "Doe" ),
					Contact.Gender.MALE,
					LocalDate.of( 1970, 1, 1 )
			);
			final Contact alternativeContact = new Contact(
					11,
					new Contact.Name( "Jane", "Doe" ),
					Contact.Gender.FEMALE,
					LocalDate.of( 1970, 1, 1 )
			);
			final Contact alternativeContact2 = new Contact(
					111,
					new Contact.Name( "Granny", "Doe" ),
					Contact.Gender.FEMALE,
					LocalDate.of( 1970, 1, 1 )
			);
			alternativeContact.setAlternativeContact( alternativeContact2 );
			contact.setAlternativeContact( alternativeContact );
			contact.setAddresses(
					List.of(
							new Address( "Street 1", 1234 ),
							new Address( "Street 2", 5678 )
					)
			);
			session.persist( alternativeContact2 );
			session.persist( alternativeContact );
			session.persist( contact );
			alternativeContact2.setAlternativeContact( contact );

		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}
