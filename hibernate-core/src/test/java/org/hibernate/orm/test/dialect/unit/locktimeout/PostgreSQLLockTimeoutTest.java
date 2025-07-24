/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.unit.locktimeout;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQLDialect;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.Test;

import static org.hibernate.Timeouts.NO_WAIT;
import static org.hibernate.Timeouts.SKIP_LOCKED;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(PostgreSQLDialect.class)
public class PostgreSQLLockTimeoutTest extends BaseUnitTestCase {

	private final Dialect dialect = new PostgreSQLDialect();

	@Test
	public void testLockTimeoutNoAliasNoTimeout() {
		assertEquals(
				" for share",
				dialect.getForUpdateString( new LockOptions( LockMode.PESSIMISTIC_READ ) )
		);
		assertEquals(
				" for no key update",
				dialect.getForUpdateString( new LockOptions( LockMode.PESSIMISTIC_WRITE ) )
		);
	}

	@Test
	public void testLockTimeoutNoAliasNoWait() {
		assertEquals(
				" for share nowait",
				dialect.getForUpdateString( new LockOptions( LockMode.PESSIMISTIC_READ )
						.setTimeout( NO_WAIT ) )
		);
		assertEquals(
				" for no key update nowait",
				dialect.getForUpdateString( new LockOptions( LockMode.PESSIMISTIC_WRITE )
						.setTimeout( NO_WAIT ) )
		);
	}

	@Test
	public void testLockTimeoutNoAliasSkipLocked() {
		assertEquals(
				" for share skip locked",
				dialect.getForUpdateString( new LockOptions( LockMode.PESSIMISTIC_READ )
						.setTimeout( SKIP_LOCKED ) )
		);
		assertEquals(
				" for no key update skip locked",
				dialect.getForUpdateString( new LockOptions( LockMode.PESSIMISTIC_WRITE )
						.setTimeout( SKIP_LOCKED ) )
		);
	}

	@Test
	public void testLockTimeoutAliasNoTimeout() {
		String alias = "a";
		assertEquals(
				" for share of a",
				dialect.getForUpdateString(
						alias,
						new LockOptions( LockMode.PESSIMISTIC_READ )
				)
		);
		assertEquals(
				" for no key update of a",
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
				" for share of a nowait",
				dialect.getForUpdateString(
						alias,
						new LockOptions( LockMode.PESSIMISTIC_READ ).setTimeout( NO_WAIT )
				)
		);
		assertEquals(
				" for no key update of a nowait",
				dialect.getForUpdateString(
						alias,
						new LockOptions( LockMode.PESSIMISTIC_WRITE ).setTimeout( NO_WAIT )
				)
		);
	}

	@Test
	public void testLockTimeoutAliasSkipLocked() {
		String alias = "a";
		assertEquals(
				" for share of a skip locked",
				dialect.getForUpdateString(
						alias,
						new LockOptions( LockMode.PESSIMISTIC_READ ).setTimeout( SKIP_LOCKED )
				)
		);
		assertEquals(
				" for no key update of a skip locked",
				dialect.getForUpdateString(
						alias,
						new LockOptions( LockMode.PESSIMISTIC_WRITE ).setTimeout( SKIP_LOCKED )
				)
		);
	}
}
