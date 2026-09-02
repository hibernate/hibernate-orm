/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.unique.spi;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.USE;

/// Describes syntax which requires a unique declaration to be represented as
/// an index instead of a relational unique constraint.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public record UniqueKeyRepresentationRequest(
		boolean containsFormula,
		boolean explicitType,
		boolean explicitUsing) {
}
