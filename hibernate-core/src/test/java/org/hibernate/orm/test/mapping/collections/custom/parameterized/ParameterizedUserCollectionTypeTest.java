/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.custom.parameterized;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tes for parameterized user collection types.
 *
 * @author Holger Brands
 * @author Steve Ebersole
 */
public abstract class ParameterizedUserCollectionTypeTest extends BaseCoreFunctionalTestCase {
	@SuppressWarnings( {"unchecked"})
	@Test
	public void testBasicOperation() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Entity entity = new Entity( "tester" );
		entity.getValues().add( "value-1" );
		s.persist( entity );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		entity = s.get( Entity.class, "tester" );
		assertTrue( Hibernate.isInitialized( entity.getValues() ) );
		assertEquals( 1, entity.getValues().size() );
		assertEquals( "Hello", ( ( DefaultableList ) entity.getValues() ).getDefaultValue() );
		s.remove( entity );
		t.commit();
		s.close();
	}
}
