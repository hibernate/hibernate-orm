/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.creation.internal.options;

import jakarta.annotation.Nonnull;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/// Mutable collector for creating a stateful child session from an existing
/// session.
///
/// The collector starts with the normal stateful defaults and then inherits the
/// values that are meaningful for child session creation, such as tenant
/// identifier, identifier rollback, JDBC time zone, and temporal identifier.
/// Calling [#shareTransactionContext()] enables reuse of the original
/// session's transaction/JDBC context through [CommonSharedOptions].
///
/// @since 8.0
/// @author Steve Ebersole
public class SharedStatefulOptions extends StatefulOptions implements CommonSharedOptions {
	private final SharedSessionContractImplementor original;
	private boolean shareTransactionContext;

	public SharedStatefulOptions(SharedSessionContractImplementor original) {
		super( original.getFactory() );
		this.original = original;
		tenantIdentifier( original.getTenantIdentifierValue() );
		identifierRollback( original.isIdentifierRollbackEnabled() );
		jdbcTimeZone( original.getJdbcTimeZone() );
		atChangeset( original.getLoadQueryInfluencers().getTemporalIdentifier() );
	}

	@Override
	@Nonnull
	public SharedSessionContractImplementor getOriginalSession() {
		return original;
	}

	@Override
	public boolean isTransactionCoordinatorShared() {
		return shareTransactionContext;
	}

	/// Enable sharing of the original session's transaction/JDBC context.
	public void shareTransactionContext() {
		shareTransactionContext = true;
	}
}
