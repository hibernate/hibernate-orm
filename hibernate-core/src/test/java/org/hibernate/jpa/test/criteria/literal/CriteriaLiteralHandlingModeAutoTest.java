/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria.literal;

import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.RequiresDialect;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
public class CriteriaLiteralHandlingModeAutoTest extends AbstractCriteriaLiteralHandlingModeTest {

	protected String expectedSQL() {
		return "select 'abc' as col_0_0_, abstractcr0_.name as col_1_0_ from Book abstractcr0_ where abstractcr0_.id=1 and abstractcr0_.name=?";
	}
}
