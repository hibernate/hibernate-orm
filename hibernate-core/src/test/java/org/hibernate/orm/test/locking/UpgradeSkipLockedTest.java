/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.query.Query;

/**
 * @author Vlad Mihalcea
 */
public class UpgradeSkipLockedTest
		extends AbstractSkipLockedTest {


	@Override
	protected void applySkipLocked(Query query) {
		query.setLockOptions(
			new LockOptions( lockMode() ).setFollowOnLocking( false )
		);
	}

	@Override
	protected LockMode lockMode() {
		return LockMode.UPGRADE_SKIPLOCKED;
	}
}
