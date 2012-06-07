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

import org.hibernate.test.event.collection.ChildEntity;

/**
 *
 * @author Gail Badner
 */
public class ChildWithBidirectionalManyToMany extends ChildEntity {
	private Collection parents;

	public ChildWithBidirectionalManyToMany() {
	}

	public ChildWithBidirectionalManyToMany(String name, Collection parents) {
		super( name );
		this.parents = parents;
	}

	public Collection getParents() {
		return parents;
	}

	public void setParents(Collection parents) {
		this.parents = parents;
	}

	public void addParent(ParentWithBidirectionalManyToMany parent) {
		if ( parent != null ) {
			parents.add( parent );
		}
	}

	public void removeParent(ParentWithBidirectionalManyToMany parent) {
		if ( parent != null ) {
			parents.remove( parent );
		}
	}
}
