/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.event.collection.association.unidirectional.manytomany;

import java.util.ArrayList;
import java.util.Collection;

import org.hibernate.orm.test.event.collection.Child;
import org.hibernate.orm.test.event.collection.ChildEntity;
import org.hibernate.orm.test.event.collection.ParentWithCollection;
import org.hibernate.orm.test.event.collection.association.AbstractAssociationCollectionEventTest;
import org.hibernate.orm.test.event.collection.association.unidirectional.ParentWithCollectionOfEntities;

/**
 * @author Gail Badner
 */
public class UnidirectionalManyToManyBagCollectionEventTest extends AbstractAssociationCollectionEventTest {
	@Override
	public String[] getMappings() {
		return new String[] { "event/collection/association/unidirectional/manytomany/UnidirectionalManyToManyBagMapping.hbm.xml" };
	}

	@Override
	public ParentWithCollection createParent(String name) {
		return new ParentWithCollectionOfEntities( name );
	}

	@Override
	public Collection createCollection() {
		return new ArrayList();
	}

	public Child createChild(String name) {
		return new ChildEntity( name );
	}
}
