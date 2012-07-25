/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.annotations.identifiercollection;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Emmanuel Bernard
 */
@FailureExpectedWithNewMetamodel
public class IdentifierCollectionTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testIdBag() throws Exception {
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
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( passport );
		s.flush();
		//s.clear();
		passport = (Passport) s.get( Passport.class, passport.getId() );
		int canadaCount = 0;
		for ( Stamp stamp : passport.getStamps() ) {
			if ( "Canada".equals( stamp.getCountry() ) ) canadaCount++;
		}
		assertEquals( 2, canadaCount );
		assertEquals( 1, passport.getVisaStamp().size() );
		tx.rollback();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Passport.class,
				Stamp.class
		};
	}
}
