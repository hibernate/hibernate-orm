/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: $

package org.hibernate.test.event.collection.association.bidirectional.onetomany;



/**
 *
 * @author Gail Badner
 */
public class ParentWithBidirectionalOneToManySubclass extends ParentWithBidirectionalOneToMany {
	public ParentWithBidirectionalOneToManySubclass() {
	}

	public ParentWithBidirectionalOneToManySubclass(String name) {
		super( name );
	}

}
