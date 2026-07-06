/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import org.hibernate.boot.mapping.internal.materialize.BasicValueResolutionBuilder;
import org.hibernate.boot.mapping.internal.sources.BasicValueSource;
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
	static ValueResolution valueResolution(BasicValueResolutionBuilder.Input input) {
		return () -> BasicValueResolutionBuilder.applyResolution( input );
	}

	static ValueResolution valueResolution(BasicValue value, BasicValueSource source) {
		value.setImplicitSourceJavaType( source.sourceJavaType() );
		return valueResolution( BasicValueResolutionBuilder.Input.create( value, source ) );
	}

	static ValueResolution valueResolution(BasicValue value, BasicValueSource source, java.lang.reflect.Type resolvedJavaType) {
		value.setImplicitSourceJavaType( source.sourceJavaType() );
		final BasicValueResolutionBuilder.Input input = BasicValueResolutionBuilder.Input.create( value, source );
		input.setResolvedJavaType( resolvedJavaType );
		return valueResolution( input );
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
