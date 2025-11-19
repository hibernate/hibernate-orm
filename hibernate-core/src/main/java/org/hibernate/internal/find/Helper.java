/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.find;

import jakarta.persistence.Timeout;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Locking;
import org.hibernate.Timeouts;

/**
 * @author Steve Ebersole
 */
public class Helper {
	public static LockOptions makeLockOptions(LockMode lockMode, Locking.Scope lockScope, Timeout lockTimeout, Locking.FollowOn lockFollowOn) {
		if ( lockMode == null || lockMode == LockMode.NONE ) {
			return LockOptions.NONE;
		}
		if ( lockMode == LockMode.READ ) {
			return LockOptions.READ;
		}

		final var lockOptions = new LockOptions( lockMode );
		lockOptions.setScope( lockScope != null ? lockScope : Locking.Scope.ROOT_ONLY );
		lockOptions.setTimeout( lockTimeout != null ? lockTimeout : Timeouts.WAIT_FOREVER );
		lockOptions.setFollowOnStrategy( lockFollowOn != null ? lockFollowOn : Locking.FollowOn.ALLOW );
		return lockOptions;
	}
}
