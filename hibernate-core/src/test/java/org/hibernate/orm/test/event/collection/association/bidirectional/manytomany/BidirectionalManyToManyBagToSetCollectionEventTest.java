/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.event.collection.association.bidirectional.manytomany;

import java.util.ArrayList;
import java.util.Collection;

import org.hibernate.orm.test.event.collection.ParentWithCollection;
import org.hibernate.orm.test.event.collection.association.AbstractAssociationCollectionEventTest;
import org.hibernate.testing.orm.junit.DomainModel;

/**
 *
 * @author Gail Badner
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/event/collection/association/bidirectional/manytomany/BidirectionalManyToManyBagToSetMapping.hbm.xml")
public class BidirectionalManyToManyBagToSetCollectionEventTest extends AbstractAssociationCollectionEventTest {

	@Override
	public ParentWithCollection createParent(String name) {
		return new ParentWithBidirectionalManyToMany( name );
	}

	@Override
	public Collection createCollection() {
		return new ArrayList();
	}
}
