/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.subselect;

import org.hibernate.Session;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@JiraKey(value = "HHH-12590")
@RequiresDialect(PostgreSQLDialect.class)
public class SubselectAliasPostgresTest extends BaseCoreFunctionalTestCase {

	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/orm/test/";
	}

	@Override
	public String[] getMappings() {
		return new String[] {"subselect/Book-lazy-extra.hbm.xml"};
	}

	@Test
	public void testSubselect() {
		Session s = openSession();

		s.getTransaction().begin();
		try {
			Author b = new Author();
			b.setName( "Camilleri" );
			b.setId( 1 );

			s.persist( b );

			Book movie = new Book();
			movie.setId( 2 );
			movie.setAuthorId( 1 );
			movie.setTitle( "Il sognaglio" );
			s.persist( movie );

			Book movie2 = new Book();
			movie2.setId( 3 );
			movie2.setAuthorId( 1 );
			movie2.setTitle( "Il casellante" );

			s.persist( movie2 );

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
			Author director = s.get( Author.class, 1 );
			assertThat( director.getBooks().size(), is( 2 ) );
		}
		finally {
			s.close();
		}
	}

}
