/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.test.immutable.entitywithmutablecollection.inverse;

import junit.framework.Test;

import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.test.immutable.entitywithmutablecollection.AbstractEntityWithOneToManyTest;

/**
 * @author Gail Badner
 *
 * These tests reproduce HHH-4992.
 */
public class VersionedEntityWithInverseOneToManyFailureExpectedTest extends AbstractEntityWithOneToManyTest {

	public VersionedEntityWithInverseOneToManyFailureExpectedTest(String str) {
		super(str);
	}

	public String[] getMappings() {
		return new String[] { "immutable/entitywithmutablecollection/inverse/ContractVariationVersioned.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( VersionedEntityWithInverseOneToManyFailureExpectedTest.class );
	}

	public void testAddExistingOneToManyElementToPersistentEntity() {
		reportSkip(
				"known to fail with versioned entity with inverse collection",
				"AddExistingOneToManyElementToPersistentEntity"
		);
	}

	public void testAddExistingOneToManyElementToPersistentEntityFailureExpected() {
		super.testAddExistingOneToManyElementToPersistentEntity();
	}

	public void testCreateWithEmptyOneToManyCollectionUpdateWithExistingElement() {
		reportSkip(
				"known to fail with versioned entity with inverse collection",
				"CreateWithEmptyOneToManyCollectionUpdateWithExistingElement"
		);
	}

	public void testCreateWithEmptyOneToManyCollectionUpdateWithExistingElementFailureExpected() {
		super.testCreateWithEmptyOneToManyCollectionUpdateWithExistingElement();
	}

	public void testCreateWithEmptyOneToManyCollectionMergeWithExistingElement() {
		reportSkip(
				"known to fail with versioned entity with inverse collection",
				"CreateWithEmptyOneToManyCollectionMergeWithExistingElement"
		);
	}

	public void testCreateWithEmptyOneToManyCollectionMergeWithExistingElementFailureExpected() {
		super.testCreateWithEmptyOneToManyCollectionMergeWithExistingElement();
	}

	public void testRemoveOneToManyElementUsingUpdate() {
		reportSkip(
				"known to fail with versioned entity with inverse collection",
				"RemoveOneToManyElementUsingUpdate"
		);
	}

	public void testRemoveOneToManyElementUsingUpdateFailureExpected() {
		super.testRemoveOneToManyElementUsingUpdate();
	}

	public void testRemoveOneToManyElementUsingMerge() {
		reportSkip(
				"known to fail with versioned entity with inverse collection",
				"RemoveOneToManyElementUsingMerge" 
		);
	}

	public void testRemoveOneToManyElementUsingMergeFailureExpected() {
		super.testRemoveOneToManyElementUsingMerge();
	}
}