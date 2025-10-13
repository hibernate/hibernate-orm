/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.event.collection.association.bidirectional.onetomany;

import org.hibernate.orm.test.event.collection.ParentWithCollection;
import org.hibernate.testing.orm.junit.DomainModel;

/**
 * @author Gail Badner
 */
@DomainModel(xmlMappings = "org/hibernate/orm/test/event/collection/association/bidirectional/onetomany/BidirectionalOneToManyBagSubclassMapping.hbm.xml")
public class BidirectionalOneToManyBagSubclassCollectionEventTest extends BidirectionalOneToManyBagCollectionEventTest {

	@Override
	public ParentWithCollection createParent(String name) {
		return new ParentWithBidirectionalOneToManySubclass( name );
	}
}
