/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.spi;


import org.hibernate.SPI;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.Exportable;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.internal.util.collections.ArrayHelper;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Exports a relational database object as SQL create and drop commands.
///
/// Implement this contract when a database requires a complete DDL operation
/// different from Hibernate's stock exporter. Return commands in execution
/// order and return [#NO_COMMANDS] when no command is required. An exporter must
/// not execute SQL or retain the per-call metadata or generation context.
///
/// Supply each database-object specialization from the corresponding Dialect
/// method.
///
/// @apiNote This is an ORM-centric contract.
///
/// @see org.hibernate.dialect.Dialect#getTableExporter()
/// @see org.hibernate.dialect.Dialect#getSequenceExporter()
/// @see org.hibernate.dialect.Dialect#getIndexExporter()
/// @see org.hibernate.dialect.Dialect#getForeignKeyExporter()
/// @see org.hibernate.dialect.Dialect#getUserDefinedTypeExporter()
///
/// @since 8.0
/// @author Steve Ebersole
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface Exporter<T extends Exportable> {
	/// An immutable empty command result.
	String[] NO_COMMANDS = ArrayHelper.EMPTY_STRING_ARRAY;

	/// Return the commands needed to create `exportable`, in execution order.
	String[] getSqlCreateStrings(T exportable, Metadata metadata, SqlStringGenerationContext context);

	/// Return the commands needed to drop `exportable`, in execution order.
	String[] getSqlDropStrings(T exportable, Metadata metadata, SqlStringGenerationContext context);
}
