/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.unique.spi;

import org.hibernate.SPI;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/// Supported base for a provider which decorates another unique-key strategy.
///
/// Override only the operations whose database grammar differs. All other
/// operations, including representation and capability answers, are forwarded.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI({ USE, IMPLEMENT })
public abstract class DelegatingUniqueDelegate implements UniqueDelegate {
	private final UniqueDelegate delegate;

	/// Construct a decorator which forwards to the given strategy.
	@SPI(IMPLEMENT)
	protected DelegatingUniqueDelegate(UniqueDelegate delegate) {
		this.delegate = requireNonNull( delegate, "delegate" );
	}

	protected final UniqueDelegate delegate() {
		return delegate;
	}

	@Override
	public UniqueKeyRepresentation representation(UniqueKeyRepresentationRequest request) {
		return delegate.representation( request );
	}

	@Override
	public boolean supportsNullsNotDistinct() {
		return delegate.supportsNullsNotDistinct();
	}

	@Override
	public String getColumnDefinitionUniquenessFragment(Column column, SqlStringGenerationContext context) {
		return delegate.getColumnDefinitionUniquenessFragment( column, context );
	}

	@Override
	public String getTableCreationUniqueConstraintsFragment(Table table, SqlStringGenerationContext context) {
		return delegate.getTableCreationUniqueConstraintsFragment( table, context );
	}

	@Override
	public String getAlterTableToAddUniqueKeyCommand(
			UniqueKey uniqueKey,
			Metadata metadata,
			SqlStringGenerationContext context) {
		return delegate.getAlterTableToAddUniqueKeyCommand( uniqueKey, metadata, context );
	}

	@Override
	public String getAlterTableToDropUniqueKeyCommand(
			UniqueKey uniqueKey,
			Metadata metadata,
			SqlStringGenerationContext context) {
		return delegate.getAlterTableToDropUniqueKeyCommand( uniqueKey, metadata, context );
	}
}
