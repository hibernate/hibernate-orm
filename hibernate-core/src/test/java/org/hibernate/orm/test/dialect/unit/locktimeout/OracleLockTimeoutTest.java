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
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;

import static org.hibernate.Timeouts.NO_WAIT;
import static org.hibernate.Timeouts.SKIP_LOCKED;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(OracleDialect.class)
public class OracleLockTimeoutTest {

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
						.setTimeout( NO_WAIT ) )
		);
		assertEquals(
				" for update nowait",
				dialect.getForUpdateString( new LockOptions( LockMode.PESSIMISTIC_WRITE )
						.setTimeout( NO_WAIT ) )
		);
	}

	@Test
	public void testLockTimeoutNoAliasSkipLocked() {
		assertEquals(
				" for update skip locked",
				dialect.getForUpdateString( new LockOptions( LockMode.PESSIMISTIC_READ )
						.setTimeout( SKIP_LOCKED ) )
		);
		assertEquals(
				" for update skip locked",
				dialect.getForUpdateString( new LockOptions( LockMode.PESSIMISTIC_WRITE )
						.setTimeout( SKIP_LOCKED ) )
		);
	}

	@Test
	public void testLockTimeoutAliasNoTimeout() {
		String alias = "a";
		assertEquals(
				" for update",
				dialect.getForUpdateString(
						alias,
						new LockOptions( LockMode.PESSIMISTIC_READ )
				)
		);
		assertEquals(
				" for update",
				dialect.getForUpdateString(
						alias,
						new LockOptions( LockMode.PESSIMISTIC_WRITE )
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
						new LockOptions( LockMode.PESSIMISTIC_READ )
								.setTimeout( NO_WAIT )
				)
		);
		assertEquals(
				" for update nowait",
				dialect.getForUpdateString(
						alias,
						new LockOptions( LockMode.PESSIMISTIC_WRITE )
								.setTimeout( NO_WAIT )
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
						new LockOptions( LockMode.PESSIMISTIC_READ )
								.setTimeout( SKIP_LOCKED )
				)
		);
		assertEquals(
				" for update skip locked",
				dialect.getForUpdateString(
						alias,
						new LockOptions( LockMode.PESSIMISTIC_WRITE )
								.setTimeout( SKIP_LOCKED )
				)
		);
	}
}
