/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.event.collection.association.bidirectional.onetomany;
import java.util.Collection;
import java.util.Iterator;

import org.hibernate.orm.test.event.collection.AbstractParentWithCollection;
import org.hibernate.orm.test.event.collection.Child;

/**
 *
 * @author Gail Badner
 */
public class ParentWithBidirectionalOneToMany extends AbstractParentWithCollection {
	public ParentWithBidirectionalOneToMany() {
	}

	public ParentWithBidirectionalOneToMany(String name) {
		super( name );
	}

	public Child createChild(String name ) {
		return new ChildWithManyToOne( name );
	}

	public Child addChild(String name) {
		Child child = createChild( name );
		addChild( child );
		return child;
	}

	public void addChild(Child child) {
		super.addChild( child );
		( ( ChildWithManyToOne ) child ).setParent( this );
	}

	public void newChildren(Collection children) {
		if ( children == getChildren() ) {
			return;
		}
		if ( getChildren() != null ) {
			for ( Iterator it = getChildren().iterator(); it.hasNext(); ) {
				ChildWithManyToOne child =  ( ChildWithManyToOne ) it.next();
				child.setParent( null );
			}
		}
		if ( children != null ) {
			for ( Iterator it = children.iterator(); it.hasNext(); ) {
				ChildWithManyToOne child = ( ChildWithManyToOne ) it.next();
				child.setParent( this );
			}
		}
		super.newChildren( children );
	}

	public void removeChild(Child child) {
		// Note: there can be more than one child in the collection
		super.removeChild( child );
		// only set the parent to null if child is no longer in the bag
		if ( ! getChildren().contains( child ) ) {
			( ( ChildWithManyToOne ) child ).setParent( null );
		}
	}

}
