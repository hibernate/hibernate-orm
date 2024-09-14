/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
