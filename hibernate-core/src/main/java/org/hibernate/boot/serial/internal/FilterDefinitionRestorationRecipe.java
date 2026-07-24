/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.serial.internal;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.model.internal.FilterDefBinder;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.metamodel.mapping.JdbcMapping;

/// Declarative archive form of a [FilterDefinition].
///
/// Parameter mappings are rebuilt from their declared type classes rather than
/// retaining producer-side `JdbcMapping` descriptors.
///
/// @since 9.0
/// @author Steve Ebersole
record FilterDefinitionRestorationRecipe(
		String name,
		String defaultCondition,
		boolean autoEnabled,
		boolean applyToLoadByKey,
		Map<String, String> parameterTypeClassNames,
		Map<String, String> parameterResolverClassNames) implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	FilterDefinitionRestorationRecipe {
		parameterTypeClassNames = Map.copyOf( parameterTypeClassNames );
		parameterResolverClassNames = Map.copyOf( parameterResolverClassNames );
	}

	static FilterDefinitionRestorationRecipe from(FilterDefinition definition) {
		return new FilterDefinitionRestorationRecipe(
				definition.getFilterName(),
				definition.getDefaultFilterCondition(),
				definition.isAutoEnabled(),
				definition.isAppliedToLoadByKey(),
				definition.getParameterTypeClassNames(),
				definition.getParameterResolverClassNames()
		);
	}

	FilterDefinition resolve(MetadataBuildingContext context) {
		final Map<String, JdbcMapping> parameterMappings = new HashMap<>();
		parameterTypeClassNames.forEach( (parameterName, className) -> {
			final Class<?> parameterType;
			try {
				parameterType = context.getBootstrapContext()
						.getClassLoaderAccess()
						.classForName( className );
			}
			catch (RuntimeException e) {
				throw new IllegalStateException(
						"Could not resolve archived type class '" + className
								+ "' for filter '" + name + "' parameter '" + parameterName + "'",
						e
				);
			}
			final JdbcMapping mapping;
			try {
				mapping = FilterDefBinder.resolveFilterParamType( parameterType, context );
			}
			catch (RuntimeException e) {
				throw new IllegalStateException(
						"Could not reconstruct archived type class '" + className
								+ "' for filter '" + name + "' parameter '" + parameterName + "'",
						e
				);
			}
			if ( mapping == null ) {
				throw new IllegalStateException(
						"Archived type class '" + className + "' for filter '" + name
								+ "' parameter '" + parameterName + "' did not resolve to a JdbcMapping"
				);
			}
			parameterMappings.put( parameterName, mapping );
		} );
		return FilterDefinition.restored(
				name,
				defaultCondition,
				autoEnabled,
				applyToLoadByKey,
				parameterMappings,
				parameterTypeClassNames,
				parameterResolverClassNames
		);
	}

}
