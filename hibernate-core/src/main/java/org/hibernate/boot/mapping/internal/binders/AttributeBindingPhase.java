/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import org.hibernate.boot.mapping.internal.materialize.BasicValueResolutionBuilder;
import org.hibernate.boot.mapping.internal.materialize.BasicValueResolutionDetails;
import org.hibernate.boot.mapping.internal.context.MappingResolutionServices;
import org.hibernate.boot.mapping.internal.context.MappingResolutionState;
import org.hibernate.boot.mapping.internal.sources.BasicValueSource;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.BasicValue;

/// Narrow contracts for coordinator-driven attribute/value binding phases.
///
/// Attribute and value bindings are produced while walking type members, nested
/// components, identifiers, and synthetic contributors.  Unlike
/// [TypeBindingPhase], not every participant is itself a managed-type binder, so
/// the binding state keeps the phase participants until the coordinator reaches
/// the matching phase.
///
/// @since 9.0
/// @author Steve Ebersole
public interface AttributeBindingPhase {
	static ValueResolution valueResolution(
			BasicValueResolutionDetails details,
			MappingResolutionServices services,
			MappingResolutionState state,
			MetadataBuildingContext buildingContext) {
		return () -> BasicValueResolutionBuilder.applyResolution(
				details,
				services,
				state,
				buildingContext
		);
	}

	static ValueResolution valueResolution(
			BasicValue value,
			BasicValueSource source,
			MetadataBuildingContext buildingContext,
			MappingResolutionServices services,
			MappingResolutionState state) {
		value.setImplicitSourceJavaType( source.sourceJavaType() );
		return valueResolution(
				BasicValueResolutionDetails.create( value, source ),
				services,
				state,
				buildingContext
		);
	}

	static ValueResolution valueResolution(
			BasicValue value,
			BasicValueSource source,
			java.lang.reflect.Type resolvedJavaType,
			MetadataBuildingContext buildingContext,
			MappingResolutionServices services,
			MappingResolutionState state) {
		value.setImplicitSourceJavaType( source.sourceJavaType() );
		final BasicValueResolutionDetails details = BasicValueResolutionDetails.create( value, source );
		details.setResolvedJavaType( resolvedJavaType );
		return valueResolution( details, services, state, buildingContext );
	}

	/// Apply custom attribute/value mapping after the structural mapping object is
	/// available and before value resolution.
	interface CustomMapping {
		void bindCustomMapping();
	}

	/// Resolve materializer-created value types after custom mapping has had a
	/// chance to mutate the mapping object.
	interface ValueResolution {
		void resolveValue();
	}

	/// Apply final side effects that depend on completed value resolution.
	interface PostValueResolution {
		void afterValueResolution();
	}
}
