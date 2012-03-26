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
package org.hibernate.test.collection.custom.parameterized;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

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
		entity = ( Entity ) s.get( Entity.class, "tester" );
		assertTrue( Hibernate.isInitialized( entity.getValues() ) );
		assertEquals( 1, entity.getValues().size() );
        assertEquals( "Hello", ( ( DefaultableList ) entity.getValues() ).getDefaultValue() );
		s.delete( entity );
		t.commit();
		s.close();
	}
}
