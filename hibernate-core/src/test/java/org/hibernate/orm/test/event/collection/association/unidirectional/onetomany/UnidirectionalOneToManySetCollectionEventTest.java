/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.event.collection.association.unidirectional.onetomany;

import java.util.Collection;
import java.util.HashSet;

import org.hibernate.orm.test.event.collection.ParentWithCollection;
import org.hibernate.orm.test.event.collection.association.AbstractAssociationCollectionEventTest;
import org.hibernate.orm.test.event.collection.association.unidirectional.ParentWithCollectionOfEntities;

/**
 * @author Gail Badner
 */
public class UnidirectionalOneToManySetCollectionEventTest extends AbstractAssociationCollectionEventTest {
	@Override
	public String[] getMappings() {
		return new String[] { "event/collection/association/unidirectional/onetomany/UnidirectionalOneToManySetMapping.hbm.xml" };
	}

	@Override
	public ParentWithCollection createParent(String name) {
		return new ParentWithCollectionOfEntities( name );
	}

	@Override
	public Collection createCollection() {
		return new HashSet();
	}
}
