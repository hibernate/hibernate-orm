/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.subselect;

import org.hibernate.Session;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import org.junit.Test;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "")
public class SetSubselectTest extends BaseCoreFunctionalTestCase {

	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/orm/test/";
	}

	@Override
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

			s.persist( b );

			Book book = new Book();
			book.setId( 2 );
			book.setAuthorId( 1 );
			book.setTitle( "Il sognaglio" );
			s.persist( book );

			Book book2 = new Book();
			book2.setId( 3 );
			book2.setAuthorId( 1 );
			book2.setTitle( "Il casellante" );

			s.persist( book2 );

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
