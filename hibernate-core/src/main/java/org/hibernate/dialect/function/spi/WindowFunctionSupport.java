/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.spi;

import java.util.EnumSet;
import java.util.Set;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;
import static org.hibernate.dialect.function.spi.WindowFunctionSupport.Feature.FRAME_EXCLUSION;
import static org.hibernate.dialect.function.spi.WindowFunctionSupport.Feature.GROUPS_FRAME;
import static org.hibernate.dialect.function.spi.WindowFunctionSupport.Feature.RANGE_FRAME;
import static org.hibernate.dialect.function.spi.WindowFunctionSupport.Feature.ROWS_FRAME;
import static org.hibernate.dialect.function.spi.WindowFunctionSupport.Feature.WINDOW_FUNCTIONS;

/// Immutable description of a Dialect's native window-function syntax.
///
/// Providers supply a stable profile through
/// [org.hibernate.dialect.Dialect#getWindowFunctionSupport]. Baseline window
/// functions, window partitioning, each frame unit, and non-default frame
/// exclusion are reported independently so consumers can validate the exact
/// syntax they render. Do not infer support for one frame unit from another.
///
/// Every refinement requires [Feature#WINDOW_FUNCTIONS]. In addition,
/// [Feature#FRAME_EXCLUSION] requires at least one supported frame unit.
/// Providers refining a family profile should copy the profile returned by the
/// superclass and change only the features which differ.
///
/// This profile does not describe function-specific window restrictions or
/// restrictions on individual frame bounds.
///
/// @see org.hibernate.dialect.Dialect#getWindowFunctionSupport()
/// @since 8.0
/// @author Steve Ebersole
@SPI({ USE, SUPPLY })
public final class WindowFunctionSupport {
	/// No native window-function syntax.
	public static final WindowFunctionSupport NONE = new WindowFunctionSupport( Set.of() );

	/// The base-Dialect profile, which has no native window-function syntax.
	public static final WindowFunctionSupport STANDARD = new WindowFunctionSupport( Set.of() );

	private final Set<Feature> features;

	private WindowFunctionSupport(Set<Feature> features) {
		this.features = Set.copyOf( features );
		validate();
	}

	/// Create a builder initialized from [#STANDARD].
	public static Builder builder() {
		return new Builder( STANDARD );
	}

	/// Create a builder initialized with every feature from the given profile.
	///
	/// @param base the non-null profile to copy
	public static Builder builder(WindowFunctionSupport base) {
		return new Builder( requireArgument( base, "base" ) );
	}

	/// The immutable set of supported window-function syntax features.
	public Set<Feature> getFeatures() {
		return features;
	}

	/// Whether the given window-function syntax feature is supported natively.
	public boolean supports(Feature feature) {
		return features.contains( requireArgument( feature, "feature" ) );
	}

	private void validate() {
		if ( !features.contains( WINDOW_FUNCTIONS ) && features.size() > 0 ) {
			throw new IllegalArgumentException( "Every window-function refinement requires WINDOW_FUNCTIONS" );
		}
		if ( features.contains( FRAME_EXCLUSION )
				&& !features.contains( ROWS_FRAME )
				&& !features.contains( RANGE_FRAME )
				&& !features.contains( GROUPS_FRAME ) ) {
			throw new IllegalArgumentException( "FRAME_EXCLUSION requires at least one frame feature" );
		}
	}

	/// An independently configurable window-function syntax feature.
	public enum Feature {
		/// Baseline window-function calls such as `row_number() over (...)`.
		WINDOW_FUNCTIONS,

		/// The `partition by` clause of a window specification.
		PARTITION_BY,

		/// A window frame using the `rows` unit.
		ROWS_FRAME,

		/// A window frame using the `range` unit.
		RANGE_FRAME,

		/// A window frame using the `groups` unit.
		GROUPS_FRAME,

		/// Non-default `exclude current row`, `exclude group`, and `exclude ties`
		/// frame refinements.
		FRAME_EXCLUSION
	}

	/// Build an immutable window-function-support profile.
	public static final class Builder {
		private final EnumSet<Feature> features;

		private Builder(WindowFunctionSupport base) {
			features = base.features.isEmpty()
					? EnumSet.noneOf( Feature.class )
					: EnumSet.copyOf( base.features );
		}

		/// Enable the given window-function features without changing other
		/// features.
		public Builder features(Feature... features) {
			requireArgument( features, "features" );
			for ( Feature feature : features ) {
				feature( feature, true );
			}
			return this;
		}

		/// Enable or disable one window-function feature.
		public Builder feature(Feature feature, boolean supported) {
			requireArgument( feature, "feature" );
			if ( supported ) {
				features.add( feature );
			}
			else {
				features.remove( feature );
			}
			return this;
		}

		/// Build and validate an immutable snapshot of this builder.
		public WindowFunctionSupport build() {
			return new WindowFunctionSupport( features );
		}
	}

	private static <T> T requireArgument(T argument, String name) {
		if ( argument == null ) {
			throw new IllegalArgumentException( name + " must not be null" );
		}
		return argument;
	}
}
