/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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

		bookStore1 = (BookStore) s.load( BookStore.class, bookStore1.getId() );
		bookStore2 = (BookStore) s.load( BookStore.class, bookStore2.getId() );
		bookStore3 = (BookStore) s.load( BookStore.class, bookStore3.getId() );

		s.beginTransaction();
		s.delete( bookStore2 );
		s.getTransaction().commit();

		s.flush();

		bookStore1.getBooks().size();
		bookStore3.getBooks().size();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Author.class, Book.class, Publisher.class, BookStore.class };
	}

}
