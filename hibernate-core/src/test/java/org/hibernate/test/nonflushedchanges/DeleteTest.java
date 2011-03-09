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
package org.hibernate.test.nonflushedchanges;


import org.hibernate.Session;

import org.junit.Test;

import org.hibernate.testing.tm.SimpleJtaTransactionManagerImpl;

/**
 * adapted this from "ops" tests version
 *
 * @author Gail Badner
 * @author Steve Ebersole
 */
public class DeleteTest extends AbstractOperationTestCase {
	@Test
	@SuppressWarnings( {"unchecked"})
	public void testDeleteVersionedWithCollectionNoUpdate() throws Exception {
		// test adapted from HHH-1564...
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		VersionedEntity c = new VersionedEntity( "c1", "child-1" );
		VersionedEntity p = new VersionedEntity( "root", "root" );
		p.getChildren().add( c );
		c.setParent( p );
		s.save( p );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		clearCounts();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		VersionedEntity loadedParent = ( VersionedEntity ) s.get( VersionedEntity.class, "root" );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		loadedParent = ( VersionedEntity ) getOldToNewEntityRefMap().get( loadedParent );
		s.delete( loadedParent );
		applyNonFlushedChangesToNewSessionCloseOldSession( s );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertInsertCount( 0 );
		assertUpdateCount( 0 );
		assertDeleteCount( 2 );
	}

	@Test
	public void testNoUpdateOnDelete() throws Exception {
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		Node node = new Node( "test" );
		s.persist( node );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		clearCounts();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		s.delete( node );
		applyNonFlushedChangesToNewSessionCloseOldSession( s );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertUpdateCount( 0 );
		assertInsertCount( 0 );
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testNoUpdateOnDeleteWithCollection() throws Exception {
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		Node parent = new Node( "parent" );
		Node child = new Node( "child" );
		parent.getCascadingChildren().add( child );
		s.persist( parent );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		clearCounts();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		parent = ( Node ) s.get( Node.class, "parent" );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		parent = ( Node ) getOldToNewEntityRefMap().get( parent );
		s.delete( parent );
		applyNonFlushedChangesToNewSessionCloseOldSession( s );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertUpdateCount( 0 );
		assertInsertCount( 0 );
		assertDeleteCount( 2 );
	}
}
