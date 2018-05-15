/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.event.collection.association;
import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.dialect.AbstractHANADialect;
import org.hibernate.test.event.collection.AbstractCollectionEventTest;
import org.hibernate.test.event.collection.ChildEntity;
import org.hibernate.test.event.collection.CollectionListeners;
import org.hibernate.test.event.collection.ParentWithCollection;
import org.hibernate.test.event.collection.association.bidirectional.manytomany.ChildWithBidirectionalManyToMany;
import org.hibernate.testing.SkipForDialect;

/**
 * @author Gail Badner
 */
public abstract class AbstractAssociationCollectionEventTest extends AbstractCollectionEventTest {
	@Test
	@SkipForDialect(value = AbstractHANADialect.class, comment = " HANA doesn't support tables consisting of only a single auto-generated column")
	public void testDeleteParentButNotChild() {
		CollectionListeners listeners = new CollectionListeners( sessionFactory() );
		ParentWithCollection parent = createParentWithOneChild( "parent", "child" );
		ChildEntity child = ( ChildEntity ) parent.getChildren().iterator().next();
		listeners.clear();
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		parent = ( ParentWithCollection ) s.get( parent.getClass(), parent.getId() );
		child = ( ChildEntity ) s.get( child.getClass(), child.getId() );
		parent.removeChild( child );
		s.delete( parent );
		tx.commit();
		s.close();
		int index = 0;
		checkResult( listeners, listeners.getInitializeCollectionListener(), parent, index++ );
		if ( child instanceof ChildWithBidirectionalManyToMany ) {
			checkResult( listeners, listeners.getInitializeCollectionListener(), ( ChildWithBidirectionalManyToMany ) child, index++ );
		}
		checkResult( listeners, listeners.getPreCollectionRemoveListener(), parent, index++ );
		checkResult( listeners, listeners.getPostCollectionRemoveListener(), parent, index++ );
		if ( child instanceof ChildWithBidirectionalManyToMany ) {
			checkResult( listeners, listeners.getPreCollectionUpdateListener(), ( ChildWithBidirectionalManyToMany ) child, index++ );
			checkResult( listeners, listeners.getPostCollectionUpdateListener(), ( ChildWithBidirectionalManyToMany ) child, index++ );
		}
		checkNumberOfResults( listeners, index );
	}
}
