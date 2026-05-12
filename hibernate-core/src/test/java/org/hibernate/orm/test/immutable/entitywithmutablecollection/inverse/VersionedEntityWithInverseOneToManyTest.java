/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.immutable.entitywithmutablecollection.inverse;

import org.hibernate.orm.test.immutable.entitywithmutablecollection.AbstractEntityWithOneToManyTest;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;

/**
 * @author Gail Badner
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/immutable/entitywithmutablecollection/inverse/ContractVariationVersioned.hbm.xml"
)
public class VersionedEntityWithInverseOneToManyTest extends AbstractEntityWithOneToManyTest {
	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Override
	protected boolean checkUpdateCountsAfterAddingExistingElement() {
		return false;
	}

	@Override
	protected boolean checkUpdateCountsAfterRemovingElementWithoutDelete() {
		return false;
	}
}
