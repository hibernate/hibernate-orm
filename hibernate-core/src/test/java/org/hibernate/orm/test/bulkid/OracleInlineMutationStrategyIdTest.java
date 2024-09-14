/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bulkid;

/**
 * Special test that tries to update 1100 rows. Oracle only supports up to 1000 parameters per in-predicate,
 * so we want to test if this scenario works.
 *
 * @author Vlad Mihalcea
 */
public class OracleInlineMutationStrategyIdTest extends InlineMutationStrategyIdTest {

	@Override
	protected int entityCount() {
		return 1100;
	}
}
