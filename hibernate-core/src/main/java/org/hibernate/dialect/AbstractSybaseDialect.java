/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import org.hibernate.SPI;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Supported provider base for Dialects derived from the Sybase family.
///
/// Extend this class when a provider needs to reuse the maintained
/// [SybaseDialect] implementation. Applications should select a concrete
/// Dialect instead of this abstract extension contract.
///
/// @since 8.0
/// @author Steve Ebersole
@SPI({ USE, IMPLEMENT })
public abstract class AbstractSybaseDialect extends SybaseDialect {
	/// Creates a Sybase-family Dialect for a known database version.
	///
	/// @since 8.0
	@SPI(IMPLEMENT)
	protected AbstractSybaseDialect(DatabaseVersion version) {
		super( version );
	}

	/// Creates a Sybase-family Dialect from bootstrap resolution information.
	///
	/// @since 8.0
	@SPI(IMPLEMENT)
	protected AbstractSybaseDialect(DialectResolutionInfo info) {
		super( info );
	}

	/// Supplies the pessimistic-locking profile inherited from the maintained
	/// Sybase Dialect. Override this method to supply a provider-specific profile.
	///
	/// @since 8.0
	/// @see LockingSupport
	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public LockingSupport getLockingSupport() {
		return super.getLockingSupport();
	}
}
