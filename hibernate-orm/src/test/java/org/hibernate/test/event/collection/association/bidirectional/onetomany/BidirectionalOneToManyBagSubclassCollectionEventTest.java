/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.event.collection.association.bidirectional.onetomany;

import org.hibernate.test.event.collection.ParentWithCollection;

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
