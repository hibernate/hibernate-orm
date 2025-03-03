/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.event.collection.association.bidirectional.manytomany;
import java.util.Collection;

import org.hibernate.orm.test.event.collection.ChildEntity;

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
