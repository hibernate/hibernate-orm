/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.session.runtime.check;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.hibernate.query.Query;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Fabio Massimo Ercoli
 */
@TestForIssue(jiraKey = "HHH-13604")
public class CheckForbiddenAPIAtRuntimeTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Book.class };
	}

	@Test
	public void smoke() {
		Book book = new Book();
		book.setId( 1 );
		book.setIsbn( "777-33-99999-11-7" );
		book.setTitle( "Songs of Innocence and Experience" );
		book.setAuthor( "William Blake" );
		book.setCopies( 1_000_000 );

		doInHibernate( this::sessionFactory, session -> {
			session.persist( book );
		} );

		doInHibernate( this::sessionFactory, session -> {
			Book loaded = session.load( Book.class, 1 );
			assertEquals( book, loaded );

			loaded.setCopies( 999_999 );
		} );

		doInHibernate( this::sessionFactory, session -> {
			Book loaded = session.load( Book.class, 1 );
			assertEquals( Integer.valueOf( 999_999 ), loaded.getCopies() );

			session.remove( loaded );
		} );

		doInHibernate( this::sessionFactory, session -> {
			Query<Book> query = session.createQuery( "select b from Book b", Book.class );
			List<Book> books = query.getResultList();

			assertEquals( 0, books.size() );
		} );
	}
}
