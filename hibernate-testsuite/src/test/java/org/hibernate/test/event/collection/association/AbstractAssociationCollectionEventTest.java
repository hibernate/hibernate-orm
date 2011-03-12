//$Id: $
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution statements
 * applied by the authors.
 *
 * All third-party contributions are distributed under license by Red Hat
 * Middleware LLC.  This copyrighted material is made available to anyone
 * wishing to use, modify, copy, or redistribute it subject to the terms
 * and conditions of the GNU Lesser General Public License, as published by
 * the Free Software Foundation.  This program is distributed in the hope
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the GNU Lesser General Public License for more details.  You should
 * have received a copy of the GNU Lesser General Public License along with
 * this distribution; if not, write to: Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor Boston, MA  02110-1301  USA
 */
package org.hibernate.test.event.collection.association;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.event.collection.AbstractCollectionEventTest;
import org.hibernate.test.event.collection.ChildEntity;
import org.hibernate.test.event.collection.CollectionListeners;
import org.hibernate.test.event.collection.ParentWithCollection;
import org.hibernate.test.event.collection.association.bidirectional.manytomany.ChildWithBidirectionalManyToMany;

/**
 *
 * @author Gail Badner
 */
public abstract class AbstractAssociationCollectionEventTest extends AbstractCollectionEventTest {
	public AbstractAssociationCollectionEventTest(String string) {
		super( string );
	}

	public void testDeleteParentButNotChild() {
		CollectionListeners listeners = new CollectionListeners( getSessions() );
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
