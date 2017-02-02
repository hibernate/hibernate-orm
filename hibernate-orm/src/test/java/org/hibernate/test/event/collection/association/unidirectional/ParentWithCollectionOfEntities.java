/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: $

package org.hibernate.test.event.collection.association.unidirectional;
import org.hibernate.test.event.collection.AbstractParentWithCollection;
import org.hibernate.test.event.collection.Child;
import org.hibernate.test.event.collection.ChildEntity;

/**
 * @author Gail Badner
 */
public class ParentWithCollectionOfEntities extends AbstractParentWithCollection {

	public ParentWithCollectionOfEntities() {
	}

	public ParentWithCollectionOfEntities(String name) {
		super( name );
	}

	public Child createChild(String name) {
		return new ChildEntity( name );
	}
}
