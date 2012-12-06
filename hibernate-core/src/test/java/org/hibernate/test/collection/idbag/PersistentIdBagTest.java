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
package org.hibernate.test.collection.idbag;
import java.util.ArrayList;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.collection.internal.PersistentIdentifierBag;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests related to operations on a PersistentIdentifierBag
 *
 * @author Steve Ebersole
 */
@FailureExpectedWithNewMetamodel( message = "<idbag> not supported" )
public class PersistentIdBagTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "collection/idbag/Mappings.hbm.xml" };
	}

	@Test
	public void testWriteMethodDirtying() {
		IdbagOwner parent = new IdbagOwner( "root" );
		IdbagOwner child = new IdbagOwner( "c1" );
		parent.getChildren().add( child );
		IdbagOwner otherChild = new IdbagOwner( "c2" );

		Session session = openSession();
		session.beginTransaction();
		session.save( parent );
		session.flush();
		// at this point, the list on parent has now been replaced with a PersistentBag...
		PersistentIdentifierBag children = ( PersistentIdentifierBag ) parent.getChildren();

		assertFalse( children.remove( otherChild ) );
		assertFalse( children.isDirty() );

		ArrayList otherCollection = new ArrayList();
		otherCollection.add( child );
		assertFalse( children.retainAll( otherCollection ) );
		assertFalse( children.isDirty() );

		otherCollection = new ArrayList();
		otherCollection.add( otherChild );
		assertFalse( children.removeAll( otherCollection ) );
		assertFalse( children.isDirty() );

		children.clear();
		session.delete( child );
		assertTrue( children.isDirty() );

		session.flush();

		children.clear();
		assertFalse( children.isDirty() );

		session.delete( parent );
		session.getTransaction().commit();
		session.close();
	}
}
