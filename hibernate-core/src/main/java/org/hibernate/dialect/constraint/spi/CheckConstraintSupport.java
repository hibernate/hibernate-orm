/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.constraint.spi;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Defines supported check-constraint placements and rendering.
///
/// A strategy which supports [CheckConstraintPlacement#NAMED_COLUMN] must also
/// support [CheckConstraintPlacement#ANONYMOUS_COLUMN]. Render one complete
/// check fragment from the immutable request; do not retain it.
///
/// @see Dialect#getCheckConstraintSupport()
///
/// @author Steve Ebersole
/// @since 8.0
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface CheckConstraintSupport {
	default boolean supports(CheckConstraintPlacement placement) {
		requireNonNull( placement, "placement" );
		return true;
	}

	default String render(CheckConstraintRenderRequest request) {
		requireNonNull( request, "request" );
		return request.name() == null
				? "check (" + request.expression() + ")"
				: "constraint " + request.name() + " check (" + request.expression() + ")";
	}
}
