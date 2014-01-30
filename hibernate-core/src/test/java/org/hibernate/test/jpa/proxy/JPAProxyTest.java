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
package org.hibernate.test.jpa.proxy;

import javax.persistence.EntityNotFoundException;

import junit.framework.AssertionFailedError;
import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.jpa.AbstractJPATest;
import org.hibernate.test.jpa.Item;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Test relation between proxies and get()/load() processing
 * and make sure the interactions match the ejb3 expectations
 *
 * @author Steve Ebersole
 */
@FailureExpectedWithNewMetamodel
public class JPAProxyTest extends AbstractJPATest {
	@Test
	public void testEjb3ProxyUsage() {
		Session s = openSession();
		Transaction txn = s.beginTransaction();

		Item item = ( Item ) s.load( Item.class, new Long(-1) );
		assertFalse( Hibernate.isInitialized( item ) );
		try {
			Hibernate.initialize( item );
			fail( "proxy access did not fail on non-existent proxy" );
		}
		catch ( EntityNotFoundException e ) {
			// expected behavior
		}
		catch ( Throwable t ) {
			fail( "unexpected exception type on non-existent proxy access : " + t );
		}

		s.clear();

		Item item2 = ( Item ) s.load( Item.class, new Long(-1) );
		assertFalse( Hibernate.isInitialized( item2 ) );
		assertFalse( item == item2 );
		try {
			item2.getName();
			fail( "proxy access did not fail on non-existent proxy" );
		}
		catch ( EntityNotFoundException e ) {
			// expected behavior
		}
		catch ( Throwable t ) {
			fail( "unexpected exception type on non-existent proxy access : " + t );
		}

		txn.commit();
		s.close();
	}

	/**
	 * The ejb3 find() method maps to the Hibernate get() method
	 */
	@Test
	public void testGetSemantics() {
		Long nonExistentId = new Long( -1 );
		Session s = openSession();
		Transaction txn = s.beginTransaction();
		Item item = ( Item ) s.get( Item.class, nonExistentId );
		assertNull( "get() of non-existent entity did not return null", item );
		txn.commit();
		s.close();

		s = openSession();
		txn = s.beginTransaction();
		// first load() it to generate a proxy...
		item = ( Item ) s.load( Item.class, nonExistentId );
		assertFalse( Hibernate.isInitialized( item ) );
		// then try to get() it to make sure we get an exception
		try {
			s.get( Item.class, nonExistentId );
			fail( "force load did not fail on non-existent entity" );
		}
		catch ( EntityNotFoundException e ) {
			// expected behavior
		}
		catch( AssertionFailedError e ) {
			throw e;
		}
		catch ( Throwable t ) {
			fail( "unexpected exception type on non-existent entity force load : " + t );
		}
		txn.commit();
		s.close();
	}
}
