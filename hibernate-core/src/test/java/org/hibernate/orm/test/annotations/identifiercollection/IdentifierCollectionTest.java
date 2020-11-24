/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.identifiercollection;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = {
				Passport.class,
				Stamp.class
		}
)
@SessionFactory
public class IdentifierCollectionTest {

	@Test
	public void testIdBag(SessionFactoryScope scope) {

		scope.inTransaction(
				session -> {
					Passport passport = new Passport();
					passport.setName( "Emmanuel Bernard" );
					Stamp canada = new Stamp();
					canada.setCountry( "Canada" );
					passport.getStamps().add( canada );
					passport.getVisaStamp().add( canada );
					Stamp norway = new Stamp();
					norway.setCountry( "Norway" );
					passport.getStamps().add( norway );
					passport.getStamps().add( canada );
					session.persist( passport );
					session.flush();
					//s.clear();
					passport = session.get( Passport.class, passport.getId() );
					int canadaCount = 0;
					for ( Stamp stamp : passport.getStamps() ) {
						if ( "Canada".equals( stamp.getCountry() ) ) {
							canadaCount++;
						}
					}
					assertEquals( 2, canadaCount );
					assertEquals( 1, passport.getVisaStamp().size() );
				}
		);
	}
}
