/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
		try {
			scope.inTransaction( (session) -> {
				session.createQuery( "select c from Contact c" ).executeUpdate();
			} );
			fail( "Expecting failure" );
		}
		catch (IllegalStateException ise) {
			assertThat( ise.getCause() ).isNotNull();
			assertThat( ise.getCause() ).isInstanceOf( IllegalQueryOperationException.class );
		}
	}

	@Test
	public void testInvalidSelectQueryCall(SessionFactoryScope scope) {
		try {
			scope.inTransaction( (session) -> {
				session.createQuery( "delete Contact" ).list();
			} );
			fail( "Expecting failure" );
		}
		catch (IllegalStateException ise) {
			assertThat( ise.getCause() ).isNotNull();
			assertThat( ise.getCause() ).isInstanceOf( IllegalQueryOperationException.class );
		}

		try {
			scope.inTransaction( (session) -> {
				session.createQuery( "delete Contact" ).uniqueResult();
			} );
			fail( "Expecting failure" );
		}
		catch (IllegalStateException ise) {
			assertThat( ise.getCause() ).isNotNull();
			assertThat( ise.getCause() ).isInstanceOf( IllegalQueryOperationException.class );
		}
	}

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Contact contact = new Contact(
					1,
					new Contact.Name( "John", "Jingleheimer-Schmidt"),
					Contact.Gender.MALE,
					LocalDate.EPOCH
			);
			session.persist( contact );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createQuery( "delete Contact" ).executeUpdate();
		} );
	}
}
