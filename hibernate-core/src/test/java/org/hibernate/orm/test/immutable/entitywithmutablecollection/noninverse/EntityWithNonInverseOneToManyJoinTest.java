/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.immutable.entitywithmutablecollection.noninverse;

import org.hibernate.dialect.CUBRIDDialect;
import org.hibernate.orm.test.immutable.entitywithmutablecollection.AbstractEntityWithOneToManyTest;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SkipForDialect;

/**
 * @author Gail Badner
 */
@SkipForDialect(
		dialectClass = CUBRIDDialect.class,
		reason = "As of verion 8.4.1 CUBRID doesn't support temporary tables. This test fails with" +
				"HibernateException: cannot doAfterTransactionCompletion multi-table deletes using dialect not supporting temp tables"
)
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/immutable/entitywithmutablecollection/noninverse/ContractVariationOneToManyJoin.hbm.xml"
)
public class EntityWithNonInverseOneToManyJoinTest extends AbstractEntityWithOneToManyTest {
}
