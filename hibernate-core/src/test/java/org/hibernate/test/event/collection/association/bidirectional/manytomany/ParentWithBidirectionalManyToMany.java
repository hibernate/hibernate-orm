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
