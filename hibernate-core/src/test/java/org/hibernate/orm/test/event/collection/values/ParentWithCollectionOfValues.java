/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.event.collection.values;
import org.hibernate.orm.test.event.collection.AbstractParentWithCollection;
import org.hibernate.orm.test.event.collection.Child;
import org.hibernate.orm.test.event.collection.ChildValue;

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
