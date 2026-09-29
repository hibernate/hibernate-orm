/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import java.util.Optional;
import java.util.List;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;

/// Resolved facts for an independently named SQL enum.
/// All reference components are non-null.
///
/// @param javaType The domain enum identity
/// @param representation Textual or ordinal named-enum representation
/// @param values Effective database labels in DDL order, after conversion; defensively copied
/// @param converterType The application converter class or internal converter implementation, when present
///
/// @since 9.0
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public record EnumNamingInput(
		JavaTypeNamingInput javaType,
		EnumNamingRepresentation representation,
		List<String> values,
		Optional<JavaTypeNamingInput> converterType) {
	public EnumNamingInput {
		requireNonNull( javaType, "javaType" );
		requireNonNull( representation, "representation" );
		values = List.copyOf( values );
		requireNonNull( converterType, "converterType" );
	}
}
