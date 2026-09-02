/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.spi;

import org.hibernate.SPI;

/// Stable entity metadata available while selecting a locking strategy.
///
/// @since 8.0
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public interface EntityLockTarget {
	/// The Hibernate entity name.
	String entityName();

	/// Whether the entity has an optimistic-lock version attribute.
	boolean versioned();
}
