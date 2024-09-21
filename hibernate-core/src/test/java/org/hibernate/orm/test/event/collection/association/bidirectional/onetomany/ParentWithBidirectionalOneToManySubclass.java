/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.event.collection.association.bidirectional.onetomany;



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
