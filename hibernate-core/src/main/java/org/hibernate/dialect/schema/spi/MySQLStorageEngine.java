/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.schema.spi;

import org.hibernate.SPI;
import org.hibernate.annotations.OnDeleteAction;

/// Describes the schema and foreign-key behavior of a MySQL storage engine.
///
/// Implement action support and self-reference handling directly so the owning
/// Dialect can expose them through its foreign-key strategy. Return fragments
/// which remain stable for the selected engine.
///
/// @author Vlad Mihalcea
@SPI(SPI.Role.IMPLEMENT)
public interface MySQLStorageEngine {
	boolean supportsOnDeleteAction(OnDeleteAction action);

	String getTableTypeString(String engineKeyword);

	boolean requiresSelfReferentialForeignKeyNullification();

	boolean dropConstraints();
}
