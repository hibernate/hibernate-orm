/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.annotations.manytomany;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Andrea Boriero
 */
@Disabled("Filtesr not yet implemented")
public class ManyToManyWithFiltersTest extends SessionFactoryBasedFunctionalTest {

	@Test
	public void testAssociationTableUniqueConstraints() {
		inTransaction(
				session -> {
					Permission readAccess = new Permission();
					readAccess.setPermission( "read" );
					readAccess.setExpirationDate( new Date() );
					Collection<Permission> coll = new ArrayList<>( 2 );
					coll.add( readAccess );
					coll.add( readAccess );
					Group group = new Group();
					group.setId( new Integer( 1 ) );
					group.setPermissions( coll );
					session.persist( group );
					session.getTransaction().commit();
					fail( "Unique constraints not applied on association table" );
				}
		);
	}

	@Test
	public void testAssociationTableAndOrderBy() {
		inTransaction(
				session -> {
					session.enableFilter( "Groupfilter" );

					Permission readAccess = new Permission();
					readAccess.setPermission( "read" );
					readAccess.setExpirationDate( new Date() );

					Permission writeAccess = new Permission();
					writeAccess.setPermission( "write" );
					writeAccess.setExpirationDate( new Date( new Date().getTime() - 10 * 60 * 1000 ) );

					Collection<Permission> coll = new ArrayList<>( 2 );
					coll.add( readAccess );
					coll.add( writeAccess );

					Group group = new Group();
					group.setId( new Integer( 1 ) );
					group.setPermissions( coll );

					session.persist( group );
					session.flush();
					session.clear();
					group = session.get( Group.class, group.getId() );
					session.createQuery( "select g from Group g join fetch g.permissions" ).list();
					assertEquals( "write", group.getPermissions().iterator().next().getPermission() );
				}
		);
	}

	@Test
	public void testAssociationTableAndOrderByWithSet() {
		inTransaction(
				session -> {
					session.enableFilter( "Groupfilter" );

					Permission readAccess = new Permission();
					readAccess.setPermission( "read" );
					readAccess.setExpirationDate( new Date() );

					Permission writeAccess = new Permission();
					writeAccess.setPermission( "write" );
					writeAccess.setExpirationDate( new Date( new Date().getTime() - 10 * 60 * 1000 ) );

					Permission executeAccess = new Permission();
					executeAccess.setPermission( "execute" );
					executeAccess.setExpirationDate( new Date( new Date().getTime() - 5 * 60 * 1000 ) );

					Set<Permission> coll = new HashSet<>( 3 );
					coll.add( readAccess );
					coll.add( writeAccess );
					coll.add( executeAccess );

					GroupWithSet group = new GroupWithSet();
					group.setId( new Integer( 1 ) );
					group.setPermissions( coll );
					session.persist( group );
					session.flush();
					session.clear();

					group = session.get( GroupWithSet.class, group.getId() );
					session.createQuery( "select g from Group g join fetch g.permissions" ).list();
					Iterator<Permission> permIter = group.getPermissions().iterator();
					assertEquals( "write", permIter.next().getPermission() );
					assertEquals( "execute", permIter.next().getPermission() );
					assertEquals( "read", permIter.next().getPermission() );
				}
		);
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return false;
	}

	@Override
	protected void cleanupTestData() {
		inTransaction(
				session -> {
					List<Group> groups = session.createQuery( "from Group" ).list();
					groups.forEach( group -> {
						group.getPermissions().forEach( permission -> session.delete( permission ) );
						session.delete( group );
					} );

					List<GroupWithSet> groupWithSetss = session.createQuery( "from GroupWithSet" ).list();
					groupWithSetss.forEach( group -> {
						group.getPermissions().forEach( permission -> session.delete( permission ) );
						session.delete( group );
					} );
				}
		);
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Group.class,
				Permission.class,
				GroupWithSet.class
		};
	}
}
