/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.flush;

import java.util.Iterator;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * Thanks to Jan Hodac and Laurent Almeras for providing test cases for this
 * issue.
 * 
 * @author Guillaume Smet
 */
@TestForIssue(jiraKey = "HHH-7821")
public class TestClearBatchFetchQueueAfterFlush extends BaseCoreFunctionalTestCase {

	public void configure(Configuration cfg) {
		cfg.setProperty( Environment.DEFAULT_BATCH_FETCH_SIZE, "10" );
	}

	@Test
	public void testClearBatchFetchQueueAfterFlush() {
		Session s = openSession();
		s.beginTransaction();

		Author author1 = new Author( "David Lodge" );
		author1.getBooks().add( new Book( "A Man of Parts", author1 ) );
		author1.getBooks().add( new Book( "Thinks...", author1 ) );
		author1.getBooks().add( new Book( "Therapy", author1 ) );
		s.save( author1 );

		Iterator<Book> bookIterator = author1.getBooks().iterator();

		BookStore bookStore1 = new BookStore( "Passages" );
		bookStore1.getBooks().add( bookIterator.next() );
		s.save( bookStore1 );

		BookStore bookStore2 = new BookStore( "Librairie du Tramway" );
		bookStore2.getBooks().add( bookIterator.next() );
		s.save( bookStore2 );

		BookStore bookStore3 = new BookStore( "Le Bal des Ardents" );
		bookStore3.getBooks().add( bookIterator.next() );
		s.save( bookStore3 );

		s.flush();
		s.getTransaction().commit();
		s.clear();

		bookStore1 = s.load( BookStore.class, bookStore1.getId() );
		bookStore2 = s.load( BookStore.class, bookStore2.getId() );
		bookStore3 = s.load( BookStore.class, bookStore3.getId() );

		s.beginTransaction();
		s.delete( bookStore2 );
		s.getTransaction().commit();

		bookStore1.getBooks().size();
		bookStore3.getBooks().size();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Author.class, Book.class, Publisher.class, BookStore.class };
	}

}
