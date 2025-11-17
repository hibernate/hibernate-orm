/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.flush;

import java.util.Iterator;

import org.hibernate.Session;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.FetchSettings.DEFAULT_BATCH_FETCH_SIZE;

/**
 * Thanks to Jan Hodac and Laurent Almeras for providing test cases for this
 * issue.
 *
 * @author Guillaume Smet
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-7821")
@ServiceRegistry(settings = @Setting(name = DEFAULT_BATCH_FETCH_SIZE, value = "10"))
@DomainModel(annotatedClasses = { Author.class, Book.class, Publisher.class, BookStore.class })
@SessionFactory
public class TestClearBatchFetchQueueAfterFlush {
	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testClearBatchFetchQueueAfterFlush(SessionFactoryScope factoryScope) {
		try (Session s = factoryScope.getSessionFactory().openSession()) {
			s.beginTransaction();

			Author author1 = new Author( "David Lodge" );
			author1.getBooks().add( new Book( "A Man of Parts", author1 ) );
			author1.getBooks().add( new Book( "Thinks...", author1 ) );
			author1.getBooks().add( new Book( "Therapy", author1 ) );
			s.persist( author1 );

			Iterator<Book> bookIterator = author1.getBooks().iterator();

			BookStore bookStore1 = new BookStore( "Passages" );
			bookStore1.getBooks().add( bookIterator.next() );
			s.persist( bookStore1 );

			BookStore bookStore2 = new BookStore( "Librairie du Tramway" );
			bookStore2.getBooks().add( bookIterator.next() );
			s.persist( bookStore2 );

			BookStore bookStore3 = new BookStore( "Le Bal des Ardents" );
			bookStore3.getBooks().add( bookIterator.next() );
			s.persist( bookStore3 );

			s.flush();
			s.getTransaction().commit();
			s.clear();

			bookStore1 = s.getReference( BookStore.class, bookStore1.getId() );
			bookStore2 = s.getReference( BookStore.class, bookStore2.getId() );
			bookStore3 = s.getReference( BookStore.class, bookStore3.getId() );

			s.beginTransaction();
			s.remove( bookStore2 );
			s.getTransaction().commit();

			bookStore1.getBooks().size();
			bookStore3.getBooks().size();
		}
	}
}
