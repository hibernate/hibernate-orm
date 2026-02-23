/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.subselect;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.dialect.PostgreSQLDialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@JiraKey(value = "HHH-12590")
@RequiresDialect(PostgreSQLDialect.class)
@DomainModel(annotatedClasses = {AuthorAnnotated.class, BookAnnotated.class, BookSubselectView.class})
@SessionFactory
public class SubselectAliasPostgresTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testSubselect(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			AuthorAnnotated author = new AuthorAnnotated();
			author.setName( "Camilleri" );
			author.setId( 1 );
			session.persist( author );

			BookAnnotated book = new BookAnnotated();
			book.setId( 2 );
			book.setAuthorId( 1 );
			book.setTitle( "Il sognaglio" );
			session.persist( book );

			BookAnnotated book2 = new BookAnnotated();
			book2.setId( 3 );
			book2.setAuthorId( 1 );
			book2.setTitle( "Il casellante" );
			session.persist( book2 );
		} );

		scope.inTransaction( session -> {
			AuthorAnnotated author = session.find( AuthorAnnotated.class, 1 );

			PersistentCollection<?> booksCollection = (PersistentCollection<?>) author.getBooks();
			int size = booksCollection.getSize();

			assertThat( size, is( 2 ) );
		} );
	}
}
