/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.hibernate.dialect.AbstractSybaseDialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.lock.spi.LockingSupport;

/// External provider Dialect based on the supported Sybase-family adapter.
///
/// @since 8.0
/// @author Steve Ebersole
// tag::sybase-family-base[]
public final class ExampleSybaseDialect extends AbstractSybaseDialect {
	/// Creates the fixture Dialect for its declared database version.
	///
	/// @since 8.0
	public ExampleSybaseDialect() {
		super( DatabaseVersion.make( 17 ) );
	}

	/// Supplies the fixture-owned locking profile.
	///
	/// @since 8.0
	@Override
	public LockingSupport getLockingSupport() {
		return ExampleLockingSupport.INSTANCE;
	}
}
// end::sybase-family-base[]
