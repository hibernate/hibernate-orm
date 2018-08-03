/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import static org.junit.Assert.assertEquals;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.junit.Test;

public class Oracle9iDialectTestCase {

	@Test
	public void testGetForUpdateStringWithAllAliasesSpecified() {
		Oracle9iDialect dialect = new Oracle9iDialect();
		LockOptions lockOptions = new LockOptions();
		lockOptions.setAliasSpecificLockMode( "tableAlias1", LockMode.PESSIMISTIC_WRITE );

		String forUpdateClause = dialect.getForUpdateString( "tableAlias1", lockOptions );
		assertEquals( " for update", forUpdateClause );

		lockOptions.setAliasSpecificLockMode( "tableAlias2", LockMode.PESSIMISTIC_WRITE );
		forUpdateClause = dialect.getForUpdateString( "tableAlias1,tableAlias2", lockOptions );
		assertEquals( " for update", forUpdateClause );

	}

	@Test
	public void testGetForUpdateStringWithoutAliasSpecified() {
		Oracle9iDialect dialect = new Oracle9iDialect();
		LockOptions lockOptions = new LockOptions();
		lockOptions.setAliasSpecificLockMode( "tableAlias1", LockMode.PESSIMISTIC_WRITE );

		String forUpdateClause = dialect.getForUpdateString( "", lockOptions );
		assertEquals( " for update", forUpdateClause );
	}

	@Test
	public void testGetForUpdateStringWithSomeAliasSpecified() {
		Oracle9iDialect dialect = new Oracle9iDialect();
		LockOptions lockOptions = new LockOptions();
		lockOptions.setAliasSpecificLockMode( "tableAlias1", LockMode.PESSIMISTIC_WRITE );
		lockOptions.setAliasSpecificLockMode( "tableAlias2", LockMode.PESSIMISTIC_WRITE );

		String forUpdateClause = dialect.getForUpdateString( "tableAlias1", lockOptions );
		assertEquals( " for update of tableAlias1", forUpdateClause );

		lockOptions.setAliasSpecificLockMode( "tableAlias3", LockMode.PESSIMISTIC_WRITE );

		forUpdateClause = dialect.getForUpdateString( "tableAlias1,tableAlias3", lockOptions );
		assertEquals( " for update of tableAlias1,tableAlias3", forUpdateClause );
	}

}
