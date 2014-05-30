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
package org.hibernate.test.ops;

import org.hibernate.Session;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
public class DeleteTest extends AbstractOperationTestCase {
	@Test
	@SuppressWarnings( {"unchecked"})
	public void testDeleteVersionedWithCollectionNoUpdate() {
		// test adapted from HHH-1564...
		Session s = openSession();
		s.beginTransaction();
		VersionedEntity c = new VersionedEntity( "c1", "child-1" );
		VersionedEntity p = new VersionedEntity( "root", "root");
		p.getChildren().add( c );
		c.setParent( p );
		s.save( p );
		s.getTransaction().commit();
		s.close();

		clearCounts();

		s = openSession();
		s.beginTransaction();
        VersionedEntity loadedParent = ( VersionedEntity ) s.get( VersionedEntity.class, "root" );
        s.delete( loadedParent );
		s.getTransaction().commit();
        s.close();

		assertInsertCount( 0 );
		assertUpdateCount( 0 );
		assertDeleteCount( 2 );
	}

	@Test
	public void testNoUpdateOnDelete() {
		Session s = openSession();
        s.beginTransaction();
		Node node = new Node( "test" );
		s.persist( node );
		s.getTransaction().commit();
		s.close();

		clearCounts();

		s = openSession();
		s.beginTransaction();
		s.delete( node );
		s.getTransaction().commit();
		s.close();

		assertUpdateCount( 0 );
		assertInsertCount( 0 );
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testNoUpdateOnDeleteWithCollection() {
		Session s = openSession();
        s.beginTransaction();
		Node parent = new Node( "parent" );
		Node child = new Node( "child" );
		parent.getCascadingChildren().add( child );
		s.persist( parent );
		s.getTransaction().commit();
		s.close();

		clearCounts();

		s = openSession();
		s.beginTransaction();
		parent = ( Node ) s.get( Node.class, "parent" );
		s.delete( parent );
		s.getTransaction().commit();
		s.close();

		assertUpdateCount( 0 );
		assertInsertCount( 0 );
		assertDeleteCount( 2 );
	}
}
