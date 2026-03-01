/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.identifiercollection;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
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
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsConcurrentTransactions.class )
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
