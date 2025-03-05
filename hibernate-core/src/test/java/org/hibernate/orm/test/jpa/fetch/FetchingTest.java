/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.fetch;

import java.util.Date;

import org.hibernate.Hibernate;

import org.hibernate.orm.test.jpa.model.AbstractJPATest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Emmanuel Bernard
 */
public class FetchingTest extends AbstractJPATest {
	@Override
	public String[] getOrmXmlFiles() {
		return new String[] { "org/hibernate/orm/test/jpa/fetch/Person.hbm.xml" };
	}

	@Test
	public void testLazy() {
		inTransaction(
				session -> {
					Person p = new Person( "Gavin", "King", "JBoss Inc" );
					Stay stay = new Stay( p, new Date(), new Date(), "A380", "Blah", "Blah" );
					p.addStay( stay );
					session.persist( p );
					session.getTransaction().commit();
					session.clear();
					session.beginTransaction();
					p = (Person) session.createQuery( "select p from Person p where p.firstName = :name" )
							.setParameter( "name", "Gavin" ).uniqueResult();
					assertFalse( Hibernate.isInitialized( p.getStays() ) );
					session.remove( p );
				}
		);
	}

	@Test
	public void testHibernateFetchingLazy() {
		inTransaction(
				session -> {
					Person p = new Person( "Gavin", "King", "JBoss Inc" );
					Stay stay = new Stay( null, new Date(), new Date(), "A380", "Blah", "Blah" );
					Stay stay2 = new Stay( null, new Date(), new Date(), "A320", "Blah", "Blah" );
					Stay stay3 = new Stay( null, new Date(), new Date(), "A340", "Blah", "Blah" );
					stay.setOldPerson( p );
					stay2.setVeryOldPerson( p );
					stay3.setVeryOldPerson( p );
					p.addOldStay( stay );
					p.addVeryOldStay( stay2 );
					p.addVeryOldStay( stay3 );
					session.persist( p );
					session.getTransaction().commit();

					session.clear();
					session.beginTransaction();
					p = (Person) session.createQuery( "select p from Person p where p.firstName = :name" )
							.setParameter( "name", "Gavin" ).uniqueResult();
					assertFalse( Hibernate.isInitialized( p.getOldStays() ) );
					assertEquals( 1, p.getOldStays().size() );
					assertFalse( Hibernate.isInitialized( p.getOldStays() ), "lazy extra is failing" );
					session.clear();
					stay = session.get( Stay.class, stay.getId() );

					assertTrue( !Hibernate.isInitialized( stay.getOldPerson() ) );
					session.clear();
					stay3 = session.get( Stay.class, stay3.getId() );
					assertTrue(
							Hibernate.isInitialized( stay3.getVeryOldPerson() ),
							"FetchMode.JOIN should overrides lazy options"
					);
					session.remove( stay3.getVeryOldPerson() );
				}
		);
	}
}
