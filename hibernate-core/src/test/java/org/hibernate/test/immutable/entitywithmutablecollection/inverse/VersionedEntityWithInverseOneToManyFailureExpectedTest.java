/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.immutable.entitywithmutablecollection.inverse;

import org.junit.Test;

import org.hibernate.test.immutable.entitywithmutablecollection.AbstractEntityWithOneToManyTest;
import org.hibernate.testing.FailureExpected;

/**
 * @author Gail Badner
 *
 * These tests reproduce HHH-4992.
 */
public class VersionedEntityWithInverseOneToManyFailureExpectedTest extends AbstractEntityWithOneToManyTest {
	public String[] getMappings() {
		return new String[] { "immutable/entitywithmutablecollection/inverse/ContractVariationVersioned.hbm.xml" };
	}

	@Test
	@Override
	@FailureExpected(
			jiraKey = "HHH-4992",
			message = "known to fail with versioned entity with inverse collection"
	)
	public void testAddExistingOneToManyElementToPersistentEntity() {
		super.testAddExistingOneToManyElementToPersistentEntity();
	}

	@Test
	@Override
	@FailureExpected(
			jiraKey = "HHH-4992",
			message = "known to fail with versioned entity with inverse collection"
	)
	public void testCreateWithEmptyOneToManyCollectionUpdateWithExistingElement() {
		super.testCreateWithEmptyOneToManyCollectionUpdateWithExistingElement();
	}

	@Test
	@Override
	@FailureExpected(
			jiraKey = "HHH-4992",
			message = "known to fail with versioned entity with inverse collection"
	)
	public void testCreateWithEmptyOneToManyCollectionMergeWithExistingElement() {
		super.testCreateWithEmptyOneToManyCollectionMergeWithExistingElement();
	}

	@Test
	@Override
	@FailureExpected(
			jiraKey = "HHH-4992",
			message = "known to fail with versioned entity with inverse collection"
	)
	public void testRemoveOneToManyElementUsingUpdate() {
		super.testRemoveOneToManyElementUsingUpdate();
	}

	@Test
	@Override
	@FailureExpected(
			jiraKey = "HHH-4992",
			message = "known to fail with versioned entity with inverse collection"
	)
	public void testRemoveOneToManyElementUsingMerge() {
		super.testRemoveOneToManyElementUsingMerge();
	}
}
