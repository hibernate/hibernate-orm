/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;

/// Immutable facts for naming a basic or version column.
/// Attribute paths are relative to the declaring entity or collection.
///
/// All reference components are non-null.
///
/// @param attributePath The attribute path supplied by the binding site, including component nesting when applicable;
/// synthetic composite members may also occur, for example `eventTime.zoneOffset`
///
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public record BasicColumnNamingInput(String attributePath) {
	public BasicColumnNamingInput {
		requireNonNull( attributePath, "attributePath" );
	}
}
