/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.subselect;

import org.hibernate.Session;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "")
public class SetSubselectTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] {"subselect/Book.hbm.xml"};
	}

	@Test
	public void testSubselect() {
		Session s = openSession();

		s.getTransaction().begin();
		try {
			Author b = new Author();
			b.setName( "Camilleri" );
			b.setId( 1 );

			s.save( b );

			Book book = new Book();
			book.setId( 2 );
			book.setAuthorId( 1 );
			book.setTitle( "Il sognaglio" );
			s.save( book );

			Book book2 = new Book();
			book2.setId( 3 );
			book2.setAuthorId( 1 );
			book2.setTitle( "Il casellante" );

			s.save( book2 );

			s.getTransaction().commit();
		}
		catch (Exception e) {
			if ( s.getTransaction().getStatus() == TransactionStatus.ACTIVE ) {
				s.getTransaction().rollback();
			}
			fail( e.getMessage() );
		}
		finally {
			s.close();
		}

		s = openSession();
		try {
			Author author = s.get( Author.class, 1 );
			assertThat( author.getBooks().size(), is( 2 ) );
		}
		finally {
			s.close();
		}
	}

}
