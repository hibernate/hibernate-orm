/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.boot.query.SqlResultSetMappingDescriptor;
import org.hibernate.models.spi.AnnotationTarget;

import jakarta.annotation.Nullable;
import jakarta.persistence.SqlResultSetMapping;

/// Binding phase for query-oriented global boot registrations.
///
/// @since 9.0
/// @author Steve Ebersole
class QueryBindingPhase {
	static void bindSqlResultSetMapping(
			SqlResultSetMapping resultSetMapping,
			BindingState bindingState,
			@Nullable AnnotationTarget location,
			boolean isDefault) {
		if ( resultSetMapping == null ) {
			return;
		}

		final var mappingDefinition = SqlResultSetMappingDescriptor.from(
				resultSetMapping,
				resultSetMapping.name(),
				location == null ? null : location.getName()
		);

		if ( isDefault ) {
			bindingState.addDefaultResultSetMapping( mappingDefinition );
		}
		else {
			bindingState.addResultSetMapping( mappingDefinition );
		}
	}
}
