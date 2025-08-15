/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.unit.locktimeout;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.Test;

import static org.hibernate.Timeouts.NO_WAIT;
import static org.hibernate.Timeouts.SKIP_LOCKED;
import static org.junit.Assert.assertEquals;

/**
 * Expected lock clauses according to the official HANA FOR UPDATE clause documentation:
 *
 * https://help.sap.com/saphelp_hanaplatform/helpdata/en/20/fcf24075191014a89e9dc7b8408b26/content.htm#loio20fcf24075191014a89e9dc7b8408b26__sql_select_1sql_select_for_update
 *
 * @author Vlad Mihalcea
 */
@RequiresDialect(HANADialect.class)
public class HANALockTimeoutTest extends BaseUnitTestCase {

	private final Dialect dialect = new HANADialect();

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
				dialect.getForUpdateString( new LockOptions( LockMode.PESSIMISTIC_READ ).setTimeout( NO_WAIT ) )
		);
		assertEquals(
				" for update nowait",
				dialect.getForUpdateString( new LockOptions( LockMode.PESSIMISTIC_WRITE ).setTimeout( NO_WAIT ) )
		);
	}

	@Test
	public void testLockTimeoutNoAliasSkipLocked() {
		assertEquals(
				" for update ignore locked",
				dialect.getForUpdateString( new LockOptions( LockMode.PESSIMISTIC_READ ).setTimeout( SKIP_LOCKED ) )
		);
		assertEquals(
				" for update ignore locked",
				dialect.getForUpdateString( new LockOptions( LockMode.PESSIMISTIC_WRITE ).setTimeout( SKIP_LOCKED ) )
		);
	}

	@Test
	public void testLockTimeoutAliasNoTimeout() {
		String alias = "a";
		assertEquals(
				" for update of a",
				dialect.getForUpdateString( alias, new LockOptions( LockMode.PESSIMISTIC_READ ) )
		);
		assertEquals(
				" for update of a",
				dialect.getForUpdateString( alias, new LockOptions( LockMode.PESSIMISTIC_WRITE ) )
		);
	}

	@Test
	public void testLockTimeoutAliasNoWait() {
		String alias = "a";
		assertEquals(
				" for update of a nowait",
				dialect.getForUpdateString(
						alias,
						new LockOptions( LockMode.PESSIMISTIC_READ ).setTimeout( NO_WAIT )
				)
		);
		assertEquals(
				" for update of a nowait",
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
				" for update of a ignore locked",
				dialect.getForUpdateString(
						alias,
						new LockOptions( LockMode.PESSIMISTIC_READ )
								.setTimeout( SKIP_LOCKED )
				)
		);
		assertEquals(
				" for update of a ignore locked",
				dialect.getForUpdateString(
						alias,
						new LockOptions( LockMode.PESSIMISTIC_WRITE )
								.setTimeout( SKIP_LOCKED )
				)
		);
	}
}
