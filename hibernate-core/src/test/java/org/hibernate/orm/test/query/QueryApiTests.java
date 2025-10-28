/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import java.time.LocalDate;

import org.hibernate.query.IllegalQueryOperationException;

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
	public void testInvalidExecuteUpdateCall(SessionFactoryScope scope) {
		final String hql = "select c from Contact c";
		try {
			scope.inTransaction( (session) -> {
				session.createQuery( hql ).executeUpdate();
			} );
			fail( "Expecting failure" );
		}
		catch (IllegalStateException ise) {
			assertThat( ise.getCause() ).isNotNull();
			assertThat( ise.getCause() ).isInstanceOf( IllegalQueryOperationException.class );
			assertThat( ise.getMessage() ).endsWith( "[" + hql + "]" );
		}
	}

	@Test
	public void testInvalidSelectQueryCall(SessionFactoryScope scope) {
		final String hql = "delete Contact";

		scope.inTransaction( (session) -> {
			try {
				session.createQuery( hql ).list();
				fail( "Expecting failure" );
			}
			catch (IllegalStateException ise) {
				assertThat( ise.getCause() ).isNotNull();
				assertThat( ise.getCause() ).isInstanceOf( IllegalQueryOperationException.class );
				assertThat( ise.getMessage() ).endsWith( "[" + hql + "]" );
			}
		} );

		try {
			scope.inTransaction( (session) -> {
				session.createQuery( hql ).uniqueResult();
			} );
			fail( "Expecting failure" );
		}
		catch (IllegalStateException ise) {
			assertThat( ise.getCause() ).isNotNull();
			assertThat( ise.getCause() ).isInstanceOf( IllegalQueryOperationException.class );
			assertThat( ise.getMessage() ).endsWith( "[" + hql + "]" );
		}
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
