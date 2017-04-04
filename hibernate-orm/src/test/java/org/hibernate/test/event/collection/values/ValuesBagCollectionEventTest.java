/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.event.collection.values;

import java.util.ArrayList;
import java.util.Collection;

import org.hibernate.test.event.collection.AbstractCollectionEventTest;
import org.hibernate.test.event.collection.ParentWithCollection;

/**
 *
 * @author Gail Badner
 */
public class ValuesBagCollectionEventTest extends AbstractCollectionEventTest {
	@Override
	public String[] getMappings() {
		return new String[] { "event/collection/values/ValuesBagMapping.hbm.xml" };
	}

	@Override
	public ParentWithCollection createParent(String name) {
		return new ParentWithCollectionOfValues( name );
	}

	@Override
	public Collection createCollection() {
		return new ArrayList();
	}
}
