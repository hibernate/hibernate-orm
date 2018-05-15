/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: $

package org.hibernate.test.event.collection.values;
import org.hibernate.test.event.collection.AbstractParentWithCollection;
import org.hibernate.test.event.collection.Child;
import org.hibernate.test.event.collection.ChildValue;

/**
 *
 * @author Gail Badner
 */
public class ParentWithCollectionOfValues extends AbstractParentWithCollection {
	public ParentWithCollectionOfValues() {
	}

	public ParentWithCollectionOfValues(String name) {
		super( name );
	}
	
	public Child createChild(String name) {
		return new ChildValue( name );
	}
}
