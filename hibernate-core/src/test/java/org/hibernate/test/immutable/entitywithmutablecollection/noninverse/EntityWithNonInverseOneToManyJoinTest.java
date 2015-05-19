/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.immutable.entitywithmutablecollection.noninverse;

import org.hibernate.dialect.CUBRIDDialect;
import org.hibernate.test.immutable.entitywithmutablecollection.AbstractEntityWithOneToManyTest;
import org.hibernate.testing.SkipForDialect;

/**
 * @author Gail Badner
 */
@SkipForDialect(
        value = CUBRIDDialect.class,
        comment = "As of verion 8.4.1 CUBRID doesn't support temporary tables. This test fails with" +
                "HibernateException: cannot doAfterTransactionCompletion multi-table deletes using dialect not supporting temp tables"
)
public class EntityWithNonInverseOneToManyJoinTest extends AbstractEntityWithOneToManyTest {
	public String[] getMappings() {
		return new String[] { "immutable/entitywithmutablecollection/noninverse/ContractVariationOneToManyJoin.hbm.xml" };
	}
}
