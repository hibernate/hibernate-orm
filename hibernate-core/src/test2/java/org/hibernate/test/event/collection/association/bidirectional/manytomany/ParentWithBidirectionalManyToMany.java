/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: $

package org.hibernate.test.event.collection.association.bidirectional.manytomany;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.hibernate.test.event.collection.AbstractParentWithCollection;
import org.hibernate.test.event.collection.Child;

/**
 *
 * @author Gail Badner
 */
public class ParentWithBidirectionalManyToMany extends AbstractParentWithCollection {
	public ParentWithBidirectionalManyToMany() {
	}

	public ParentWithBidirectionalManyToMany(String name) {
		super( name );
	}

	public void newChildren(Collection children) {
		if ( children == getChildren() ) {
			return;
		}
		if ( getChildren() != null ) {
			for ( Iterator it = getChildren().iterator(); it.hasNext(); ) {
				ChildWithBidirectionalManyToMany child = ( ChildWithBidirectionalManyToMany ) it.next();
				child.removeParent( this );
			}
		}
		if ( children != null ) {
			for ( Iterator it = children.iterator(); it.hasNext(); ) {
				ChildWithBidirectionalManyToMany child = ( ChildWithBidirectionalManyToMany ) it.next();
				child.addParent( this );
			}
		}
		super.newChildren( children );
	}

	public Child createChild(String name) {
		return new ChildWithBidirectionalManyToMany( name, new HashSet() );
	}

	public void addChild(Child child) {
		super.addChild( child );
		( ( ChildWithBidirectionalManyToMany ) child ).addParent( this );
	}

	public void addAllChildren(Collection children) {
		super.addAllChildren( children );
		for ( Iterator it = children.iterator(); it.hasNext(); ) {
			ChildWithBidirectionalManyToMany child = ( ChildWithBidirectionalManyToMany ) it.next();
			child.addParent( this );
		}
	}

	public void removeChild(Child child) {
		// Note: if the collection is a bag, the same child can be in the collection more than once
		super.removeChild( child );
		// only remove the parent from the child's set if child is no longer in the collection
		if ( ! getChildren().contains( child ) ) {
			( ( ChildWithBidirectionalManyToMany ) child ).removeParent( this );
		}
	}

	public void removeAllChildren(Collection children) {
		super.removeAllChildren( children );
		for ( Iterator it = children.iterator(); it.hasNext(); ) {
			ChildWithBidirectionalManyToMany child = ( ChildWithBidirectionalManyToMany ) it.next();
			child.removeParent( this );
		}
	}

	public void clearChildren() {
		if ( getChildren() != null ) {
			for ( Iterator it = getChildren().iterator(); it.hasNext(); ) {
				ChildWithBidirectionalManyToMany child = ( ChildWithBidirectionalManyToMany ) it.next();
				child.removeParent( this );
			}
		}
		super.clearChildren();
	}
}
