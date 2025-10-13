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
import org.hibernate.testing.orm.junit.DomainModel;

/**
 * @author Gail Badner
 */
@DomainModel(xmlMappings = "org/hibernate/orm/test/event/collection/association/unidirectional/onetomany/UnidirectionalOneToManySetMapping.hbm.xml")
public class UnidirectionalOneToManySetCollectionEventTest extends AbstractAssociationCollectionEventTest {

	@Override
	public ParentWithCollection createParent(String name) {
		return new ParentWithCollectionOfEntities( name );
	}

	@Override
	public Collection createCollection() {
		return new HashSet();
	}
}
