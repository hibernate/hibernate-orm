/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: $

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
