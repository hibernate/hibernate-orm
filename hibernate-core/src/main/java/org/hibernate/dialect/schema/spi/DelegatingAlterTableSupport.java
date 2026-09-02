/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.schema.spi;

import jakarta.annotation.Nullable;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.IMPLEMENT;

/// Provider base forwarding alter-table grammar to a stable delegate.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(IMPLEMENT)
public abstract class DelegatingAlterTableSupport implements AlterTableSupport {
	private final AlterTableSupport delegate;

	@SPI(IMPLEMENT)
	protected DelegatingAlterTableSupport(AlterTableSupport delegate) {
		this.delegate = requireNonNull( delegate );
	}

	@Override public String alterTableCommand(String tableName, ExistenceCheckPlacement placement) { return delegate.alterTableCommand( tableName, placement ); }
	@Override public String addColumnPrefix() { return delegate.addColumnPrefix(); }
	@Override public String addColumnSuffix() { return delegate.addColumnSuffix(); }
	@Override public @Nullable String alterColumnType(AlterColumnTypeRequest request) { return delegate.alterColumnType( request ); }
}
