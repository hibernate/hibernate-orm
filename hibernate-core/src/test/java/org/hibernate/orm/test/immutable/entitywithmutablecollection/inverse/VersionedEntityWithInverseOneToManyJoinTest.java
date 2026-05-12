/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.immutable.entitywithmutablecollection.inverse;

import org.hibernate.orm.test.immutable.entitywithmutablecollection.AbstractEntityWithOneToManyTest;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;


/**
 * @author Gail Badner
 */
@JiraKey(value = "HHH-4992")
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsTemporaryTable.class)
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/immutable/entitywithmutablecollection/inverse/ContractVariationVersionedOneToManyJoin.hbm.xml"
)
public class VersionedEntityWithInverseOneToManyJoinTest extends AbstractEntityWithOneToManyTest {
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
