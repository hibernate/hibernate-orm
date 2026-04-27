/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import java.time.LocalDate;

import org.hibernate.query.IllegalMutationQueryException;

import org.hibernate.query.IllegalSelectQueryException;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.contacts.Contact;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@DomainModel( standardModels = StandardDomainModel.CONTACTS )
@SessionFactory
public class QueryApiTests {

	@Test
	public void testInvalidMutationQuery(SessionFactoryScope scope) {
		final String hql = "select c from Contact c";
		try {
			scope.inTransaction( (session) -> {
				session.createStatement( hql ).executeUpdate();
			} );
			fail( "Expecting failure" );
		}
		catch (IllegalArgumentException iae) {
			assertThat( iae.getCause() ).isNull();
			assertThat( iae.getSuppressed() ).hasSize( 1 );
			assertThat( iae.getSuppressed()[0] ).isInstanceOf( IllegalMutationQueryException.class );
			assertThat( iae.getSuppressed()[0].getMessage() ).endsWith( "[" + hql + "]" );
		}
	}

	@Test
	public void testInvalidSelectionQuery(SessionFactoryScope scope) {
		final String hql = "delete Contact";

		scope.inTransaction( (session) -> {
			try {
				session.createQuery( hql, Contact.class );
				fail( "Expecting failure" );
			}
			catch (IllegalArgumentException iae) {
				assertThat( iae.getCause() ).isNull();
				assertThat( iae.getSuppressed() ).hasSize( 1 );
				assertThat( iae.getSuppressed()[0] ).isInstanceOf( IllegalSelectQueryException.class );
				assertThat( iae.getSuppressed()[0].getMessage() ).endsWith( "[" + hql + "]" );
			}
		} );
	}

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Contact contact = new Contact(
					1,
					new Contact.Name( "John", "Jingleheimer-Schmidt"),
					Contact.Gender.MALE,
					LocalDate.of(1970, 1, 1)
			);
			session.persist( contact );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}
