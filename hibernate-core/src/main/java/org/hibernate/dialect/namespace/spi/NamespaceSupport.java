/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.namespace.spi;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Defines the catalog and schema lifecycle operations used by schema tooling.
///
/// Implement this contract with immutable or thread-safe state. Treat every
/// supplied namespace name as already rendered identifier text, return commands
/// in execution order, and do not retain invocation-specific state. Use a stock
/// profile from [NamespaceSupports] whenever its complete behavior matches the
/// database.
///
/// @author Steve Ebersole
/// @since 8.0
/// @see Dialect#getNamespaceSupport()
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface NamespaceSupport {
	/// Report whether schema tooling may create and drop catalogs.
	boolean canCreateCatalog();

	/// Return the ordered SQL commands which create `catalogName`.
	String[] getCreateCatalogCommands(String catalogName);

	/// Return the ordered SQL commands which drop `catalogName`.
	String[] getDropCatalogCommands(String catalogName);

	/// Report whether schema tooling may create and drop schemas.
	boolean canCreateSchema();

	/// Return the ordered SQL commands which create `schemaName`.
	String[] getCreateSchemaCommands(String schemaName);

	/// Return the ordered SQL commands which drop `schemaName`.
	String[] getDropSchemaCommands(String schemaName);
}
