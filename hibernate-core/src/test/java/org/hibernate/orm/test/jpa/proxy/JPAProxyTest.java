/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.proxy;

import org.hibernate.Hibernate;

import org.hibernate.orm.test.jpa.model.AbstractJPATest;
import org.hibernate.orm.test.jpa.model.Item;
import org.junit.jupiter.api.Test;
import junit.framework.AssertionFailedError;

import jakarta.persistence.EntityNotFoundException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * Test relation between proxies and get()/load() processing
 * and make sure the interactions match the ejb3 expectations
 *
 * @author Steve Ebersole
 */
public class JPAProxyTest extends AbstractJPATest {

	@Test
	public void testEjb3ProxyUsage() {
		inTransaction(
				s -> {
					Item item = s.getReference( Item.class, new Long( -1 ) );
					assertFalse( Hibernate.isInitialized( item ) );
					try {
						Hibernate.initialize( item );
						fail( "proxy access did not fail on non-existent proxy" );
					}
					catch (EntityNotFoundException e) {
						// expected behavior
					}
					catch (Throwable t) {
						fail( "unexpected exception type on non-existent proxy access : " + t );
					}

					s.clear();

					Item item2 = s.getReference( Item.class, new Long( -1 ) );
					assertFalse( Hibernate.isInitialized( item2 ) );
					assertFalse( item == item2 );
					try {
						item2.getName();
						fail( "proxy access did not fail on non-existent proxy" );
					}
					catch (EntityNotFoundException e) {
						// expected behavior
					}
					catch (Throwable t) {
						fail( "unexpected exception type on non-existent proxy access : " + t );
					}
				}
		);
	}

	/**
	 * The ejb3 find() method maps to the Hibernate get() method
	 */
	@Test
	public void testGetSemantics() {
		Long nonExistentId = new Long( -1 );
		inTransaction(
				session -> {
					Item item = session.get( Item.class, nonExistentId );
					assertNull(  item , "get() of non-existent entity did not return null");
				}
		);

		inTransaction(
				s -> {
					// first load() it to generate a proxy...
					Item item = s.getReference( Item.class, nonExistentId );
					assertFalse( Hibernate.isInitialized( item ) );
					// then try to get() it to make sure we get an exception
					try {
						s.get( Item.class, nonExistentId );
						fail( "force load did not fail on non-existent entity" );
					}
					catch (EntityNotFoundException e) {
						// expected behavior
					}
					catch (AssertionFailedError e) {
						throw e;
					}
					catch (Throwable t) {
						fail( "unexpected exception type on non-existent entity force load : " + t );
					}
				}
		);
	}
}
