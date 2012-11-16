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

import org.hibernate.dialect.CUBRIDDialect;
import org.hibernate.test.immutable.entitywithmutablecollection.AbstractEntityWithOneToManyTest;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.Skip;

/**
 * @author Gail Badner
 */

// The overridden tests are known to pass because one-to-many on a join table
// is not built properly due to HHH-6391. These they are skipped for now.
// When HHH-6391 is fixed, the skipped (overridden) tests should be removed.
@FailureExpectedWithNewMetamodel
@SkipForDialect(
        value = CUBRIDDialect.class,
        comment = "As of verion 8.4.1 CUBRID doesn't support temporary tables. This test fails with" +
                "HibernateException: cannot doAfterTransactionCompletion multi-table deletes using dialect not supporting temp tables"
)
public class EntityWithInverseOneToManyJoinTest extends AbstractEntityWithOneToManyTest {
	@Override
	public String[] getMappings() {
		return new String[] { "immutable/entitywithmutablecollection/inverse/ContractVariationOneToManyJoin.hbm.xml" };
	}

	// TODO: HHH-6391 is fixed, this (overridden) test should be removed.
	@Test
	@Override
	@Skip( condition = Skip.AlwaysSkip.class,message = "skip until HHH-6391 is fixed.")
	public void testDeleteOneToManyOrphan() {
		super.testDeleteOneToManyOrphan();
	}

	// TODO: HHH-6391 is fixed, this (overridden) test should be removed.
	@Test
	@Override
	@Skip( condition = Skip.AlwaysSkip.class,message = "skip until HHH-6391 is fixed.")
	public void testRemoveOneToManyOrphanUsingMerge() {
		super.testRemoveOneToManyOrphanUsingMerge();
	}

	// TODO: HHH-6391 is fixed, this (overridden) test should be removed.
	@Test
	@Override
	@Skip( condition = Skip.AlwaysSkip.class,message = "skip until HHH-6391 is fixed.")
	public void testRemoveOneToManyOrphanUsingUpdate() {
		super.testRemoveOneToManyOrphanUsingUpdate();
	}
}
