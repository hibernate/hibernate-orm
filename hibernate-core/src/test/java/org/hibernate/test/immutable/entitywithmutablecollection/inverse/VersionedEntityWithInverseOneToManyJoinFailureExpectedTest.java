/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.immutable.entitywithmutablecollection.inverse;

import org.hibernate.dialect.CUBRIDDialect;
import org.hibernate.testing.SkipForDialect;
import org.junit.Test;

import org.hibernate.test.immutable.entitywithmutablecollection.AbstractEntityWithOneToManyTest;
import org.hibernate.testing.FailureExpected;

/**
 * @author Gail Badner
 */
@SkipForDialect(
        value = CUBRIDDialect.class,
        comment = "As of verion 8.4.1 CUBRID doesn't support temporary tables. This test fails with" +
                "HibernateException: cannot doAfterTransactionCompletion multi-table deletes using dialect not supporting temp tables"
)
public class VersionedEntityWithInverseOneToManyJoinFailureExpectedTest extends AbstractEntityWithOneToManyTest {
	public String[] getMappings() {
		return new String[] { "immutable/entitywithmutablecollection/inverse/ContractVariationVersionedOneToManyJoin.hbm.xml" };
	}

	@Test
	@Override
	@FailureExpected(
			jiraKey = "HHH-4992",
			message = "known to fail with inverse collection"
	)
	public void testAddExistingOneToManyElementToPersistentEntity() {
		super.testAddExistingOneToManyElementToPersistentEntity();
	}

	@Test
	@Override
	@FailureExpected(
			jiraKey = "HHH-4992",
			message = "known to fail with inverse collection"
	)
	public void testCreateWithEmptyOneToManyCollectionUpdateWithExistingElement() {
		super.testCreateWithEmptyOneToManyCollectionUpdateWithExistingElement();
	}

	@Test
	@Override
	@FailureExpected(
			jiraKey = "HHH-4992",
			message = "known to fail with inverse collection"
	)
	public void testCreateWithEmptyOneToManyCollectionMergeWithExistingElement() {
		super.testCreateWithEmptyOneToManyCollectionMergeWithExistingElement();
	}

	@Test
	@Override
	@FailureExpected(
			jiraKey = "HHH-4992",
			message = "known to fail with inverse collection"
	)
	public void testRemoveOneToManyElementUsingUpdate() {
		super.testRemoveOneToManyElementUsingUpdate();
	}

	@Test
	@Override
	@FailureExpected(
			jiraKey = "HHH-4992",
			message = "known to fail with inverse collection"
	)
	public void testRemoveOneToManyElementUsingMerge() {
		super.testRemoveOneToManyElementUsingMerge();
	}
}
