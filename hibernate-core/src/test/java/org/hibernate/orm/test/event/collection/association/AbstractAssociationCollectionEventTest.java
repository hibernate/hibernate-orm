/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.event.collection.association;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.dialect.HANADialect;
import org.hibernate.orm.test.event.collection.AbstractCollectionEventTest;
import org.hibernate.orm.test.event.collection.ChildEntity;
import org.hibernate.orm.test.event.collection.CollectionListeners;
import org.hibernate.orm.test.event.collection.ParentWithCollection;
import org.hibernate.orm.test.event.collection.association.bidirectional.manytomany.ChildWithBidirectionalManyToMany;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

/**
 * @author Gail Badner
 */
public abstract class AbstractAssociationCollectionEventTest extends AbstractCollectionEventTest {
	@Test
	@SkipForDialect(dialectClass = HANADialect.class,
			reason = " HANA doesn't support tables consisting of only a single auto-generated column")
	public void testDeleteParentButNotChild(SessionFactoryScope scope) {
		CollectionListeners listeners = new CollectionListeners( scope.getSessionFactory() );
		ParentWithCollection parent = createParentWithOneChild( "parent", "child", scope );
		ChildEntity child = (ChildEntity) parent.getChildren().iterator().next();
		listeners.clear();
		Session s = scope.getSessionFactory().openSession();
		Transaction tx = s.beginTransaction();
		parent = (ParentWithCollection) s.get( parent.getClass(), parent.getId() );
		child = (ChildEntity) s.get( child.getClass(), child.getId() );
		parent.removeChild( child );
		s.remove( parent );
		tx.commit();
		s.close();
		int index = 0;
		checkResult( listeners, listeners.getInitializeCollectionListener(), parent, index++ );
		if ( child instanceof ChildWithBidirectionalManyToMany ) {
			checkResult( listeners, listeners.getInitializeCollectionListener(),
					(ChildWithBidirectionalManyToMany) child, index++ );
		}
		checkResult( listeners, listeners.getPreCollectionRemoveListener(), parent, index++ );
		checkResult( listeners, listeners.getPostCollectionRemoveListener(), parent, index++ );
		if ( child instanceof ChildWithBidirectionalManyToMany ) {
			checkResult( listeners, listeners.getPreCollectionUpdateListener(),
					(ChildWithBidirectionalManyToMany) child, index++ );
			checkResult( listeners, listeners.getPostCollectionUpdateListener(),
					(ChildWithBidirectionalManyToMany) child, index++ );
		}
		checkNumberOfResults( listeners, index );
	}
}
