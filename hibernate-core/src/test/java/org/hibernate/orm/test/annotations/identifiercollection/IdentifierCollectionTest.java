/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.identifiercollection;


import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Emmanuel Bernard
 */
public class IdentifierCollectionTest extends SessionFactoryBasedFunctionalTest {

	@Test
	public void testIdBag() {
		Passport passport = new Passport();
		passport.setName( "Emmanuel Bernard" );
		Stamp canada = new Stamp();
		canada.setCountry( "Canada" );
		passport.getStamps().add( canada );
		passport.getVisaStamp().add( canada );
		Stamp norway = new Stamp();
		norway.setCountry( "Norway" );
		passport.getStamps().add( norway );
		passport.getStamps().add(canada);

		inTransaction(
				session -> {
					session.persist( passport );
					session.flush();
					//s.clear();
					Passport savedPassport = session.get( Passport.class, passport.getId() );
					int canadaCount = 0;
					for ( Stamp stamp : savedPassport.getStamps() ) {
						if ( "Canada".equals( stamp.getCountry() ) ) canadaCount++;
					}
					assertEquals( 2, canadaCount );
					assertEquals( 1, savedPassport.getVisaStamp().size() );
				}
		);
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Passport.class,
				Stamp.class
		};
	}
}
