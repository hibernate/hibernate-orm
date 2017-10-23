/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.ops;

import org.junit.Test;

import org.hibernate.Session;

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
