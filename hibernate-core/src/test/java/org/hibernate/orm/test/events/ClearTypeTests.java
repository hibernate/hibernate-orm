/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.events;

import java.time.LocalDate;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.contacts.Contact;
import org.hibernate.testing.orm.domain.contacts.Contact.Name;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.time.LocalDate.EPOCH;
import static java.time.LocalDate.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.orm.domain.contacts.Contact.Gender.MALE;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel( standardModels = StandardDomainModel.CONTACTS )
@SessionFactory
public class ClearTypeTests {
	@Test
	void testSimpleUsage(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Contact contact = session.find( Contact.class, 1 );
			session.clear( Contact.class  );
			assertThat( session.contains( contact ) ).isFalse();
		} );
	}

	@Test
	void testPendingPersist(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Contact johnny = new Contact( 2, new Name( "Johnny", "Rotten" ), MALE, EPOCH );
			session.persist( johnny );
			session.clear( Contact.class  );
			assertThat( session.contains( johnny ) ).isFalse();
		} );
		scope.inTransaction( (session) -> {
			final Contact loaded = session.find( Contact.class, 2 );
			assertThat( loaded ).isNull();
		} );
	}

	@Test
	void testPendingUpdate(SessionFactoryScope scope) {
		final Contact loaded = scope.fromTransaction( (session) -> {
			return session.find( Contact.class, 1 );
		} );

		loaded.setBirthDay( now() );
		scope.inTransaction( (session) -> {
			final Contact merged = session.merge( loaded );
			session.clear( Contact.class  );
			assertThat( session.contains( merged ) ).isFalse();
			assertThat( session.contains( loaded ) ).isFalse();
		} );

		scope.inTransaction( (session) -> {
			final Contact reloaded = session.find( Contact.class, 1 );
			assertThat( reloaded.getBirthDay() ).isEqualTo( EPOCH );
		} );
	}

	@Test
	void testPendingDelete(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Contact loaded = session.find( Contact.class, 1 );
			session.remove( loaded );
			session.clear( Contact.class  );
			assertThat( session.contains( loaded ) ).isFalse();
		} );

		scope.inTransaction( (session) -> {
			final Contact reloaded = session.find( Contact.class, 1 );
			assertThat( reloaded ).isNotNull();
		} );
	}

	@BeforeEach
	void createTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new Contact( 1, new Name( "Joe", "Black" ), MALE, EPOCH ) );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createMutationQuery( "delete Contact" ).executeUpdate();
		} );
	}
}
