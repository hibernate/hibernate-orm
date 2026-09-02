/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.spi;

import org.hibernate.SPI;

/// Determines whether a completed SQL statement requires follow-on locking.
///
/// The policy receives only the stable, focused facts relevant to that
/// decision. In particular, it does not receive execution-level query options.
///
/// @see LockingSupport#getFollowOnLockingPolicy()
///
/// @since 8.0
/// @author Steve Ebersole
@FunctionalInterface
@SPI({ SPI.Role.USE, SPI.Role.IMPLEMENT, SPI.Role.SUPPLY })
public interface FollowOnLockingPolicy {
	/// A policy which never requests follow-on locking.
	FollowOnLockingPolicy NEVER = request -> false;

	/// A policy which always requests follow-on locking.
	FollowOnLockingPolicy ALWAYS = request -> true;

	/// Whether the completed statement requires follow-on locking.
	boolean useFollowOnLocking(FollowOnLockingRequest request);
}
