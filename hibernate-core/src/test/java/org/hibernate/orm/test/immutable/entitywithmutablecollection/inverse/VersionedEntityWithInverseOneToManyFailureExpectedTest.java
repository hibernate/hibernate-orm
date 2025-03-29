/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.immutable.entitywithmutablecollection.inverse;

import org.hibernate.orm.test.immutable.entitywithmutablecollection.AbstractEntityWithOneToManyTest;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Gail Badner
 * <p>
 * These tests reproduce HHH-4992.
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/immutable/entitywithmutablecollection/inverse/ContractVariationVersioned.hbm.xml"
)
public class VersionedEntityWithInverseOneToManyFailureExpectedTest extends AbstractEntityWithOneToManyTest {
	public String[] getMappings() {
		return new String[] {};
	}

	@Test
	@Override
	@FailureExpected(
			jiraKey = "HHH-4992",
			reason = "known to fail with versioned entity with inverse collection"
	)
	public void testAddExistingOneToManyElementToPersistentEntity(SessionFactoryScope scope) {
		super.testAddExistingOneToManyElementToPersistentEntity( scope );
	}

	@Test
	@Override
	@FailureExpected(
			jiraKey = "HHH-4992",
			reason = "known to fail with versioned entity with inverse collection"
	)
	public void testCreateWithEmptyOneToManyCollectionUpdateWithExistingElement(SessionFactoryScope scope) {
		super.testCreateWithEmptyOneToManyCollectionUpdateWithExistingElement( scope );
	}

	@Test
	@Override
	@FailureExpected(
			jiraKey = "HHH-4992",
			reason = "known to fail with versioned entity with inverse collection"
	)
	public void testCreateWithEmptyOneToManyCollectionMergeWithExistingElement(SessionFactoryScope scope) {
		super.testCreateWithEmptyOneToManyCollectionMergeWithExistingElement( scope );
	}

	@Test
	@Override
	@FailureExpected(
			jiraKey = "HHH-4992",
			reason = "known to fail with versioned entity with inverse collection"
	)
	public void testRemoveOneToManyElementUsingUpdate(SessionFactoryScope scope) {
		super.testRemoveOneToManyElementUsingUpdate( scope );
	}

	@Test
	@Override
	@FailureExpected(
			jiraKey = "HHH-4992",
			reason = "known to fail with versioned entity with inverse collection"
	)
	public void testRemoveOneToManyElementUsingMerge(SessionFactoryScope scope) {
		super.testRemoveOneToManyElementUsingMerge( scope );
	}
}
