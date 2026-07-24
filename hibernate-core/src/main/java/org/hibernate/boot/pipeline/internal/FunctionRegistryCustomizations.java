/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Internal;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;

/// Explicit function-registry customizations supplied by a bootstrap entry point.
///
/// @since 9.0
/// @author Steve Ebersole
@Internal
public record FunctionRegistryCustomizations(
		List<FunctionContributor> functionContributors,
		Map<String, SqmFunctionDescriptor> sqlFunctions) {
	public static final FunctionRegistryCustomizations NONE =
			new FunctionRegistryCustomizations( List.of(), Map.of() );

	public FunctionRegistryCustomizations {
		functionContributors = functionContributors == null ? List.of() : List.copyOf( functionContributors );
		sqlFunctions = sqlFunctions == null
				? Map.of()
				: Collections.unmodifiableMap( new LinkedHashMap<>( sqlFunctions ) );
	}
}
