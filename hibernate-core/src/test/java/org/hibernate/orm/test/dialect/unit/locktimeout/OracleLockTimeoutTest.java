/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.unit.locktimeout;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(OracleDialect.class)
public class OracleLockTimeoutTest extends BaseUnitTestCase {

	private final Dialect dialect = new OracleDialect( DatabaseVersion.make( 12 ) );

	@Test
	public void testLockTimeoutNoAliasNoTimeout() {
		assertEquals(
				" for update",
				dialect.getForUpdateString( new LockOptions( LockMode.PESSIMISTIC_READ ) )
		);
		assertEquals(
				" for update",
				dialect.getForUpdateString( new LockOptions( LockMode.PESSIMISTIC_WRITE ) )
		);
	}

	@Test
	public void testLockTimeoutNoAliasNoWait() {
		assertEquals(
				" for update nowait",
				dialect.getForUpdateString( new LockOptions( LockMode.PESSIMISTIC_READ )
													.setTimeOut( LockOptions.NO_WAIT ) )
		);
		assertEquals(
				" for update nowait",
				dialect.getForUpdateString( new LockOptions( LockMode.PESSIMISTIC_WRITE )
													.setTimeOut( LockOptions.NO_WAIT ) )
		);
	}

	@Test
	public void testLockTimeoutNoAliasSkipLocked() {
		assertEquals(
				" for update skip locked",
				dialect.getForUpdateString( new LockOptions( LockMode.PESSIMISTIC_READ )
													.setTimeOut( LockOptions.SKIP_LOCKED ) )
		);
		assertEquals(
				" for update skip locked",
				dialect.getForUpdateString( new LockOptions( LockMode.PESSIMISTIC_WRITE )
													.setTimeOut( LockOptions.SKIP_LOCKED ) )
		);
	}

	@Test
	public void testLockTimeoutAliasNoTimeout() {
		String alias = "a";
		assertEquals(
				" for update",
				dialect.getForUpdateString(
						alias,
						new LockOptions( LockMode.PESSIMISTIC_READ ).setAliasSpecificLockMode(
								alias,
								LockMode.PESSIMISTIC_READ
						)
				)
		);
		assertEquals(
				" for update",
				dialect.getForUpdateString(
						alias,
						new LockOptions( LockMode.PESSIMISTIC_WRITE ).setAliasSpecificLockMode(
								alias,
								LockMode.PESSIMISTIC_WRITE
						)
				)
		);
	}

	@Test
	public void testLockTimeoutAliasNoWait() {
		String alias = "a";
		assertEquals(
				" for update nowait",
				dialect.getForUpdateString(
						alias,
						new LockOptions( LockMode.PESSIMISTIC_READ ).setAliasSpecificLockMode(
								alias,
								LockMode.PESSIMISTIC_READ
						)
								.setTimeOut( LockOptions.NO_WAIT )
				)
		);
		assertEquals(
				" for update nowait",
				dialect.getForUpdateString(
						alias,
						new LockOptions( LockMode.PESSIMISTIC_WRITE ).setAliasSpecificLockMode(
								alias,
								LockMode.PESSIMISTIC_WRITE
						)
								.setTimeOut( LockOptions.NO_WAIT )
				)
		);
	}

	@Test
	public void testLockTimeoutAliasSkipLocked() {
		String alias = "a";
		assertEquals(
				" for update skip locked",
				dialect.getForUpdateString(
						alias,
						new LockOptions( LockMode.PESSIMISTIC_READ ).setAliasSpecificLockMode(
								alias,
								LockMode.PESSIMISTIC_READ
						)
								.setTimeOut( LockOptions.SKIP_LOCKED )
				)
		);
		assertEquals(
				" for update skip locked",
				dialect.getForUpdateString(
						alias,
						new LockOptions( LockMode.PESSIMISTIC_WRITE ).setAliasSpecificLockMode(
								alias,
								LockMode.PESSIMISTIC_WRITE
						)
								.setTimeOut( LockOptions.SKIP_LOCKED )
				)
		);
	}
}
