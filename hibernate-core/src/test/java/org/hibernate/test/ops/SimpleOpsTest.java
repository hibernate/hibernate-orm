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

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.metamodel.MetadataSources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
public class SimpleOpsTest extends AbstractOperationTestCase {
	public String[] getMappings() {
		return new String[] { "ops/SimpleEntity.hbm.xml" };
	}

	@Test
	public void testBasicOperations() {
		clearCounts();

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		SimpleEntity entity = new SimpleEntity(  );
		entity.setId( 1L );
		entity.setName( "name" );
		s.save( entity );
		tx.commit();
		s.close();

		assertInsertCount( 1 );
		assertUpdateCount( 0 );
		assertDeleteCount( 0 );

		clearCounts();

		s = openSession();
		tx = s.beginTransaction();
		entity = ( SimpleEntity ) s.get( SimpleEntity.class, entity.getId() );
		assertEquals( Long.valueOf( 1L ), entity.getId() );
		assertEquals( "name", entity.getName() );
		entity.setName( "new name" );
		tx.commit();
		s.close();

		assertInsertCount( 0 );
		assertUpdateCount( 1 );
		assertDeleteCount( 0 );

		clearCounts();

		s = openSession();
		tx = s.beginTransaction();
		entity = ( SimpleEntity ) s.load( SimpleEntity.class, entity.getId() );
		assertFalse( Hibernate.isInitialized( entity ) );
		assertEquals( Long.valueOf( 1L ), entity.getId() );
		assertEquals( "new name", entity.getName() );
		assertTrue( Hibernate.isInitialized( entity ) );
		tx.commit();
		s.close();

		assertInsertCount( 0 );
		assertUpdateCount( 0 );
		assertDeleteCount( 0 );

		entity.setName( "another new name" );

		s = openSession();
		tx = s.beginTransaction();
		s.merge( entity );
		tx.commit();
		s.close();

		assertInsertCount( 0 );
		assertUpdateCount( 1 );
		assertDeleteCount( 0 );

		clearCounts();

		s = openSession();
		tx = s.beginTransaction();
		entity = ( SimpleEntity ) s.get( SimpleEntity.class, entity.getId() );
		assertEquals( Long.valueOf( 1L ), entity.getId() );
		assertEquals( "another new name", entity.getName() );
		s.delete( entity );
		tx.commit();
		s.close();

		assertInsertCount( 0 );
		assertUpdateCount( 0 );
		assertDeleteCount( 1 );
	}
}

