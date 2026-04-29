/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks.jpa4;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.NotImplementedYet;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = {Book.class, Journaler.class})
@SessionFactory
public class BasicListenerTests {
	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	@NotImplementedYet
	void testSimplyLifecycle(SessionFactoryScope factoryScope) {
		Journaler.reset();
		factoryScope.inTransaction( (session) -> {
			session.persist(  new Book( 1, "123456789", "The Wrath of Rings" ) );
		} );

		assertThat( Journaler.bookCreateCount ).isEqualTo( 1 );
		assertThat( Journaler.bookUpdateCount ).isEqualTo( 0 );
		assertThat( Journaler.bookDeleteCount ).isEqualTo( 0 );

		Journaler.reset();

		factoryScope.inTransaction( (session) -> {
			var book = session.get( Book.class, 1 );
			book.setIsbn( "1234567890" );
		} );

		assertThat( Journaler.bookCreateCount ).isEqualTo( 0 );
		assertThat( Journaler.bookUpdateCount ).isEqualTo( 1 );
		assertThat( Journaler.bookDeleteCount ).isEqualTo( 0 );

		Journaler.reset();

		factoryScope.inTransaction( (session) -> {
			var book = session.get( Book.class, 1 );
			session.remove( book );
		} );

		assertThat( Journaler.bookCreateCount ).isEqualTo( 0 );
		assertThat( Journaler.bookUpdateCount ).isEqualTo( 0 );
		assertThat( Journaler.bookDeleteCount ).isEqualTo( 1 );
	}
}
