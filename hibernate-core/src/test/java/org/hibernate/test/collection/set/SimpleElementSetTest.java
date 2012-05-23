/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2006-2012, Red Hat Inc. or third-party contributors as
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

package org.hibernate.test.collection.set;

import java.sql.SQLException;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
public class SimpleElementSetTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "collection/set/User.hbm.xml" };
	}

	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( USE_NEW_METADATA_MAPPINGS, "true");
	}

	@Test
	public void testLoad() throws HibernateException, SQLException {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User u = new User( );
		u.setUserName( "username" );
		u.getSessionAttributeNames().add( "name" );
		u.getSessionAttributeNames().add( "anothername" );
		s.save( u );
		t.commit();
		s.close();
		s = openSession();
		t = s.beginTransaction();
		u = (User) s.get( User.class, "username" );
		assertTrue( Hibernate.isInitialized( u.getSessionAttributeNames() ) );
		assertEquals( 2, u.getSessionAttributeNames().size() );
		assertTrue( Hibernate.isInitialized( u.getSessionAttributeNames() ) );
		assertTrue( u.getSessionAttributeNames().contains( "name" ) );
		assertTrue( u.getSessionAttributeNames().contains( "anothername" ) );
		s.delete( u );
		t.commit();
		s.close();
	}
}
