/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.instrument.cases;
import junit.framework.Assert;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.instrument.domain.Entity;

/**
 *
 * @author Steve Ebersole
 */
public class TestManyToOneProxyExecutable extends AbstractExecutable {
	public void execute() {
		Session s = getFactory().openSession();
		Transaction t = s.beginTransaction();
		Entity root = new Entity( "root" );
		Entity child1 = new Entity( "child1" );
		Entity child2 = new Entity( "child2" );
		root.setChild( child1 );
		child1.setSibling( child2 );
		Entity gChild1 = new Entity( "grandchild 1" );
		Entity gChild2 = new Entity( "grandchild 2" );
		child1.setChild( gChild1 );
		gChild1.setSibling( gChild2 );
		s.save( root );
		t.commit();
		s.close();

		// NOTE : child is mapped with lazy="proxy"; sibling with lazy="no-proxy"...

		s = getFactory().openSession();
		t = s.beginTransaction();
		// load root
		root = ( Entity ) s.get( Entity.class, root.getId() );
		Assert.assertFalse( Hibernate.isPropertyInitialized( root, "name" ) );
		Assert.assertFalse( Hibernate.isPropertyInitialized( root, "sibling" ) );
		Assert.assertTrue( Hibernate.isPropertyInitialized( root, "child" ) );

		// get a handle to the child1 proxy reference (and make certain that
		// this does not force the lazy properties of the root entity
		// to get initialized.
		child1 = root.getChild();
		Assert.assertFalse( Hibernate.isInitialized( child1 ) );
		Assert.assertFalse( Hibernate.isPropertyInitialized( root, "name" ) );
		Assert.assertFalse( Hibernate.isPropertyInitialized( root, "sibling" ) );
		Assert.assertFalse( Hibernate.isPropertyInitialized( child1, "name" ) );
		Assert.assertFalse( Hibernate.isPropertyInitialized( child1, "sibling" ) );
		Assert.assertFalse( Hibernate.isPropertyInitialized( child1, "child" ) );

		child1.getName();
		Assert.assertFalse( Hibernate.isPropertyInitialized( root, "name" ) );
		Assert.assertFalse( Hibernate.isPropertyInitialized( root, "sibling" ) );
		Assert.assertTrue( Hibernate.isPropertyInitialized( child1, "name" ) );
		Assert.assertTrue( Hibernate.isPropertyInitialized( child1, "sibling" ) );
		Assert.assertTrue( Hibernate.isPropertyInitialized( child1, "child" ) );

		gChild1 = child1.getChild();
		Assert.assertFalse( Hibernate.isInitialized( gChild1 ) );
		Assert.assertFalse( Hibernate.isPropertyInitialized( root, "name" ) );
		Assert.assertFalse( Hibernate.isPropertyInitialized( root, "sibling" ) );

		s.delete( root );
		t.commit();
		s.close();
	}
}
