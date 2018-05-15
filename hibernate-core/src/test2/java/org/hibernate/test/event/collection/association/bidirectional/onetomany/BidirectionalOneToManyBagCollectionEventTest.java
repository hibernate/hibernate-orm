/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.event.collection.association.bidirectional.onetomany;
import java.util.ArrayList;
import java.util.Collection;

import org.hibernate.test.event.collection.Child;
import org.hibernate.test.event.collection.ParentWithCollection;
import org.hibernate.test.event.collection.association.AbstractAssociationCollectionEventTest;

/**
 *
 * @author Gail Badner
 */
public class BidirectionalOneToManyBagCollectionEventTest extends AbstractAssociationCollectionEventTest {
	@Override
	public String[] getMappings() {
		return new String[] { "event/collection/association/bidirectional/onetomany/BidirectionalOneToManyBagMapping.hbm.xml" };
	}

	@Override
	public ParentWithCollection createParent(String name) {
		return new ParentWithBidirectionalOneToMany( name );
	}

	@Override
	public Collection createCollection() {
		return new ArrayList();
	}

	public Child createChild(String name) {
		return new ChildWithManyToOne( name );
	}
}
