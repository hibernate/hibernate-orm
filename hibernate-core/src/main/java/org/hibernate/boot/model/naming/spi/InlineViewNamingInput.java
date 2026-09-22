/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import org.hibernate.SPI;
import org.hibernate.relational.naming.spi.LogicalName;

import static java.util.Objects.requireNonNull;

/// An inline-view dependency. There is no physical table name or exposed query text.
///
/// All reference components are non-null.
///
/// @param logicalName The non-null logical identity of the inline view; no physical table name is fabricated
///
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public record InlineViewNamingInput(LogicalName logicalName) implements TableNamingInput {
	public InlineViewNamingInput {
		requireNonNull( logicalName, "logicalName" );
	}
}
