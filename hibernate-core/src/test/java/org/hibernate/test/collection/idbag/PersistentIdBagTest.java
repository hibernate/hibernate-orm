/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collection.idbag;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;

import org.hibernate.Session;
import org.hibernate.collection.internal.PersistentIdentifierBag;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests related to operations on a PersistentIdentifierBag
 *
 * @author Daniel Strobusch
 * @author Steve Ebersole
 */
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

		ArrayList<IdbagOwner> otherCollection = new ArrayList<>();
		otherCollection.add( child );
		assertFalse( children.retainAll( otherCollection ) );
		assertFalse( children.isDirty() );

		otherCollection = new ArrayList<>();
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

	@Test
	@TestForIssue(jiraKey = "HHH-10875")
	public void testDeleteViaIterator() {
		IdbagOwner parent = createParentWithThreeChildren();

		Session session = openSession();
		session.beginTransaction();
		session.save( parent );
		session.flush();
		// at this point, the list on parent has now been replaced with a PersistentBag...
		PersistentIdentifierBag children = ( PersistentIdentifierBag ) parent.getChildren();

		// Note that the following works: children.remove(c1);
		// Deleting the same item via the iterator must work as well.
        // As long as PersistentIdentifierBag#beforeRemove is not called on deletion, the wrong child is deleted and
		// all remaining children get updated. As long as there is no unique constraint, this is just a performance
		// issue. However, since there is a unique constraint on (PARENT_FK, CHILD_FK) the update cascade fails,
		// since the intermediate state is not valid.
		Iterator iterator = children.iterator();
		iterator.next();
		iterator.remove();

        // flush session to make sure that the constraint is not violated
		session.flush();

		session.getTransaction().rollback();
		session.close();

	}

	@Test
	@TestForIssue(jiraKey = "HHH-10875")
	public void testDeleteViaListIteratorNext() {
		IdbagOwner parent = createParentWithThreeChildren();

		Session session = openSession();
		session.beginTransaction();
		session.save( parent );
		session.flush();
		// at this point, the list on parent has now been replaced with a PersistentBag...
		PersistentIdentifierBag children = ( PersistentIdentifierBag ) parent.getChildren();

        // Make sure PersistentIdentifierBag#beforeRemove is not called on deletion.
		ListIterator iterator = children.listIterator();
		iterator.next();
		iterator.remove();

		// flush session to make sure that the constraint is not violated
		session.flush();

		session.getTransaction().rollback();
		session.close();

	}

	@Test
	@TestForIssue(jiraKey = "HHH-10875")
	public void testDeleteViaListIteratorPrevious() {
		IdbagOwner parent = createParentWithThreeChildren();

		Session session = openSession();
		session.beginTransaction();
		session.save( parent );
		session.flush();
		// at this point, the list on parent has now been replaced with a PersistentBag...
		PersistentIdentifierBag children = ( PersistentIdentifierBag ) parent.getChildren();

		// Make sure PersistentIdentifierBag#beforeRemove is not called on deletion.
		ListIterator iterator = children.listIterator();
		iterator.next();
		iterator.previous();
		iterator.remove();

		// flush session to make sure that the constraint is not violated
		session.flush();

		session.getTransaction().rollback();
		session.close();

	}

	private IdbagOwner createParentWithThreeChildren() {
		IdbagOwner parent = new IdbagOwner( "root" );
		IdbagOwner c1 = new IdbagOwner( "c1" );
		parent.getChildren().add( c1 );
		IdbagOwner c2 = new IdbagOwner( "c2" );
		parent.getChildren().add( c2 );
		IdbagOwner c3 = new IdbagOwner( "c3" );
		parent.getChildren().add( c3 );
		return parent;
	}
}
