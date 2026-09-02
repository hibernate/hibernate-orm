/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.schema.spi;

import jakarta.annotation.Nullable;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.USE;

/// Provides the rendered inputs for a non-identity column definition.
///
/// The collation value is the rendered collation name, while default and
/// generated values are expressions without their surrounding clauses.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public record ColumnDefinitionRequest(
		String sqlType,
		@Nullable String renderedCollation,
		boolean nullable,
		@Nullable String defaultExpression,
		@Nullable String generatedExpression) {
	public ColumnDefinitionRequest {
		requireNonNull( sqlType );
	}
}
