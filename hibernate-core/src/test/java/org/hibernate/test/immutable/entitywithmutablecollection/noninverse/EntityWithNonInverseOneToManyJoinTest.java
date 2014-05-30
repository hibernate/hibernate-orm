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
package org.hibernate.test.immutable.entitywithmutablecollection.noninverse;

import org.hibernate.dialect.CUBRIDDialect;
import org.hibernate.test.immutable.entitywithmutablecollection.AbstractEntityWithOneToManyTest;
import org.hibernate.testing.FailureExpectedWithNewUnifiedXsd;
import org.hibernate.testing.SkipForDialect;

/**
 * @author Gail Badner
 */
@SkipForDialect(
        value = CUBRIDDialect.class,
        comment = "As of verion 8.4.1 CUBRID doesn't support temporary tables. This test fails with" +
                "HibernateException: cannot doAfterTransactionCompletion multi-table deletes using dialect not supporting temp tables"
)
@FailureExpectedWithNewUnifiedXsd
public class EntityWithNonInverseOneToManyJoinTest extends AbstractEntityWithOneToManyTest {
	public String[] getMappings() {
//		return new String[] { "immutable/entitywithmutablecollection/noninverse/ContractVariationOneToManyJoin.hbm.xml" };
		// TODO: force it to blow up -- some of the abstract methods pass, so the builds will fail w/o this
		return null;
	}
}
