/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temptable.spi;

import java.util.List;

import jakarta.annotation.Nullable;

import org.hibernate.SPI;

/// Read-only description of a temporary table supplied to a
/// [TemporaryTableExporter].
///
/// Hibernate owns descriptor construction. Exporters may inspect the descriptor
/// while assembling runtime DDL, but must not retain it or assume a concrete
/// implementation type.
///
/// @since 8.0
/// @author Steve Ebersole
@SPI
public interface TemporaryTableDescriptor {
	/// The qualified table name rendered in SQL.
	String getQualifiedTableName();

	/// The temporary-table kind selected for this table.
	TemporaryTableKind getTemporaryTableKind();

	/// The immutable columns in DDL export order.
	List<? extends TemporaryTableColumnDescriptor> getColumnsForExport();

	/// The session discriminator column used by persistent temporary tables, or
	/// `null` when rows do not require session isolation.
	@Nullable TemporaryTableColumnDescriptor getSessionUidColumn();
}
