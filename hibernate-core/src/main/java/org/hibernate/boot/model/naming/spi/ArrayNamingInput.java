/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import java.util.Optional;

import org.hibernate.SPI;
import org.hibernate.relational.naming.spi.LogicalName;

import static java.util.Objects.requireNonNull;

/// Facts for an independently named SQL array, excluding anonymous SQL array syntax.
/// All reference components are non-null.
///
/// @param elementJavaType Domain element identity, not the array or collection container
/// @param preferredJdbcJavaType Preferred Java representation of the element JDBC type, when available
/// @param elementJdbcTypeCode Default SQL type code of the element JDBC type
/// @param converterType The element converter identity, not an outer collection wrapper
/// @param namedElement Resolved named enum, array, or struct dependency, if present
/// @param declaredElementTypeName Unqualified explicit logical struct name from an aggregate declaration; not an array default
/// @param representation VARRAY or nested-table representation; both have the same default convention
///
/// @since 9.0
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public record ArrayNamingInput(
		JavaTypeNamingInput elementJavaType,
		Optional<JavaTypeNamingInput> preferredJdbcJavaType,
		int elementJdbcTypeCode,
		Optional<JavaTypeNamingInput> converterType,
		Optional<NamedSqlTypeNamingInput> namedElement,
		Optional<LogicalName> declaredElementTypeName,
		ArrayNamingRepresentation representation) {
	public ArrayNamingInput {
		requireNonNull( elementJavaType, "elementJavaType" );
		requireNonNull( preferredJdbcJavaType, "preferredJdbcJavaType" );
		requireNonNull( converterType, "converterType" );
		requireNonNull( namedElement, "namedElement" );
		requireNonNull( declaredElementTypeName, "declaredElementTypeName" );
		requireNonNull( representation, "representation" );
	}
}
