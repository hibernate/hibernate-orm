/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temptable.spi;

import java.util.function.Function;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// An exporter for temporary tables.
///
/// Unlike other [DDL exporters][org.hibernate.tool.schema.spi.Exporter], this
/// exporter is called at runtime instead of during schema management. A custom
/// implementation should normally extend or compose
/// [StandardTemporaryTableExporter] and must interpret descriptors according to
/// the [TemporaryTableStrategy] supplied by the same Dialect.
///
/// Exporters may be reused concurrently. They must not retain descriptors or
/// sessions passed to these methods.
///
/// @see Dialect#getTemporaryTableExporter()
///
/// @since 8.0
/// @author Steve Ebersole
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface TemporaryTableExporter {
	/// Build the complete create-table command for the descriptor.
	String getSqlCreateCommand(TemporaryTableDescriptor temporaryTable);

	/// Build the complete drop-table command for the descriptor.
	String getSqlDropCommand(TemporaryTableDescriptor temporaryTable);

	/// Build the complete cleanup command for the descriptor.
	///
	/// For a persistent table, use `sessionUidAccess` to restrict cleanup to the
	/// current session. Do not evaluate it when the descriptor has no session
	/// discriminator column.
	String getSqlTruncateCommand(
			TemporaryTableDescriptor temporaryTable,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			SharedSessionContractImplementor session);
}
