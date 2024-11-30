/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.event.collection.association.unidirectional;
import org.hibernate.orm.test.event.collection.AbstractParentWithCollection;
import org.hibernate.orm.test.event.collection.Child;
import org.hibernate.orm.test.event.collection.ChildEntity;

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
