/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.event.collection.association.bidirectional.manytomany;
import java.util.ArrayList;
import java.util.Collection;

import org.hibernate.test.event.collection.ParentWithCollection;
import org.hibernate.test.event.collection.association.AbstractAssociationCollectionEventTest;

/**
 *
 * @author Gail Badner
 */
public class BidirectionalManyToManyBagToSetCollectionEventTest extends AbstractAssociationCollectionEventTest {
	@Override
	public String[] getMappings() {
		return new String[] { "event/collection/association/bidirectional/manytomany/BidirectionalManyToManyBagToSetMapping.hbm.xml" };
	}

	@Override
	public ParentWithCollection createParent(String name) {
		return new ParentWithBidirectionalManyToMany( name );
	}

	@Override
	public Collection createCollection() {
		return new ArrayList();
	}
}
