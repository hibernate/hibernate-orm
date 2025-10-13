/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.event.collection.values;

import java.util.ArrayList;
import java.util.Collection;

import org.hibernate.orm.test.event.collection.AbstractCollectionEventTest;
import org.hibernate.orm.test.event.collection.ParentWithCollection;
import org.hibernate.testing.orm.junit.DomainModel;

/**
 *
 * @author Gail Badner
 */
@DomainModel(xmlMappings = "org/hibernate/orm/test/event/collection/values/ValuesBagMapping.hbm.xml")
public class ValuesBagCollectionEventTest extends AbstractCollectionEventTest {

	@Override
	public ParentWithCollection createParent(String name) {
		return new ParentWithCollectionOfValues( name );
	}

	@Override
	public Collection createCollection() {
		return new ArrayList();
	}
}
