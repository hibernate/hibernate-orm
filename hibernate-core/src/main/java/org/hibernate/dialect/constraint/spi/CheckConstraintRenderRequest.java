/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.constraint.spi;

import jakarta.annotation.Nullable;
import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.USE;
import static org.hibernate.internal.util.StringHelper.isBlank;

/// Describes one check constraint at its selected DDL placement.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public record CheckConstraintRenderRequest(
		CheckConstraintPlacement placement,
		@Nullable String name,
		String expression,
		@Nullable String options) {
	public CheckConstraintRenderRequest {
		requireNonNull( placement, "placement" );
		if ( isBlank( expression ) ) {
			throw new IllegalArgumentException( "expression must not be blank" );
		}
		switch ( placement ) {
			case ANONYMOUS_COLUMN -> {
				if ( name != null ) {
					throw new IllegalArgumentException( "anonymous column checks must not have a name" );
				}
			}
			case NAMED_COLUMN -> {
				if ( isBlank( name ) ) {
					throw new IllegalArgumentException( "named column checks must have a name" );
				}
			}
			case TABLE -> {
				if ( isBlank( name ) ) {
					name = null;
				}
			}
		}
	}
}
