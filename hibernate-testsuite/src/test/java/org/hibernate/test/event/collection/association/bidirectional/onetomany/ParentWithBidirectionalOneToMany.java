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
package org.hibernate.test.event.collection.association.bidirectional.onetomany;

import java.util.Collection;
import java.util.Iterator;

import org.hibernate.test.event.collection.Child;
import org.hibernate.test.event.collection.AbstractParentWithCollection;

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

	public Child createChild( String name ) {
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
