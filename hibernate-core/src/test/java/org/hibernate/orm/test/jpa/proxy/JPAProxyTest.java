/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.proxy;

import org.hibernate.Hibernate;

import org.hibernate.orm.test.jpa.model.AbstractJPATest;
import org.hibernate.orm.test.jpa.model.Item;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityNotFoundException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
					Item item = s.getReference( Item.class, -1L );
					assertFalse( Hibernate.isInitialized( item ) );
					assertThrows(
							EntityNotFoundException.class,
							() -> Hibernate.initialize(item),
						"proxy access did not fail on non-existent proxy"
					);

					s.clear();

					Item item2 = s.getReference( Item.class, -1L );
					assertFalse( Hibernate.isInitialized( item2 ) );
					assertFalse( item == item2 );
					assertThrows(
							EntityNotFoundException.class,
							item2::getName,
							"proxy access did not fail on non-existent proxy"
					);
				}
		);
	}

	/**
	 * The ejb3 find() method maps to the Hibernate get() method
	 */
	@Test
	public void testGetSemantics() {
		Long nonExistentId = -1L;
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
					assertThrows(
							EntityNotFoundException.class,
							() -> s.get( Item.class, nonExistentId ),
							"force load did not fail on non-existent entity"
					);
				}
		);
	}
}
