/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.event.collection.association.bidirectional.onetomany;

import org.hibernate.orm.test.event.collection.ParentWithCollection;

/**
 * @author Gail Badner
 */
public class BidirectionalOneToManyBagSubclassCollectionEventTest extends BidirectionalOneToManyBagCollectionEventTest {
	@Override
	public String[] getMappings() {
		return new String[] { "event/collection/association/bidirectional/onetomany/BidirectionalOneToManyBagSubclassMapping.hbm.xml" };
	}

	@Override
	public ParentWithCollection createParent(String name) {
		return new ParentWithBidirectionalOneToManySubclass( name );
	}
}
