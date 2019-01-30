/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

public class HANADialectTestCase extends BaseUnitTestCase {

	@Test
	@TestForIssue(jiraKey = "HHH-13239")
	public void testLockWaitTimeout() {
		HANAColumnStoreDialect dialect = new HANAColumnStoreDialect();

		String sql = "select dummy from sys.dummy";

		LockOptions lockOptions = new LockOptions( LockMode.PESSIMISTIC_WRITE );
		lockOptions.setTimeOut( 2000 );

		Map<String, String[]> keyColumns = new HashMap<>();

		String sqlWithLock = dialect.applyLocksToSql( sql, lockOptions, new HashMap<>() );
		assertEquals( sql + " for update wait 2", sqlWithLock );

		lockOptions.setTimeOut( 0 );
		sqlWithLock = dialect.applyLocksToSql( sql, lockOptions, new HashMap<>() );
		assertEquals( sql + " for update nowait", sqlWithLock );

		lockOptions.setTimeOut( 500 );
		sqlWithLock = dialect.applyLocksToSql( sql, lockOptions, new HashMap<>() );
		assertEquals( sql + " for update nowait", sqlWithLock );

		lockOptions.setTimeOut( 1500 );
		sqlWithLock = dialect.applyLocksToSql( sql, lockOptions, new HashMap<>() );
		assertEquals( sql + " for update wait 1", sqlWithLock );

		lockOptions.setAliasSpecificLockMode( "dummy", LockMode.PESSIMISTIC_READ );
		keyColumns.put( "dummy", new String[]{ "dummy" } );
		sqlWithLock = dialect.applyLocksToSql( sql, lockOptions, keyColumns );
		assertEquals( sql + " for update of dummy.dummy wait 1", sqlWithLock );
	}
}
