/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import java.util.function.Function;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.temptable.spi.StandardTemporaryTableExporter;
import org.hibernate.dialect.temptable.spi.TemporaryTableDescriptor;
import org.hibernate.dialect.temptable.spi.TemporaryTableExporter;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/// Example provider-supplied exporter which composes Hibernate's standard
/// implementation using only the supported read-only descriptor surface.
///
/// @author Steve Ebersole
public final class ExampleTemporaryTableExporter implements TemporaryTableExporter {
	private final TemporaryTableExporter delegate;

	public ExampleTemporaryTableExporter(Dialect dialect) {
		delegate = new StandardTemporaryTableExporter( dialect );
	}

	@Override
	public String getSqlCreateCommand(TemporaryTableDescriptor temporaryTable) {
		return delegate.getSqlCreateCommand( temporaryTable );
	}

	@Override
	public String getSqlDropCommand(TemporaryTableDescriptor temporaryTable) {
		return delegate.getSqlDropCommand( temporaryTable );
	}

	@Override
	public String getSqlTruncateCommand(
			TemporaryTableDescriptor temporaryTable,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			SharedSessionContractImplementor session) {
		return delegate.getSqlTruncateCommand( temporaryTable, sessionUidAccess, session );
	}
}
