/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.annotations.manytomany.associationtable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.Test;

import org.hibernate.JDBCException;
import org.hibernate.Session;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Gail Badner (extracted from ManyToManyTest authored by Emmanuel Bernard)
 */
@FailureExpectedWithNewMetamodel
public class ManyToManyAssociationTableTest extends BaseCoreFunctionalTestCase {

	@Test
	public void testAssociationTableUniqueConstraints() throws Exception {
		Session s = openSession();
		Permission readAccess = new Permission();
		readAccess.setPermission( "read" );
		readAccess.setExpirationDate( new Date() );
		Collection<Permission> coll = new ArrayList<Permission>( 2 );
		coll.add( readAccess );
		coll.add( readAccess );
		Group group = new Group();
		group.setId( new Integer( 1 ) );
		group.setPermissions( coll );
		s.getTransaction().begin();
		try {
			s.persist( group );
			s.getTransaction().commit();
			fail( "Unique constraints not applied on association table" );
		}
		catch (JDBCException e) {
			//success
			s.getTransaction().rollback();
		}
		finally {
			s.close();
		}
	}

	@Test
	public void testAssociationTableAndOrderBy() throws Exception {
		Session s = openSession();
		s.enableFilter( "Groupfilter" );
		Permission readAccess = new Permission();
		readAccess.setPermission( "read" );
		readAccess.setExpirationDate( new Date() );
		Permission writeAccess = new Permission();
		writeAccess.setPermission( "write" );
		writeAccess.setExpirationDate( new Date( new Date().getTime() - 10*60*1000 ) );
		Collection<Permission> coll = new ArrayList<Permission>( 2 );
		coll.add( readAccess );
		coll.add( writeAccess );
		Group group = new Group();
		group.setId( new Integer( 1 ) );
		group.setPermissions( coll );
		s.getTransaction().begin();
		s.persist( group );
		s.flush();
		s.clear();
		group = (Group) s.get( Group.class, group.getId() );
		s.createQuery( "select g from Group g join fetch g.permissions").list();
		assertEquals( "write", group.getPermissions().iterator().next().getPermission() );
		s.getTransaction().rollback();
		s.close();
	}

	@Test
	public void testAssociationTableAndOrderByWithSet() throws Exception {
		Session s = openSession();
		s.enableFilter( "Groupfilter" );

		Permission readAccess = new Permission();
		readAccess.setPermission( "read" );
		readAccess.setExpirationDate( new Date() );

		Permission writeAccess = new Permission();
		writeAccess.setPermission( "write" );
		writeAccess.setExpirationDate( new Date( new Date().getTime() - 10*60*1000 ) );

		Permission executeAccess = new Permission();
		executeAccess.setPermission( "execute" );
		executeAccess.setExpirationDate( new Date( new Date().getTime() - 5*60*1000 ) );

		Set<Permission> coll = new HashSet<Permission>( 3 );
		coll.add( readAccess );
		coll.add( writeAccess );
		coll.add( executeAccess );

		GroupWithSet group = new GroupWithSet();
		group.setId( new Integer( 1 ) );
		group.setPermissions( coll );
		s.getTransaction().begin();
		s.persist( group );
		s.flush();
		s.clear();

		group = (GroupWithSet) s.get( GroupWithSet.class, group.getId() );
		s.createQuery( "select g from Group g join fetch g.permissions").list();
		Iterator<Permission> permIter = group.getPermissions().iterator();
		assertEquals( "write", permIter.next().getPermission() );
		assertEquals( "execute", permIter.next().getPermission() );
		assertEquals( "read", permIter.next().getPermission() );
		s.getTransaction().rollback();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Group.class,
				GroupWithSet.class,
				Permission.class
		};
	}

}
