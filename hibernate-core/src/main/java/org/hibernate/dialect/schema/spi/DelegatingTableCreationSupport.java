/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.schema.spi;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.IMPLEMENT;

/// Provider base forwarding table-creation grammar to a stable delegate.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(IMPLEMENT)
public abstract class DelegatingTableCreationSupport implements TableCreationSupport {
	private final TableCreationSupport delegate;

	@SPI(IMPLEMENT)
	protected DelegatingTableCreationSupport(TableCreationSupport delegate) {
		this.delegate = requireNonNull( delegate );
	}

	@Override public String createTableCommand(TableCreationKind kind) { return delegate.createTableCommand( kind ); }
	@Override public String tableCreationOptions() { return delegate.tableCreationOptions(); }
	@Override public boolean requiresViewColumnList() { return delegate.requiresViewColumnList(); }
}
