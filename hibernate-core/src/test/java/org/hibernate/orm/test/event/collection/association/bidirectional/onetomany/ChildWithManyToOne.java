/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.event.collection.association.bidirectional.onetomany;
import org.hibernate.orm.test.event.collection.ChildEntity;
import org.hibernate.orm.test.event.collection.ParentWithCollection;

/**
 * @author Gail Badner
 */
public class ChildWithManyToOne extends ChildEntity {
	private ParentWithCollection parent;

	public ChildWithManyToOne() {
	}

	public ChildWithManyToOne(String name) {
		super( name );
	}

	public ParentWithCollection getParent() {
		return parent;
	}

	public void setParent(ParentWithCollection parent) {
		this.parent = parent;
	}
}
