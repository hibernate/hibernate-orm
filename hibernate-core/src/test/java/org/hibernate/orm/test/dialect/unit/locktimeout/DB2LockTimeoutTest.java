/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.unit.locktimeout;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.hibernate.Timeouts.SKIP_LOCKED;
import static org.junit.Assert.assertEquals;

/**
 * @author Gavin King
 */
public class DB2LockTimeoutTest extends BaseUnitTestCase {

	private final Dialect dialect = new DB2Dialect( DatabaseVersion.make( 11, 5 ) );

	@Test
	public void testLockTimeoutNoAliasNoTimeout() {
		assertEquals(
				" for read only with rs use and keep share locks",
				dialect.getForUpdateString( new LockOptions( LockMode.PESSIMISTIC_READ ) )
		);
		assertEquals(
				" for read only with rs use and keep update locks",
				dialect.getForUpdateString( new LockOptions( LockMode.PESSIMISTIC_WRITE ) )
		);
	}

	@Test
	public void testLockTimeoutNoAliasSkipLocked() {
		assertEquals(
				" for read only with rs use and keep share locks skip locked data",
				dialect.getForUpdateString( new LockOptions( LockMode.PESSIMISTIC_READ ).setTimeout( SKIP_LOCKED ) )
		);
		assertEquals(
				" for read only with rs use and keep update locks skip locked data",
				dialect.getForUpdateString( new LockOptions( LockMode.PESSIMISTIC_WRITE ).setTimeout( SKIP_LOCKED ) )
		);
	}
}
