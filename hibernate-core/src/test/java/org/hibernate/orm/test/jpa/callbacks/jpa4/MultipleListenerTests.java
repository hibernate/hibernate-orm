/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks.jpa4;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = {
		Book.class, Person.class, Publisher.class, CreationWatcher.class, BookWatcher.class
} )
@SessionFactory
public class MultipleListenerTests {
	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	void testOverloadedListenerMethodsByCallbackTarget(SessionFactoryScope factoryScope) {
		EventSink.reset();

		factoryScope.inTransaction( (session) -> {
			var publisher = new Publisher( 1, "Hibernate Press" );
			var author = new Person( 1, "Billy" );
			session.persist( publisher );
			session.persist( author );
			session.persist( new Book( 1, "123456789", "The Wrath of Rings", author ) );
		} );

		// CreationWatcher
		assertThat( EventSink.personCreationEvents ).containsExactly( CreationWatcher.class );

		// CreationWatcher & BookWatcher
		// 		- because they are both @EntityListener, the order is undefined
		assertThat( EventSink.bookCreationEvents ).containsExactlyInAnyOrder( CreationWatcher.class, BookWatcher.class );


		// PublisherListener & CreationWatcher & Publisher (callback)
		//		- only PublisherListener is an @EntityListener, so the order is well-defined
		assertThat( EventSink.publisherCreationEvents ).containsExactly( CreationWatcher.class, PublisherListener.class, Publisher.class );
	}
}
